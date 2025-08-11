package com.boondi.NewSolace;

import com.boondi.NewSolace.consumer.NewMochaSolaceConnection;
import com.boondi.NewSolace.consumer.NewPogSolaceConnection;
import com.boondi.properties.instance.InstanceProperties;
import com.boondi.solace.UpstreamListener;
import com.boondi.solace.properties.SolaceUpstreamReceiverProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A new Spring configuration bean for wiring up the refactored Solace components.
 * This class is for demonstration purposes to show how the new components would be configured.
 */
@Configuration("NewSolaceConnectionManagerBean")
public class NewSolaceConnectionManagerBean {

    @Bean
    public ManagedSolaceConnection newUpstreamReceiverConnection(
            InstanceProperties instanceProperties,
            SolaceUpstreamReceiverProperties solaceProperties,
            SolaceSessionService sessionService,
            UpstreamListener upstreamListener) {

        return switch (instanceProperties.getUpstreamType()) {
            case POG -> new NewPogSolaceConnection(solaceProperties, instanceProperties, sessionService, upstreamListener);
            case MOCHA -> new NewMochaSolaceConnection(solaceProperties, instanceProperties, sessionService, upstreamListener);
            default -> throw new IllegalArgumentException("Unsupported upstream type: " + instanceProperties.getUpstreamType());
        };
    }
}
