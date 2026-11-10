package com.example.torexit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

class TorExitListHealthIndicatorTest {

    private final ExitNodeService service = mock(ExitNodeService.class);
    private final TorExitListHealthIndicator indicator = new TorExitListHealthIndicator(service);

    @Test
    void isDownUntilTheListIsLoaded() {
        when(service.isLoaded()).thenReturn(false);

        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    void isUpOnceTheListIsLoaded() {
        when(service.isLoaded()).thenReturn(true);
        when(service.lastUpdated()).thenReturn(Instant.parse("2026-11-10T11:30:02Z"));

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }
}
