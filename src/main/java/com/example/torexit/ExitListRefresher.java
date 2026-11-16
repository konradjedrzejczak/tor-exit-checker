package com.example.torexit;

import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Decides when the exit list has to be downloaded again, and does it in the background.
 *
 * <p>A scheduled job calls {@link #refreshIfExpired()} every {@code check-interval} (one minute by default).
 * It downloads only when there is no list yet or the current one is older than the TTL. HTTP requests never
 * download anything, so they never wait for the Tor Project.
 *
 * <p>If a download fails, the previous list stays in use and the next try is one check interval later.
 */
@Component
public class ExitListRefresher {

    private static final Logger log = LoggerFactory.getLogger(ExitListRefresher.class);

    private final ExitListLoader loader;
    private final ExitNodeRegistry registry;
    private final Clock clock;
    private final Duration cacheTtl;

    public ExitListRefresher(ExitListLoader loader, ExitNodeRegistry registry, Clock clock, TorExitProperties properties) {
        this.loader = loader;
        this.registry = registry;
        this.clock = clock;
        this.cacheTtl = properties.cacheTtl();
    }

    // The first run happens right at start-up. "synchronized" makes sure two refreshes never run at the same time.
    @Scheduled(fixedDelayString = "${tor.exit-list.check-interval}")
    public synchronized void refreshIfExpired() {
        if (isFresh()) {
            return;
        }
        boolean hadList = registry.isLoaded();
        try {
            var addresses = loader.load();
            registry.replace(addresses, clock.instant());
            log.info("Loaded {} Tor exit addresses", addresses.size());
        } catch (RuntimeException e) {
            log.warn("Could not refresh the Tor exit list{}: {}", hadList ? " (keeping the previous one)" : "", e.toString());
        }
    }

    private boolean isFresh() {
        return registry.isLoaded()
                && Duration.between(registry.lastUpdated(), clock.instant()).compareTo(cacheTtl) < 0;
    }
}
