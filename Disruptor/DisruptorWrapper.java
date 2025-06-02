@Component
public class DisruptorWrapper<T> implements SmartLifecycle {
    private final Disruptor<DisruptorEvent<T>> disruptor;
    private final DisruptorConfig config;
    private volatile boolean running = false;
    
    @Autowired
    public DisruptorWrapper(DisruptorConfig config, 
                           Executor executor, 
                           EventFactory<DisruptorEvent<T>> eventFactory) {
        this.config = config;
        
        this.disruptor = new Disruptor<>(
            eventFactory,
            config.getRingBufferSize(),
            executor,
            config.isMultiProducer() ? ProducerType.MULTI : ProducerType.SINGLE,
            createWaitStrategy()
        );
    }
    
    private WaitStrategy createWaitStrategy() {
        switch (config.getWaitStrategy()) {
            case 1: return new SleepingWaitStrategy();
            case 2: return new YieldingWaitStrategy();
            case 3: return new BusySpinWaitStrategy();
            default: return new BlockingWaitStrategy();
        }
    }
    
    public void registerHandlers(List<EventHandler<DisruptorEvent<T>>> handlers) {
        disruptor.handleEventsWith(handlers.toArray(new EventHandler[0]));
    }
    
    public void publishEvent(EventTranslator<DisruptorEvent<T>> translator) {
        if (config.isBlockWhenFull()) {
            disruptor.getRingBuffer().publishEvent(translator);
        } else {
            if (!disruptor.getRingBuffer().tryPublishEvent(translator)) {
                // Handle full buffer
                throw new IllegalStateException("Disruptor ring buffer full");
            }
        }
    }
    
    @Override
    public void start() {
        if (!running) {
            disruptor.start();
            running = true;
        }
    }
    
    @Override
    public void stop() {
        if (running) {
            try {
                disruptor.shutdown(config.getShutdownTimeoutMs(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                // Force shutdown
                disruptor.halt();
            }
            running = false;
        }
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }
    
    public RingBuffer<DisruptorEvent<T>> getRingBuffer() {
        return disruptor.getRingBuffer();
    }
}