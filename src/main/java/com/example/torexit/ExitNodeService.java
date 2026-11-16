package com.example.torexit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Keeps the Tor exit list in memory.
 *
 * <p>How the cache works: a background job calls {@link #refreshIfExpired()} every minute. It downloads the
 * list only when there is none yet or the current one is older than the TTL. HTTP requests never download
 * anything; they only read what is in memory, so they are always fast.
 *
 * <p>If a download fails, the previous list stays in use (and the next try is one minute later). If there has
 * never been a successful download, {@link #isExitNode} throws {@link ExitListUnavailableException}.
 */
@Service
public class ExitNodeService {

    private static final Logger log = LoggerFactory.getLogger(ExitNodeService.class);

    /** The list together with the time it was downloaded. Never modified after creation. */
    private record Cache(Set<Ipv4Address> addresses, Instant loadedAt) {
    }

    private final ExitListClient client;
    private final Clock clock;
    private final Duration cacheTtl;

    // One volatile reference to an immutable object: the background thread swaps in a whole new Cache,
    // and request threads always see either the old or the new one, never a half-updated state.
    private volatile Cache cache;

    public ExitNodeService(ExitListClient client, Clock clock, TorExitProperties properties) {
        this.client = client;
        this.clock = clock;
        this.cacheTtl = properties.cacheTtl();
    }

    @Scheduled(fixedDelayString = "${tor.exit-list.check-interval}") // first run is right at start-up
    public void refreshIfExpired() {
        Cache current = cache;
        if (current != null && Duration.between(current.loadedAt(), clock.instant()).compareTo(cacheTtl) < 0) {
            return; // still fresh
        }

        try {
            Set<Ipv4Address> addresses = ExitListParser.parse(client.fetch());
            if (addresses.isEmpty()) {
                // A real list is never empty; most likely we got an error page. Do not throw away a good list.
                throw new IllegalStateException("downloaded list contains no exit addresses");
            }
            cache = new Cache(Set.copyOf(addresses), clock.instant());
            log.info("Loaded {} Tor exit addresses", addresses.size());
        } catch (RuntimeException e) {
            log.warn("Could not refresh the Tor exit list{}: {}",
                    current == null ? "" : " (keeping the previous one)", e.toString());
        }
    }

    /** @throws ExitListUnavailableException if the list has not been loaded yet */
    public boolean isExitNode(Ipv4Address ip) {
        return loadedCache().addresses().contains(ip);
    }

    /** When the list currently in use was downloaded. */
    public Instant lastUpdated() {
        return loadedCache().loadedAt();
    }

    public boolean isLoaded() {
        return cache != null;
    }

    private Cache loadedCache() {
        Cache current = cache;
        if (current == null) {
            throw new ExitListUnavailableException();
        }
        return current;
    }
}
