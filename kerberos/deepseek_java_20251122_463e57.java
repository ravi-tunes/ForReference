// SolaceHealthIndicator.java
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class SolaceHealthIndicator implements HealthIndicator {
    private final SolaceClient solaceClient;

    public SolaceHealthIndicator(SolaceClient solaceClient) {
        this.solaceClient = solaceClient;
    }

    @Override
    public Health health() {
        try {
            if (solaceClient.isConnected()) {
                return Health.up()
                    .withDetail("connectionState", solaceClient.getConnectionState())
                    .withDetail("broker", "connected")
                    .build();
            } else {
                return Health.down()
                    .withDetail("connectionState", solaceClient.getConnectionState())
                    .withDetail("broker", "disconnected")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("connectionState", "ERROR")
                .build();
        }
    }
}