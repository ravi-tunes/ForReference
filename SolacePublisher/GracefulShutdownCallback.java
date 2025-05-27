import com.lmax.disruptor.Disruptor;
import com.lmax.disruptor.RingBuffer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class GracefulShutdownCallback implements Runnable {

    private final Disruptor<?> disruptor;
    private final RingBuffer<?> ringBuffer;
    private final ConfigurableApplicationContext applicationContext;

    public GracefulShutdownCallback(
            Disruptor<?> disruptor,
            RingBuffer<?> ringBuffer,
            ApplicationContext applicationContext
    ) {
        this.disruptor = disruptor;
        this.ringBuffer = ringBuffer;
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    @Override
    public void run() {
        try {
            System.err.println("Shutdown callback invoked: Stopping disruptor...");

            // Stop Disruptor gracefully
            disruptor.shutdown();

            // Optionally clear the ring buffer
            while (ringBuffer.hasAvailableCapacity(1)) {
                ringBuffer.tryNext();
            }

            System.err.println("Disruptor stopped. Shutting down Spring Boot application...");

            // Close Spring context
            applicationContext.close();
        } catch (Exception e) {
            System.err.println("Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
