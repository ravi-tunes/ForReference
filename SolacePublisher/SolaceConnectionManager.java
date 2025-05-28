import com.solacesystems.jcsmp.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages the connection to Solace using JCSMP with retry and configuration support.
 */
public class SolaceConnectionManager {

    private static final Logger logger = LogManager.getLogger(SolaceConnectionManager.class);

    private final SolaceConfigProperties config;
    private volatile JCSMPSession session;

    public SolaceConnectionManager(SolaceConfigProperties config) {
        this.config = config;
    }

    public synchronized JCSMPSession getSession() throws JCSMPException {
        if (session != null && !session.isClosed()) {
            return session;
        }

        JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, config.getHost());
        properties.setProperty(JCSMPProperties.VPN_NAME, config.getVpn());
        properties.setProperty(JCSMPProperties.USERNAME, config.getUsername());
        properties.setProperty(JCSMPProperties.PASSWORD, config.getPassword());
        properties.setProperty(JCSMPProperties.REAPPLY_SUBSCRIPTIONS, true);
        properties.setProperty(JCSMPProperties.CLIENT_NAME, "BookPublisherClient");

        int[] retryDelays = {5, 15, 60}; // seconds

        for (int i = 0; i < retryDelays.length; i++) {
            try {
                logger.info("Attempting to connect to Solace (attempt {}/{}).", i + 1, retryDelays.length);
                session = JCSMPFactory.onlyInstance().createSession(properties);
                session.connect();
                logger.info("Successfully connected to Solace.");
                return session;
            } catch (JCSMPException e) {
                logger.error("Connection attempt {} failed: {}", i + 1, e.getMessage());
                if (i < retryDelays.length - 1) {
                    try {
                        Thread.sleep(retryDelays[i] * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new JCSMPException("Interrupted during Solace connection retry", ie);
                    }
                } else {
                    throw new JCSMPException("All Solace connection attempts failed", e);
                }
            }
        }

        throw new IllegalStateException("Unreachable code - retry logic should handle termination.");
    }
}
