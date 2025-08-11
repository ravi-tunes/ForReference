package com.boondi.NewSolace.consumer;

import com.boondi.NewSolace.SolaceSessionService;
import com.boondi.eq.ptd.solace.SolaceListener;
import com.boondi.eq.ptd.solace.SolacePublisher;
import com.boondi.properties.instance.InstanceProperties;
import com.boondi.solace.properties.SolaceUpstreamReceiverProperties;
import com.boondi.mocha.apex.dictionaries.entity.mochadict2.xplatform.MochaEntityDict2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * A refactored implementation of the Mocha Solace connection, responsible for
 * handling Mocha-specific upstream message consumption and publishing responses.
 */
@Service("NewMochaSolaceConnection")
public class NewMochaSolaceConnection extends AbstractUpstreamConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewMochaSolaceConnection.class);

    private final Map<String, SolacePublisher> publisherMap = new HashMap<>();

    @Autowired
    public NewMochaSolaceConnection(SolaceUpstreamReceiverProperties properties, InstanceProperties instanceProperties, SolaceSessionService sessionService, SolaceListener solaceListener) {
        super(properties, instanceProperties, sessionService, solaceListener);
    }

    @Override
    public void disconnect() {
        super.disconnect();
        publisherMap.clear();
    }

    public void publishResponse(MochaEntityDict2.DestinationReply entity, String replyTopic) throws Exception {
        if (!isConnected()) {
            throw new IllegalStateException("Mocha Solace connection is not available.");
        }

        SolacePublisher publisher = publisherMap.computeIfAbsent(replyTopic, key -> {
            try {
                return reliableSession.createPublisher(key);
            } catch (Exception e) {
                LOGGER.error("Failed to create publisher for topic: {}", key, e);
                throw new RuntimeException(e);
            }
        });

        try {
            byte[] messageData = entity.toByteArray();
            publisher.publish(messageData, 0, messageData.length);
        } catch (Exception e) {
            LOGGER.error("Failed to publish response to topic: {}", replyTopic, e);
            // Attempt to recreate publisher on next attempt
            publisherMap.remove(replyTopic);
            throw e;
        }
    }
}
