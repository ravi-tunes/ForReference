import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SolaceConnectionConfig {

    @Bean
    public SolaceConnectionManager receiverConnection(SolaceReceiverProperties props) {
        return new SolaceConnectionManager(props);
    }

    @Bean
    public SolaceConnectionManager sender1Connection(SolaceSender1Properties props) {
        return new SolaceConnectionManager(props);
    }

    @Bean
    public SolaceConnectionManager sender2Connection(SolaceSender2Properties props) {
        return new SolaceConnectionManager(props);
    }
}
