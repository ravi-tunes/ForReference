package com.boondi.NewSolace;

import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("solaceHealthIndicator")
public class SolaceHealthIndicator extends CompositeHealthIndicator {

    public SolaceHealthIndicator(HealthAggregator healthAggregator, Map<String, HealthIndicator> healthIndicators) {
        super(healthAggregator);
        healthIndicators.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof ManagedSolaceConnection)
                .forEach(entry -> addHealthIndicator(entry.getKey(), entry.getValue()));
    }
}
