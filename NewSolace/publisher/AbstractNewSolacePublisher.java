package com.boondi.NewSolace.publisher;

import com.boondi.NewSolace.AbstractManagedSolaceConnection;
import com.boondi.NewSolace.SolaceSessionService;
import com.boondi.NewSolace.common.DestinationType;
import com.boondi.solace.properties.SolaceConfigProperties;
import com.solacesystems.jcsmp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.time.Instant;

/**
 * Abstract base class for the new Solace publishers, containing common logic for
 * session management and message publishing.
 */
public abstract class AbstractNewSolacePublisher extends AbstractManagedSolaceConnection implements NewSolacePublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractNewSolacePublisher.class);

    protected final SolaceConfigProperties properties;
    protected final SolaceSessionService sessionService;
    protected JCSMPSession session;
    protected XMLMessageProducer producer;

    public AbstractNewSolacePublisher(SolaceConfigProperties properties, SolaceSessionService sessionService) {
        this.properties = properties;
        this.sessionService = sessionService;
    }

    @Override
    public void connect() {
        if (!isConnected()) {
            lastConnectionAttemptTime = Instant.now();
            try {
                session = sessionService.createSession(properties);
                producer = session.getMessageProducer(new JCSMPStreamingPublishEventHandler() {
                    @Override
                    public void responseReceived(String messageID) {
                        // As per requirements, we don't process acks, so this is intentionally left blank.
                    }

                    @Override
                    public void handleError(String messageID, JCSMPException e, long timestamp) {
                        LOGGER.error("Producer received error for msg: {}@{} - {}", messageID, timestamp, e);
                    }
                });
                connectionError = null;
            } catch (JCSMPException e) {
                connectionError = e.getMessage();
                LOGGER.error("Failed to connect publisher", e);
            }
        }
    }

    @Override
    public void disconnect() {
        if (session != null && !session.isClosed()) {
            session.closeSession();
        }
        connectionError = null;
    }

    @Override
    public boolean isConnected() {
        return session != null && !session.isClosed();
    }

    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    protected void sendMessage(byte[] message, String destinationName, DestinationType destinationType) throws Exception {
        if (!isConnected()) {
            connect(); // Attempt to reconnect if not connected
        }

        if (!isConnected()) {
            throw new IllegalStateException("Solace connection is not available.");
        }

        Destination destination = createDestination(destinationName, destinationType);
        BytesXMLMessage msg = JCSMPFactory.onlyInstance().createMessage(BytesXMLMessage.class);
        msg.setData(message);

        producer.send(msg, destination);
    }

    private Destination createDestination(String name, DestinationType type) {
        if (type == DestinationType.QUEUE) {
            return JCSMPFactory.onlyInstance().createQueue(name);
        }
        return JCSMPFactory.onlyInstance().createTopic(name);
    }
}
