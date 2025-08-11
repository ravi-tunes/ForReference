package com.boondi.NewSolace.publisher;

import com.boondi.NewSolace.SolaceSessionService;
import com.boondi.NewSolace.properties.NewSolaceAtlasPublisherProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * A refactored Solace publisher for sending messages to the Atlas system.
 */
@Service("NewSolaceAtlasPublisher")
public class NewSolaceAtlasPublisher extends AbstractNewSolacePublisher {

    private final NewSolaceAtlasPublisherProperties properties;

    @Autowired
    public NewSolaceAtlasPublisher(NewSolaceAtlasPublisherProperties properties, SolaceSessionService sessionService) {
        super(properties, sessionService);
        this.properties = properties;
    }

    @Override
    public void sendMessage(byte[] message) throws Exception {
        if (properties.getDestinationName() == null || properties.getDestinationName().isEmpty()) {
            throw new IllegalArgumentException("Destination cannot be null or empty.");
        }
        super.sendMessage(message, properties.getDestinationName(), properties.getDestinationType());
    }
}
