package com.boondi.NewSolace.consumer;

import com.boondi.NewSolace.AbstractManagedSolaceConnection;
import com.boondi.NewSolace.SolaceSessionService;
import com.boondi.eq.ptd.solace.SolaceConsumer;
import com.boondi.eq.ptd.solace.SolaceListener;
import com.boondi.eq.ptd.solace.SolaceReliableSession;
import com.boondi.properties.instance.InstanceProperties;
import com.boondi.solace.properties.SolaceUpstreamReceiverProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Abstract base class for upstream consumers, containing common logic for managing
 * a SolaceReliableSession and a SolaceConsumer.
 */
public abstract class AbstractUpstreamConsumer extends AbstractManagedSolaceConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUpstreamConsumer.class);

    protected final SolaceUpstreamReceiverProperties properties;
    protected final InstanceProperties instanceProperties;
    protected final SolaceSessionService sessionService;
    protected SolaceReliableSession reliableSession;
    protected SolaceConsumer consumer;
    protected SolaceListener solaceListener;

    public AbstractUpstreamConsumer(SolaceUpstreamReceiverProperties properties, InstanceProperties instanceProperties, SolaceSessionService sessionService, SolaceListener solaceListener) {
        this.properties = properties;
        this.instanceProperties = instanceProperties;
        this.sessionService = sessionService;
        this.solaceListener = solaceListener;
    }

    @Override
    public void connect() {
        if (!isConnected()) {
            lastConnectionAttemptTime = Instant.now();
            try {
                reliableSession = sessionService.createReliableSession(properties, instanceProperties.getInstanceName());
                consumer = reliableSession.createConsumer(properties.getQueuename());
                consumer.addListener(solaceListener);
                consumer.start();
                connectionError = null;
            } catch (Exception e) {
                connectionError = e.getMessage();
                LOGGER.error("Failed to connect upstream consumer", e);
            }
        }
    }

    @Override
    public void disconnect() {
        if (consumer != null) {
            try {
                consumer.stop();
            } catch (Exception e) {
                LOGGER.error("Error stopping consumer", e);
            } finally {
                consumer = null;
            }
        }
        if (reliableSession != null) {
            try {
                reliableSession.stop();
            } catch (Exception e) {
                LOGGER.error("Error stopping reliable session", e);
            } finally {
                reliableSession = null;
            }
        }
        connectionError = null;
    }

    @Override
    public boolean isConnected() {
        if (reliableSession == null) {
            return false;
        }
        JCSMPSession session = reliableSession.get_session();
        return session != null && !session.isClosed();
    }
}
