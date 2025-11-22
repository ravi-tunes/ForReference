// KerberosAuthManager.java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class KerberosAuthManager {
    private static final Logger logger = LoggerFactory.getLogger(KerberosAuthManager.class);

    private final String username;
    private final char[] password;
    private final KerberosConfig config;
    
    private volatile LoginContext loginContext;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "kerberos-auth-scheduler");
        t.setDaemon(true);
        return t;
    });
    
    private final AtomicBoolean isRenewing = new AtomicBoolean(false);
    private final AtomicReference<HealthStatus> healthStatus = 
        new AtomicReference<>(HealthStatus.UNKNOWN);

    public KerberosAuthManager(KerberosConfig config) {
        // In production, you'd get these from secure sources like Vault
        this.username = System.getenv("KERBEROS_USERNAME");
        String pwd = System.getenv("KERBEROS_PASSWORD");
        this.password = pwd != null ? pwd.toCharArray() : new char[0];
        this.config = config;
        
        if (username == null || password.length == 0) {
            throw new IllegalStateException("Kerberos credentials not found in environment variables");
        }
    }

    public void start() throws KerberosAuthException {
        try {
            logger.info("Initializing Kerberos authentication for user: {}", username);
            
            System.setProperty("java.security.krb5.kdc", config.getKdc());
            System.setProperty("java.security.krb5.realm", config.getRealm());
            
            javax.security.auth.login.Configuration.setConfiguration(
                new ProgrammaticJaasConfig(username));

            performLogin();
            scheduleNextMaintenance();
            
            logger.info("Kerberos authentication initialized successfully");
        } catch (Exception e) {
            healthStatus.set(HealthStatus.DOWN);
            throw new KerberosAuthException("Failed to initialize Kerberos authentication", e);
        }
    }

    public Subject getSubject() {
        return loginContext != null ? loginContext.getSubject() : null;
    }

    public HealthCheckResult checkHealth() {
        try {
            KerberosTicket tgt = getTGT();
            if (tgt == null) {
                healthStatus.set(HealthStatus.DOWN);
                return HealthCheckResult.unhealthy("No TGT available");
            }

            Instant now = Instant.now();
            Instant expiry = tgt.getEndTime().toInstant();
            Duration timeToExpiry = Duration.between(now, expiry);

            if (timeToExpiry.isNegative()) {
                healthStatus.set(HealthStatus.DOWN);
                return HealthCheckResult.unhealthy("TGT expired at " + tgt.getEndTime());
            }

            Duration buffer = Duration.ofMillis(config.getRefreshBufferMs());
            if (timeToExpiry.compareTo(buffer) <= 0) {
                healthStatus.set(HealthStatus.UP_WITH_WARNING);
                return HealthCheckResult.healthy("TGT expires soon in " + timeToExpiry);
            }

            healthStatus.set(HealthStatus.UP);
            return HealthCheckResult.healthy("TGT valid for " + timeToExpiry);
        } catch (Exception e) {
            healthStatus.set(HealthStatus.DOWN);
            return HealthCheckResult.unhealthy("Health check failed: " + e.getMessage());
        }
    }

    public HealthStatus getHealthStatus() {
        return healthStatus.get();
    }

    private synchronized void scheduleNextMaintenance() {
        if (scheduler.isShutdown()) {
            return;
        }

        long delayMs;
        KerberosTicket tgt = getTGT();

        if (tgt != null) {
            long now = System.currentTimeMillis();
            long expiry = tgt.getEndTime().getTime();
            long targetRefreshTime = expiry - config.getRefreshBufferMs();
            delayMs = targetRefreshTime - now;

            logger.info("Ticket valid until: {}. Scheduling refresh for: {}", 
                tgt.getEndTime(), new Date(targetRefreshTime));

            if (delayMs <= 0) {
                logger.warn("Ticket is expiring soon or expired. Scheduling immediate refresh.");
                delayMs = config.getRetryDelayMs();
            }
        } else {
            logger.error("No TGT found to calculate expiry. Retrying in 1 minute.");
            delayMs = 60000;
        }

        scheduler.schedule(this::maintenanceTask, delayMs, TimeUnit.MILLISECONDS);
    }

    private void maintenanceTask() {
        try {
            logger.debug("Executing scheduled Kerberos maintenance");
            forceRefreshOrRelogin();
        } catch (Exception e) {
            logger.error("Scheduled maintenance task failed", e);
        } finally {
            scheduleNextMaintenance();
        }
    }

    public synchronized void forceRefreshOrRelogin() {
        if (!isRenewing.compareAndSet(false, true)) {
            logger.debug("Refresh already in progress, skipping");
            return;
        }

        try {
            logger.info("Executing credential maintenance...");
            boolean renewed = attemptTicketRefresh();

            if (!renewed) {
                logger.info("Refresh failed/not supported. Performing full re-login...");
                performLogin();
            } else {
                logger.info("Ticket successfully renewed");
            }
        } catch (Exception e) {
            logger.error("Critical Kerberos error during maintenance", e);
            healthStatus.set(HealthStatus.DOWN);
        } finally {
            isRenewing.set(false);
        }
    }

    private void performLogin() throws LoginException {
        CallbackHandler cbHandler = callbacks -> {
            for (Callback cb : callbacks) {
                if (cb instanceof PasswordCallback) {
                    ((PasswordCallback) cb).setPassword(password);
                } else if (cb instanceof NameCallback) {
                    ((NameCallback) cb).setName(username);
                }
            }
        };

        LoginContext newContext = new LoginContext("StandardJaasConfig", cbHandler);
        newContext.login();
        this.loginContext = newContext;
        logger.info("Kerberos login successful. New TGT acquired.");
    }

    private boolean attemptTicketRefresh() {
        try {
            KerberosTicket tgt = getTGT();
            if (tgt != null && tgt.isRenewable()) {
                // Validate ticket before refresh
                validateTicket(tgt);
                tgt.refresh();
                logger.info("Ticket renewed via Kerberos API");
                return true;
            } else {
                logger.info("Ticket is not renewable, re-login required");
                return false;
            }
        } catch (Exception e) {
            logger.warn("Ticket refresh attempt failed: {}", e.getMessage());
            return false;
        }
    }

    private void validateTicket(KerberosTicket ticket) throws KerberosAuthException {
        if (ticket == null) {
            throw new KerberosAuthException("No Kerberos ticket available");
        }

        if (ticket.getEndTime().before(new Date())) {
            throw new KerberosAuthException("Kerberos ticket has expired");
        }

        if (!ticket.isRenewable()) {
            logger.warn("Kerberos ticket is not renewable");
        }

        // Additional validation can be added here
        logger.debug("Kerberos ticket validation successful");
    }

    private KerberosTicket getTGT() {
        if (loginContext == null || loginContext.getSubject() == null) {
            return null;
        }

        Set<KerberosTicket> tickets = loginContext.getSubject().getPrivateCredentials(KerberosTicket.class);
        for (KerberosTicket ticket : tickets) {
            if (ticket.getServer().getName().startsWith("krbtgt/")) {
                return ticket;
            }
        }
        return null;
    }

    public void shutdown() {
        logger.info("Shutting down KerberosAuthManager...");
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (loginContext != null) {
            try {
                loginContext.logout();
                logger.info("Kerberos logout successful");
            } catch (LoginException e) {
                logger.error("Error during Kerberos logout", e);
            }
        }

        // Clear sensitive data
        if (password != null) {
            Arrays.fill(password, '\0');
        }
        
        healthStatus.set(HealthStatus.DOWN);
        logger.info("KerberosAuthManager shutdown complete");
    }

    // Health status enum
    public enum HealthStatus {
        UP, UP_WITH_WARNING, DOWN, UNKNOWN
    }

    // Health check result
    public static class HealthCheckResult {
        private final boolean healthy;
        private final String message;

        private HealthCheckResult(boolean healthy, String message) {
            this.healthy = healthy;
            this.message = message;
        }

        public static HealthCheckResult healthy(String message) {
            return new HealthCheckResult(true, message);
        }

        public static HealthCheckResult unhealthy(String message) {
            return new HealthCheckResult(false, message);
        }

        public boolean isHealthy() { return healthy; }
        public String getMessage() { return message; }
    }
}