package com.example.torexit;

import java.time.Instant;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Holds the current exit list in memory and answers questions about it. It does not know where the list comes
 * from or when it should be replaced; it only stores it.
 *
 * <p>The list and the time it was downloaded live in one immutable object. The background thread swaps in a
 * whole new one through a single volatile field, so request threads always see either the old or the new list,
 * never a half-updated state, and they never have to wait.
 */
@Component
public class ExitNodeRegistry {

    /** The list together with the time it was downloaded. Never modified after creation. */
    private record Snapshot(Set<Ipv4Address> addresses, Instant loadedAt) {
    }

    private volatile Snapshot snapshot; // null until the first list has been stored

    /** Replaces the list that is used for answers. */
    public void replace(Set<Ipv4Address> addresses, Instant loadedAt) {
        snapshot = new Snapshot(Set.copyOf(addresses), loadedAt);
    }

    public boolean isLoaded() {
        return snapshot != null;
    }

    /** @throws ExitListUnavailableException if no list has been stored yet */
    public boolean isExitNode(Ipv4Address ip) {
        return current().addresses().contains(ip);
    }

    /** When the list currently in use was downloaded.
     *
     * @throws ExitListUnavailableException if no list has been stored yet
     */
    public Instant lastUpdated() {
        return current().loadedAt();
    }

    private Snapshot current() {
        Snapshot current = snapshot;
        if (current == null) {
            throw new ExitListUnavailableException();
        }
        return current;
    }
}
