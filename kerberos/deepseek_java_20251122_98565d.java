// SolaceConfig.java
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "solace")
public class SolaceConfig {
    private String host = "tcps://solace-broker:55443";
    private String vpnName = "default";
    private int connectionTimeoutMs = 10000;
    private int maxReconnectRetries = 3;

    // Getters and setters
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    
    public String getVpnName() { return vpnName; }
    public void setVpnName(String vpnName) { this.vpnName = vpnName; }
    
    public int getConnectionTimeoutMs() { return connectionTimeoutMs; }
    public void setConnectionTimeoutMs(int connectionTimeoutMs) { this.connectionTimeoutMs = connectionTimeoutMs; }
    
    public int getMaxReconnectRetries() { return maxReconnectRetries; }
    public void setMaxReconnectRetries(int maxReconnectRetries) { this.maxReconnectRetries = maxReconnectRetries; }
}