import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync // IMPORTANT: This enables Spring's asynchronous method execution capability
public class AsyncConfig {

    @Bean(name = "cachePrimerExecutor")
    public Executor cachePrimerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Set the number of threads equal to the number of caches you want to prime in parallel
        executor.setCorePoolSize(3); 
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(10);
        // Give a recognizable name to threads from this pool
        executor.setThreadNamePrefix("CachePrimer-"); 
        executor.initialize();
        return executor;
    }
}