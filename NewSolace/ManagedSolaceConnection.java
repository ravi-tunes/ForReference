package com.boondi.NewSolace;

import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Defines the contract for a managed Solace connection, providing methods for managing
 * the connection lifecycle and checking its health.
 */
public interface ManagedSolaceConnection extends HealthIndicator {

    /**
     * Establishes the connection to Solace.
     */
    void connect();

    /**
     * Disconnects from Solace and releases resources.
     */
    void disconnect();

    /**
     * Checks if the connection is currently active.
     *
     * @return true if connected, false otherwise.
     */
    boolean isConnected();
}
