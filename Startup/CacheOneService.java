import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class CacheOneService {

    private static final Logger log = LoggerFactory.getLogger(CacheOneService.class);
    private final Map<String, Object> internalCache = new ConcurrentHashMap<>();
    private volatile boolean isPrimed = false;

    /**
     * Primes the cache asynchronously.
     * The @Async annotation tells Spring to run this in a background thread
     * from our specified "cachePrimerExecutor" pool.
     *
     * @return A CompletableFuture that completes when the priming is done.
     */
    @Async("cachePrimerExecutor")
    public CompletableFuture<Void> primeCache() {
        log.info("Starting to prime Cache One... (This will run on a background thread)");

        try {
            // Simulate a long database call (e.g., 3 seconds)
            TimeUnit.SECONDS.sleep(3);

            // In a real app, you would fetch from the database here
            // e.g., jdbcTemplate.query("...", (rs, rowNum) -> ...);
            internalCache.put("key1", "value1");
            internalCache.put("key2", "value2");
            
            this.isPrimed = true;
            log.info("✅ Cache One is primed successfully. Count: {}", internalCache.size());

            // Signal successful completion of the future
            return CompletableFuture.completedFuture(null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Cache One priming was interrupted.", e);
            // Signal exceptional completion
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        } catch (Exception e) {
            log.error("Failed to prime Cache One.", e);
            // Signal exceptional completion for any other error
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    public int getCount() {
        return internalCache.size();
    }
    
    public boolean isPrimed() {
        return isPrimed;
    }
}

// Assume you have similar CacheTwoService and CacheThreeService classes