// KerberosHealthIndicator.java
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class KerberosHealthIndicator implements HealthIndicator {
    private final KerberosAuthManager authManager;
    private final AtomicReference<Health> lastHealth = new AtomicReference<>(Health.unknown().build());

    public KerberosHealthIndicator(KerberosAuthManager authManager) {
        this.authManager = authManager;
    }

    @Override
    public Health health() {
        try {
            KerberosAuthManager.HealthCheckResult result = authManager.checkHealth();
            
            Health.Builder healthBuilder = result.isHealthy() ? 
                Health.up() : Health.down();
                
            // Add detailed health information
            healthBuilder.withDetail("ticketStatus", result.getMessage());
            healthBuilder.withDetail("overallStatus", authManager.getHealthStatus().toString());
            
            Health health = healthBuilder.build();
            lastHealth.set(health);
            return health;
            
        } catch (Exception e) {
            Health health = Health.down()
                .withDetail("error", e.getMessage())
                .build();
            lastHealth.set(health);
            return health;
        }
    }

    public Health getLastHealth() {
        return lastHealth.get();
    }
}