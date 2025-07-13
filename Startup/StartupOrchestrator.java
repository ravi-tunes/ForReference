import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class StartupOrchestrator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupOrchestrator.class);

    private final CacheOneService cacheOne;
    private final CacheTwoService cacheTwo;
    private final CacheThreeService cacheThree;
    private final JmsListenerEndpointRegistry registry;

    public StartupOrchestrator(CacheOneService cacheOne, CacheTwoService cacheTwo, CacheThreeService cacheThree, JmsListenerEndpointRegistry registry) {
        this.cacheOne = cacheOne;
        this.cacheTwo = cacheTwo;
        this.cacheThree = cacheThree;
        this.registry = registry;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("--- STARTUP ORCHESTRATION: STARTING CACHE PRIMING ---");

        // 1. Trigger all async cache priming tasks.
        // These calls return immediately with a CompletableFuture.
        CompletableFuture<Void> future1 = cacheOne.primeCache();
        CompletableFuture<Void> future2 = cacheTwo.primeCache(); // Assumes a 5-second delay
        CompletableFuture<Void> future3 = cacheThree.primeCache(); // Assumes a 2-second delay

        try {
            // 2. Create a combined Future that completes only when ALL individual futures complete.
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(future1, future2, future3);

            // 3. Block and wait for the combined Future to complete.
            // .join() is a blocking call. The main application thread will pause here.
            log.info("Main thread is now waiting for all caches to finish priming...");
            allFutures.join();

            log.info("--- ALL CACHES PRIMED SUCCESSFULLY ---");

            // 4. Now that all caches are ready, start the Solace listener.
            log.info("Starting Solace message listener...");
            MessageListenerContainer listenerContainer = registry.getListenerContainer("mySolaceListener");
            if (listenerContainer != null) {
                listenerContainer.start();
                log.info("✅ Solace listener started successfully. Application is ready to process messages.");
            } else {
                log.error("Could not find listener with ID 'mySolaceListener'");
                throw new IllegalStateException("Listener container not found!");
            }

        } catch (Exception e) {
            // If any of the futures complete exceptionally, .join() will re-throw the exception.
            log.error("FATAL: Application startup failed during cache priming. Shutting down.", e);
            // Re-throwing the exception will cause the Spring Boot application to fail to start.
            throw new RuntimeException("Failed to initialize application caches.", e);
        }
    }
}