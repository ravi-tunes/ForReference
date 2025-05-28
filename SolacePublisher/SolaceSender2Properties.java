import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "solace.sender2")
public class SolaceSender2Properties extends SolaceConfigProperties {
}
