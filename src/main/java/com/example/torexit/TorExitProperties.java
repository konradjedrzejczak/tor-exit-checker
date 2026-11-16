package com.example.torexit;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under {@code tor.exit-list.*} (defaults in application.yml; any value can be overridden with an
 * environment variable, e.g. {@code TOR_EXIT_LIST_CACHE_TTL=PT15M}).
 *
 * <p>The values are checked when the application starts, so a typo fails fast with a clear message instead of
 * causing strange behaviour later.
 *
 * @param url            where the exit list is downloaded from
 * @param cacheTtl       how long a downloaded list is used before a new one is downloaded
 * @param checkInterval  how often the background job checks whether a download is due (also the retry delay)
 * @param connectTimeout limit for connecting to the Tor Project
 * @param readTimeout    limit for waiting on the response
 */
@ConfigurationProperties(prefix = "tor.exit-list")
public record TorExitProperties(
        URI url,
        Duration cacheTtl,
        Duration checkInterval,
        Duration connectTimeout,
        Duration readTimeout) {

    public TorExitProperties {
        if (url == null || url.getScheme() == null || !url.getScheme().matches("(?i)https?")) {
            throw new IllegalArgumentException("tor.exit-list.url must be an http(s) URL");
        }
        requirePositive(cacheTtl, "cache-ttl");
        requirePositive(checkInterval, "check-interval");
        requirePositive(connectTimeout, "connect-timeout");
        requirePositive(readTimeout, "read-timeout");
    }

    private static void requirePositive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("tor.exit-list." + name + " must be a positive duration");
        }
    }
}
