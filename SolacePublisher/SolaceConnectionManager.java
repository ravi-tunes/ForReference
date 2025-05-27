import com.solacesystems.jcsmp.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;

public class SolaceConnectionManager {

    private static final Logger logger = LogManager.getLogger(SolaceConnectionManager.class);

    private final String host;
    private final String vpn;
    private final String username;
    private final String password;

    private volatile JCSMPSession session;

    public SolaceConnectionManager(Properties config) {
        this.host = config.getProperty("solace.host");
        this.vpn = config.getProperty("solace.vpn");
        this.username = config.getProperty("solace.username");
        this.password = config.getProperty("solace.password");
    }

    public synchronized JCSMPSession getSession() throws JCSMPException {
        if (session != null && !session.isClosed()) {
            return session;
        }

        JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, host);
        properties.setProperty(JCSMPProperties.VPN_NAME, vpn);
        properties.setProperty(JCSMPProperties.USERNAME, username);
        properties.setProperty(JCSMPProperties.PASSWORD, password);
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

        throw new IllegalStateException("Should not reach here");
    }
}
