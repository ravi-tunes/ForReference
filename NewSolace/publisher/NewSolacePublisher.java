package com.boondi.NewSolace.publisher;

import com.boondi.NewSolace.ManagedSolaceConnection;

/**
 * Defines the contract for a Solace publisher, extending the ManagedSolaceConnection
 * interface with the ability to send messages.
 */
public interface NewSolacePublisher extends ManagedSolaceConnection {

    /**
     * Sends a byte array message to the default destination.
     *
     * @param message the byte array message to send.
     * @throws Exception if an error occurs while sending the message.
     */
    void sendMessage(byte[] message) throws Exception;

}

