package com.boondi.NewSolace;

import com.boondi.NewSolace.consumer.AbstractUpstreamConsumer;
import com.boondi.NewSolace.publisher.NewSolacePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SolaceConnectionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolaceConnectionManager.class);

    private final List<ManagedSolaceConnection> connections;

    @Autowired
    public SolaceConnectionManager(List<ManagedSolaceConnection> connections) {
        this.connections = connections;
    }

    @PostConstruct
    public void connectAll() {
        LOGGER.info("Connecting all Solace connections...");
        for (ManagedSolaceConnection connection : connections) {
            try {
                connection.connect();
            } catch (Exception e) {
                LOGGER.error("Error connecting to Solace: {}", connection, e);
            }
        }
        LOGGER.info("All Solace connections initiated.");
    }

    @PreDestroy
    public void disconnectAll() {
        LOGGER.info("Disconnecting all Solace connections gracefully...");

        // 1. Disconnect subscribers
        List<ManagedSolaceConnection> subscribers = connections.stream()
                .filter(c -> c instanceof AbstractUpstreamConsumer)
                .collect(Collectors.toList());

        for (ManagedSolaceConnection subscriber : subscribers) {
            try {
                subscriber.disconnect();
                LOGGER.info("Disconnected subscriber: {}", subscriber);
            } catch (Exception e) {
                LOGGER.error("Error disconnecting subscriber: {}", subscriber, e);
            }
        }

        // 2. Wait for a few seconds
        try {
            LOGGER.info("Waiting for 5 seconds before disconnecting publishers...");
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Interrupted while waiting to disconnect publishers.", e);
        }

        // 3. Disconnect publishers
        List<ManagedSolaceConnection> publishers = connections.stream()
                .filter(c -> c instanceof NewSolacePublisher)
                .collect(Collectors.toList());

        for (ManagedSolaceConnection publisher : publishers) {
            try {
                publisher.disconnect();
                LOGGER.info("Disconnected publisher: {}", publisher);
            } catch (Exception e) {
                LOGGER.error("Error disconnecting publisher: {}", publisher, e);
            }
        }

        LOGGER.info("All Solace connections disconnected.");
    }
}
