package com.example.torexit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ExitNodeServiceTest {

    private static final Duration TTL = Duration.ofMinutes(30);
    private static final Instant START = Instant.parse("2026-11-10T12:00:00Z");

    private final MutableClock clock = new MutableClock(START);

    // What the fake "Tor Project" currently answers, and how many times it was asked.
    private String response = "ExitAddress 192.0.2.1 2026-11-10 09:30:05\n";
    private boolean down = false;
    private int downloads = 0;

    private final ExitNodeService service = new ExitNodeService(() -> {
        downloads++;
        if (down) {
            throw new IllegalStateException("Tor Project is down");
        }
        return response;
    }, clock, TTL);

    @Test
    void cannotAnswerBeforeTheFirstDownload() {
        assertThat(service.isLoaded()).isFalse();
        assertThatThrownBy(() -> service.isExitNode("192.0.2.1")).isInstanceOf(ExitListUnavailableException.class);
    }

    @Test
    void answersFromTheDownloadedList() {
        service.refreshIfExpired();

        assertThat(service.isLoaded()).isTrue();
        assertThat(service.isExitNode("192.0.2.1")).isTrue();
        assertThat(service.isExitNode("192.0.2.2")).isFalse();
        assertThat(service.lastUpdated()).isEqualTo(START);
    }

    @Test
    void doesNotDownloadAgainBeforeTheTtlPasses() {
        service.refreshIfExpired();

        clock.advance(TTL.minusSeconds(1));
        service.refreshIfExpired();

        assertThat(downloads).isEqualTo(1);
    }

    @Test
    void downloadsAgainAfterTheTtlPasses() {
        service.refreshIfExpired();
        response = "ExitAddress 192.0.2.2 2026-11-10 09:30:05\n";

        clock.advance(TTL);
        service.refreshIfExpired();

        assertThat(downloads).isEqualTo(2);
        assertThat(service.isExitNode("192.0.2.1")).isFalse();
        assertThat(service.isExitNode("192.0.2.2")).isTrue();
        assertThat(service.lastUpdated()).isEqualTo(START.plus(TTL));
    }

    @Test
    void keepsThePreviousListWhenARefreshFails() {
        service.refreshIfExpired();
        down = true;

        clock.advance(TTL);
        service.refreshIfExpired();

        assertThat(service.isExitNode("192.0.2.1")).isTrue();
        assertThat(service.lastUpdated()).isEqualTo(START); // still the old one
    }

    @Test
    void retriesRightAwayAfterAFailedFirstDownload() {
        down = true;
        service.refreshIfExpired();
        assertThat(service.isLoaded()).isFalse();

        down = false;
        service.refreshIfExpired(); // no waiting for the TTL: there is nothing cached yet

        assertThat(service.isLoaded()).isTrue();
    }

    @Test
    void doesNotReplaceAGoodListWithAnEmptyOne() {
        service.refreshIfExpired();
        response = "<html>Please try again later</html>";

        clock.advance(TTL);
        service.refreshIfExpired();

        assertThat(service.isExitNode("192.0.2.1")).isTrue();
    }
}
