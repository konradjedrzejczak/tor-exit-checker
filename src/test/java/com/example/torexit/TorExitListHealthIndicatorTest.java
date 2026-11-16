package com.example.torexit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

class TorExitListHealthIndicatorTest {

    private final ExitNodeRegistry registry = mock(ExitNodeRegistry.class);
    private final TorExitListHealthIndicator indicator = new TorExitListHealthIndicator(registry);

    @Test
    void isDownUntilTheListIsLoaded() {
        // given
        when(registry.isLoaded()).thenReturn(false);

        // when
        Health health = indicator.health();

        // then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    void isUpOnceTheListIsLoaded() {
        // given
        when(registry.isLoaded()).thenReturn(true);
        when(registry.lastUpdated()).thenReturn(Instant.parse("2026-11-10T11:30:02Z"));

        // when
        Health health = indicator.health();

        // then
        assertSoftly(softly -> {
            softly.assertThat(health.getStatus()).isEqualTo(Status.UP);
            softly.assertThat(health.getDetails()).containsEntry("listUpdatedAt", "2026-11-10T11:30:02Z");
        });
    }
}
