package com.boondi.NewSolace;

import org.springframework.boot.actuate.health.Health;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract base class for managed Solace connections, providing a common health
 * check implementation.
 */
public abstract class AbstractManagedSolaceConnection implements ManagedSolaceConnection {

    protected final AtomicReference<Health> health = new AtomicReference<>(Health.down().build());
    protected volatile String connectionError;
    protected Instant lastConnectionAttemptTime;

    @Override
    public Health health() {
        Health.Builder healthBuilder = new Health.Builder();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("Last Connection Attempt", lastConnectionAttemptTime);

        if (isConnected()) {
            healthBuilder.up();
        } else {
            healthBuilder.down();
            details.put("Connection Error", connectionError);
        }

        return healthBuilder.withDetails(details).build();
    }
}
