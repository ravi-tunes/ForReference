// KerberosConfig.java
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@ConfigurationProperties(prefix = "kerberos")
public class KerberosConfig {
    private long refreshBufferMs = TimeUnit.HOURS.toMillis(1);
    private int maxRetries = 3;
    private long retryDelayMs = TimeUnit.SECONDS.toMillis(10);
    private long healthCheckTimeoutMs = TimeUnit.SECONDS.toMillis(5);
    private String kdc;
    private String realm;

    // Getters and setters
    public long getRefreshBufferMs() { return refreshBufferMs; }
    public void setRefreshBufferMs(long refreshBufferMs) { this.refreshBufferMs = refreshBufferMs; }
    
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    
    public long getRetryDelayMs() { return retryDelayMs; }
    public void setRetryDelayMs(long retryDelayMs) { this.retryDelayMs = retryDelayMs; }
    
    public long getHealthCheckTimeoutMs() { return healthCheckTimeoutMs; }
    public void setHealthCheckTimeoutMs(long healthCheckTimeoutMs) { this.healthCheckTimeoutMs = healthCheckTimeoutMs; }
    
    public String getKdc() { return kdc; }
    public void setKdc(String kdc) { this.kdc = kdc; }
    
    public String getRealm() { return realm; }
    public void setRealm(String realm) { this.realm = realm; }
}