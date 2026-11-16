package com.example.torexit;

import static com.example.torexit.Fixtures.ip;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ExitListRefresherTest {

    private static final Duration TTL = Duration.ofMinutes(30);
    private static final Instant START = Instant.parse("2026-11-10T12:00:00Z");

    private final MutableClock clock = new MutableClock(START);
    private final ExitNodeRegistry registry = new ExitNodeRegistry();

    // What the fake "Tor Project" currently answers, and how many times it was asked.
    private String response = "ExitAddress 192.0.2.1 2026-11-10 09:30:05\n";
    private boolean down = false;
    private int downloads = 0;

    private final ExitListRefresher refresher = new ExitListRefresher(
            new ExitListLoader(() -> {
                downloads++;
                if (down) {
                    throw new IllegalStateException("Tor Project is down");
                }
                return response;
            }),
            registry, clock, Fixtures.properties(TTL));

    @Test
    void downloadsTheListTheFirstTime() {
        // when
        refresher.refreshIfExpired();

        // then
        assertSoftly(softly -> {
            softly.assertThat(registry.isLoaded()).isTrue();
            softly.assertThat(registry.isExitNode(ip("192.0.2.1"))).isTrue();
            softly.assertThat(registry.lastUpdated()).isEqualTo(START);
        });
    }

    @Test
    void doesNotDownloadAgainBeforeTheTtlPasses() {
        // given
        refresher.refreshIfExpired();

        // when
        clock.advance(TTL.minusSeconds(1));
        refresher.refreshIfExpired();

        // then
        assertThat(downloads).isEqualTo(1);
    }

    @Test
    void downloadsAgainAfterTheTtlPasses() {
        // given
        refresher.refreshIfExpired();
        response = "ExitAddress 192.0.2.2 2026-11-10 09:30:05\n";

        // when
        clock.advance(TTL);
        refresher.refreshIfExpired();

        // then
        assertSoftly(softly -> {
            softly.assertThat(downloads).isEqualTo(2);
            softly.assertThat(registry.isExitNode(ip("192.0.2.1"))).isFalse();
            softly.assertThat(registry.isExitNode(ip("192.0.2.2"))).isTrue();
            softly.assertThat(registry.lastUpdated()).isEqualTo(START.plus(TTL));
        });
    }

    @Test
    void keepsThePreviousListWhenARefreshFails() {
        // given
        refresher.refreshIfExpired();
        down = true;

        // when
        clock.advance(TTL);
        refresher.refreshIfExpired();

        // then
        assertSoftly(softly -> {
            softly.assertThat(registry.isExitNode(ip("192.0.2.1"))).isTrue();
            softly.assertThat(registry.lastUpdated()).isEqualTo(START); // still the old one
        });
    }

    @Test
    void retriesRightAwayAfterAFailedFirstDownload() {
        // given
        down = true;
        refresher.refreshIfExpired();
        assertThat(registry.isLoaded()).isFalse();

        // when: no waiting for the TTL, because there is nothing cached yet
        down = false;
        refresher.refreshIfExpired();

        // then
        assertThat(registry.isLoaded()).isTrue();
    }

    @Test
    void doesNotReplaceAGoodListWithAnEmptyOne() {
        // given
        refresher.refreshIfExpired();
        response = "<html>Please try again later</html>";

        // when
        clock.advance(TTL);
        refresher.refreshIfExpired();

        // then
        assertThat(registry.isExitNode(ip("192.0.2.1"))).isTrue();
    }
}
