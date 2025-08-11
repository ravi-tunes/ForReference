package com.boondi.NewSolace.publisher;

import com.boondi.NewSolace.SolaceSessionService;
import com.boondi.NewSolace.properties.NewSolacePVSPublisherProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * A refactored Solace publisher for sending messages to the PVS system.
 */
@Service("NewSolacePVSPublisher")
public class NewSolacePVSPublisher extends AbstractNewSolacePublisher {

    private final NewSolacePVSPublisherProperties properties;

    @Autowired
    public NewSolacePVSPublisher(NewSolacePVSPublisherProperties properties, SolaceSessionService sessionService) {
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
