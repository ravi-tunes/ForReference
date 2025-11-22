// SolaceClient.java
import com.solacesystems.jcsmp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class SolaceClient {
    private static final Logger logger = LoggerFactory.getLogger(SolaceClient.class);

    private final KerberosAuthManager authManager;
    private final SolaceConfig solaceConfig;
    private final AtomicReference<ConnectionState> connectionState = 
        new AtomicReference<>(ConnectionState.DISCONNECTED);
    private final AtomicInteger connectionAttempts = new AtomicInteger(0);
    
    private JCSMPSession session;

    public SolaceClient(KerberosAuthManager authManager, SolaceConfig solaceConfig) {
        this.authManager = authManager;
        this.solaceConfig = solaceConfig;
    }

    public void connect() throws SolaceConnectionException, KerberosAuthException {
        connect(0);
    }

    private void connect(int retryCount) throws SolaceConnectionException, KerberosAuthException {
        if (!connectionState.compareAndSet(ConnectionState.DISCONNECTED, ConnectionState.CONNECTING) && 
            !connectionState.compareAndSet(ConnectionState.FAILED, ConnectionState.CONNECTING)) {
            logger.warn("Connection already in progress or connected. Current state: {}", connectionState.get());
            return;
        }

        connectionAttempts.incrementAndGet();
        logger.info("Attempting to connect to Solace (attempt {})", retryCount + 1);

        try {
            Subject.doAs(authManager.getSubject(), (java.security.PrivilegedAction<Void>) () -> {
                try {
                    createAndConnectSession();
                    connectionState.set(ConnectionState.CONNECTED);
                    logger.info("Successfully connected to Solace");
                } catch (JCSMPException e) {
                    throw new RuntimeException(e);
                }
                return null;
            });
        } catch (RuntimeException e) {
            connectionState.set(ConnectionState.FAILED);
            
            if (e.getCause() instanceof JCSMPException) {
                handleConnectionError((JCSMPException) e.getCause(), retryCount);
            } else {
                throw new SolaceConnectionException("Unexpected error during connection", e);
            }
        }
    }

    private void createAndConnectSession() throws JCSMPException {
        JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, solaceConfig.getHost());
        properties.setProperty(JCSMPProperties.VPN_NAME, solaceConfig.getVpnName());
        properties.setProperty(JCSMPProperties.AUTHENTICATION_SCHEME, 
            JCSMPProperties.AUTHENTICATION_SCHEME_GSS_KRB);
        
        // Set connection timeout
        properties.setProperty(JCSMPProperties.CONNECT_TIMEOUT_IN_MILLIS, 
            solaceConfig.getConnectionTimeoutMs());

        session = JCSMPFactory.onlyInstance().createSession(properties);
        
        // Add session event listener for connection monitoring
        session.getSessionEventListeners().add(new SessionEventAdapter() {
            @Override
            public void handleEvent(SessionEvent event) {
                logger.info("Received Solace session event: {}", event);
                
                switch (event.getEvent()) {
                    case DOWN_ERROR:
                        connectionState.set(ConnectionState.FAILED);
                        logger.error("Solace connection lost: {}", event.getInfo());
                        break;
                    case RECONNECTING:
                        connectionState.set(ConnectionState.RECONNECTING);
                        logger.info("Solace reconnecting...");
                        break;
                    case RECONNECTED:
                        connectionState.set(ConnectionState.CONNECTED);
                        logger.info("Solace reconnected successfully");
                        break;
                }
            }
        });
        
        session.connect();
    }

    private void handleConnectionError(JCSMPException e, int retryCount) 
            throws SolaceConnectionException, KerberosAuthException {
        
        if (isAuthenticationError(e)) {
            logger.warn("Authentication error detected. Triggering Kerberos refresh...");
            
            if (retryCount >= solaceConfig.getMaxReconnectRetries()) {
                throw new SolaceConnectionException(
                    "Max authentication retry attempts (" + solaceConfig.getMaxReconnectRetries() + ") exceeded", e);
            }
            
            authManager.forceRefreshOrRelogin();
            
            logger.info("Retrying connection (attempt {}/{})", 
                retryCount + 1, solaceConfig.getMaxReconnectRetries());
            
            try {
                Thread.sleep(solaceConfig.getConnectionTimeoutMs());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new SolaceConnectionException("Connection retry interrupted", ie);
            }
            
            connect(retryCount + 1);
        } else {
            throw new SolaceConnectionException("Non-authentication error occurred", e);
        }
    }

    private boolean isAuthenticationError(JCSMPException e) {
        String message = e.getMessage();
        return message != null && 
               (message.contains("Authentication") || 
                message.contains("GSS") || 
                message.contains("401"));
    }

    public ConnectionState getConnectionState() {
        return connectionState.get();
    }

    public boolean isConnected() {
        return connectionState.get() == ConnectionState.CONNECTED && 
               session != null && 
               !session.isClosed();
    }

    public void disconnect() {
        logger.info("Disconnecting from Solace...");
        connectionState.set(ConnectionState.DISCONNECTED);
        
        if (session != null) {
            try {
                session.closeSession();
                logger.info("Solace session closed successfully");
            } catch (Exception e) {
                logger.error("Error closing Solace session", e);
            }
            session = null;
        }
    }

    public void shutdown() {
        disconnect();
        logger.info("SolaceClient shutdown complete");
    }
}