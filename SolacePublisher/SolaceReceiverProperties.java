import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "solace.receiver")
public class SolaceReceiverProperties extends SolaceConfigProperties {
}
