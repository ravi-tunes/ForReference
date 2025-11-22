// ApplicationStartupRunner.java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartupRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationStartupRunner.class);

    private final KerberosAuthManager authManager;
    private final SolaceClient solaceClient;

    public ApplicationStartupRunner(KerberosAuthManager authManager, SolaceClient solaceClient) {
        this.authManager = authManager;
        this.solaceClient = solaceClient;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting Kerberos-Solace application...");
        
        try {
            // Initialize Kerberos first
            authManager.start();
            
            // Then connect to Solace
            solaceClient.connect();
            
            logger.info("Application started successfully");
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            throw e;
        }
    }
}