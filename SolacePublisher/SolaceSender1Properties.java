import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "solace.sender1")
public class SolaceSender1Properties extends SolaceConfigProperties {
}
