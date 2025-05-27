import com.lmax.disruptor.Disruptor;
import com.lmax.disruptor.RingBuffer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShutdownConfig {

    @Bean
    public Runnable shutdownCallback(
            Disruptor<?> disruptor,
            RingBuffer<?> ringBuffer,
            ApplicationContext applicationContext
    ) {
        return new GracefulShutdownCallback(disruptor, ringBuffer, applicationContext);
    }
}
