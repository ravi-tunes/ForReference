@ConfigurationProperties(prefix = "disruptor")
public class DisruptorConfig {
    private int ringBufferSize = 1024;  // Must be power of 2
    private int waitStrategy = 0;       // 0=Blocking, 1=Sleeping, 2=Yielding, 3=BusySpin
    private long shutdownTimeoutMs = 1000;
    private boolean multiProducer = false;
    private boolean blockWhenFull = true;

    // Getters and setters
}