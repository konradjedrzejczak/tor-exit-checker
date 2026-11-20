package com.example.torexit;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health check, visible at /actuator/health (the Docker HEALTHCHECK uses it).
 *
 * <p>UP when an exit list is loaded, so the service can answer. DOWN before the first successful download.
 * It only looks at memory and never calls the Tor Project.
 */
@Component
public class TorExitListHealthIndicator implements HealthIndicator {

    private final ExitNodeRegistry registry;

    public TorExitListHealthIndicator(ExitNodeRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Health health() {
        if (!registry.isLoaded()) {
            return Health.down().withDetail("reason", "exit list not loaded yet").build();
        }
        return Health.up().withDetail("listUpdatedAt", registry.lastUpdated().toString()).build();
    }
}