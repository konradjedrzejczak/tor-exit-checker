package com.example.torexit;

import static com.example.torexit.Fixtures.ip;
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
        // when / then
        assertThat(service.isLoaded()).isFalse();
        assertThatThrownBy(() -> service.isExitNode(ip("192.0.2.1"))).isInstanceOf(ExitListUnavailableException.class);
    }

    @Test
    void answersFromTheDownloadedList() {
        // when
        service.refreshIfExpired();

        // then
        assertThat(service.isLoaded()).isTrue();
        assertThat(service.isExitNode(ip("192.0.2.1"))).isTrue();
        assertThat(service.isExitNode(ip("192.0.2.2"))).isFalse();
        assertThat(service.lastUpdated()).isEqualTo(START);
    }

    @Test
    void doesNotDownloadAgainBeforeTheTtlPasses() {
        // given
        service.refreshIfExpired();

        // when
        clock.advance(TTL.minusSeconds(1));
        service.refreshIfExpired();

        // then
        assertThat(downloads).isEqualTo(1);
    }

    @Test
    void downloadsAgainAfterTheTtlPasses() {
        // given
        service.refreshIfExpired();
        response = "ExitAddress 192.0.2.2 2026-11-10 09:30:05\n";

        // when
        clock.advance(TTL);
        service.refreshIfExpired();

        // then
        assertThat(downloads).isEqualTo(2);
        assertThat(service.isExitNode(ip("192.0.2.1"))).isFalse();
        assertThat(service.isExitNode(ip("192.0.2.2"))).isTrue();
        assertThat(service.lastUpdated()).isEqualTo(START.plus(TTL));
    }

    @Test
    void keepsThePreviousListWhenARefreshFails() {
        // given
        service.refreshIfExpired();
        down = true;

        // when
        clock.advance(TTL);
        service.refreshIfExpired();

        // then
        assertThat(service.isExitNode(ip("192.0.2.1"))).isTrue();
        assertThat(service.lastUpdated()).isEqualTo(START); // still the old one
    }

    @Test
    void retriesRightAwayAfterAFailedFirstDownload() {
        // given
        down = true;
        service.refreshIfExpired();
        assertThat(service.isLoaded()).isFalse();

        // when: no waiting for the TTL, because there is nothing cached yet
        down = false;
        service.refreshIfExpired();

        // then
        assertThat(service.isLoaded()).isTrue();
    }

    @Test
    void doesNotReplaceAGoodListWithAnEmptyOne() {
        // given
        service.refreshIfExpired();
        response = "<html>Please try again later</html>";

        // when
        clock.advance(TTL);
        service.refreshIfExpired();

        // then
        assertThat(service.isExitNode(ip("192.0.2.1"))).isTrue();
    }
}
