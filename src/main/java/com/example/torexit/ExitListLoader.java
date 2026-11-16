package com.example.torexit;

import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Gets a usable exit list: downloads the file and reads the addresses out of it.
 * Fails (instead of returning something useless) when the result cannot be trusted.
 */
@Component
public class ExitListLoader {

    private final ExitListClient client;

    public ExitListLoader(ExitListClient client) {
        this.client = client;
    }

    /** @throws RuntimeException if the download fails or the file contains no exit addresses */
    public Set<Ipv4Address> load() {
        Set<Ipv4Address> addresses = ExitListParser.parse(client.fetch());
        if (addresses.isEmpty()) {
            // A real list is never empty; most likely we got an error page. Better to fail than to
            // let the caller replace a good list with an empty one.
            throw new IllegalStateException("downloaded list contains no exit addresses");
        }
        return addresses;
    }
}
