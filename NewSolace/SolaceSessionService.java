package com.boondi.NewSolace;

import com.boondi.eq.ptd.solace.SolaceReliableSession;
import com.boondi.solace.properties.SolaceConfigProperties;
import com.solacesystems.jcsmp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * A centralized service for creating and managing Solace sessions.
 */
@Service
public class SolaceSessionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolaceSessionService.class);

    /**
     * Creates a standard JCSMP session.
     *
     * @param properties the configuration properties for the session.
     * @return a connected JCSMPSession.
     * @throws JCSMPException if the connection fails after all retries.
     */
    @Retryable(
            value = {JCSMPException.class},
            maxAttempts = 4, // Initial attempt + 3 retries
            backoff = @Backoff(delay = 5000, multiplier = 2)
    )
    public JCSMPSession createSession(SolaceConfigProperties properties) throws JCSMPException {
        LOGGER.info("Attempting to create Solace session for host: {}", properties.getHost());
        JCSMPProperties jcsmpProperties = createJCSMPProperties(properties);
        JCSMPSession session = JCSMPFactory.onlyInstance().createSession(jcsmpProperties);
        session.connect();
        LOGGER.info("Successfully created Solace session.");
        return session;
    }

    /**
     * Creates a SolaceReliableSession.
     *
     * @param properties the configuration properties for the session.
     * @param instanceName the name of the application instance.
     * @return a started SolaceReliableSession.
     * @throws Exception if the connection fails after all retries.
     */
    @Retryable(
            value = {Exception.class},
            maxAttempts = 4, // Initial attempt + 3 retries
            backoff = @Backoff(delay = 5000, multiplier = 2)
    )
    public SolaceReliableSession createReliableSession(SolaceConfigProperties properties, String instanceName) throws Exception {
        LOGGER.info("Attempting to create SolaceReliableSession for host: {}", properties.getHost());
        SolaceReliableSession reliableSession = new SolaceReliableSession(instanceName);
        reliableSession.setHost(properties.getHost());
        reliableSession.setAuthenticationScheme(JCSMPProperties.AUTHENTICATION_SCHEME_GSS_KRB);
        reliableSession.setUser(properties.getUsername() + "@" + properties.getVpn());
        reliableSession.setAutoAck(false);
        reliableSession.setWaitTimeout(2000);
        reliableSession.start();
        LOGGER.info("Successfully created SolaceReliableSession.");
        return reliableSession;
    }

    private JCSMPProperties createJCSMPProperties(SolaceConfigProperties properties) {
        JCSMPProperties jcsmpProperties = new JCSMPProperties();
        jcsmpProperties.setProperty(JCSMPProperties.HOST, properties.getHost());
        jcsmpProperties.setProperty(JCSMPProperties.VPN_NAME, properties.getVpn());
        jcsmpProperties.setProperty(JCSMPProperties.USERNAME, properties.getUsername());
        jcsmpProperties.setProperty(JCSMPProperties.PASSWORD, properties.getPassword());
        return jcsmpProperties;
    }
}
