package com.boondi.NewSolace.properties;

import com.boondi.NewSolace.common.DestinationType;
import com.boondi.solace.properties.SolaceConfigProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@Configuration("NewSolacePVSPublisherProperties")
@ConfigurationProperties(prefix = "solace.pvs.publisher")
@Validated
public class NewSolacePVSPublisherProperties extends SolaceConfigProperties {

    @NotBlank
    private String destinationName;

    @NotNull
    private DestinationType destinationType = DestinationType.QUEUE; // Default to QUEUE for backward compatibility
}
