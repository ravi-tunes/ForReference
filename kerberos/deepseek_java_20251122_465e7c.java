// ApplicationShutdownHook.java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

@Component
public class ApplicationShutdownHook implements ApplicationListener<ContextClosedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationShutdownHook.class);

    private final KerberosAuthManager authManager;
    private final SolaceClient solaceClient;

    public ApplicationShutdownHook(KerberosAuthManager authManager, SolaceClient solaceClient) {
        this.authManager = authManager;
        this.solaceClient = solaceClient;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        logger.info("Application shutdown initiated...");
        
        solaceClient.shutdown();
        authManager.shutdown();
        
        logger.info("Application shutdown complete");
    }
}