package com.boondi.NewSolace.consumer;

import com.boondi.NewSolace.SolaceSessionService;
import com.boondi.eq.ptd.solace.SolaceListener;
import com.boondi.eq.ptd.tracer.TracerClientServiceFixImpl;
import com.boondi.properties.instance.InstanceProperties;
import com.boondi.solace.properties.SolaceUpstreamReceiverProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * A refactored implementation of the POG Solace connection, responsible for
 * handling POG-specific upstream message consumption.
 */
@Service("NewPogSolaceConnection")
public class NewPogSolaceConnection extends AbstractUpstreamConsumer {

    private TracerClientServiceFixImpl tracerClientServiceFix;

    @Autowired
    public NewPogSolaceConnection(SolaceUpstreamReceiverProperties properties, InstanceProperties instanceProperties, SolaceSessionService sessionService, SolaceListener solaceListener) {
        super(properties, instanceProperties, sessionService, solaceListener);
    }

    @Override
    public void connect() {
        super.connect();
        if (isConnected()) {
            initialiseTracerSession();
        }
    }

    @Override
    public void disconnect() {
        super.disconnect();
        tracerClientServiceFix = null;
    }

    private void initialiseTracerSession() {
        // POG requires this for consumers of protobuf messages
        System.setProperty("PBONLY.SUPPORTED.TOPICS", ""); // Add relevant topics here
        tracerClientServiceFix = new TracerClientServiceFixImpl();
        tracerClientServiceFix.setSolaceSession(reliableSession);
        tracerClientServiceFix.setTemplateTopic(""); // Add template topic here
        tracerClientServiceFix.setLoadStripingPatterns(""); // Add stripping patterns here
        tracerClientServiceFix.setRemoveAfterSend(true);
        tracerClientServiceFix.setBatchedAck(false);
        tracerClientServiceFix.setBatchedDelayedAck(false);
        tracerClientServiceFix.setAppID("ATLS");
        tracerClientServiceFix.setInstanceID(instanceProperties.getInstanceName());
    }
}
