package com.example.torexit;

import static com.example.torexit.Fixtures.ip;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ExitNodeRegistryTest {

    private static final Instant LOADED_AT = Instant.parse("2026-11-10T12:00:00Z");

    private final ExitNodeRegistry registry = new ExitNodeRegistry();

    @Test
    void cannotAnswerBeforeAListIsStored() {
        // when / then
        assertThat(registry.isLoaded()).isFalse();
        assertThatThrownBy(() -> registry.isExitNode(ip("192.0.2.1"))).isInstanceOf(ExitListUnavailableException.class);
        assertThatThrownBy(registry::lastUpdated).isInstanceOf(ExitListUnavailableException.class);
    }

    @Test
    void answersFromTheStoredList() {
        // given
        registry.replace(Set.of(ip("192.0.2.1")), LOADED_AT);

        // then
        assertSoftly(softly -> {
            softly.assertThat(registry.isLoaded()).isTrue();
            softly.assertThat(registry.isExitNode(ip("192.0.2.1"))).isTrue();
            softly.assertThat(registry.isExitNode(ip("192.0.2.2"))).isFalse();
            softly.assertThat(registry.lastUpdated()).isEqualTo(LOADED_AT);
        });
    }

    @Test
    void replacingSwapsTheWholeList() {
        // given
        registry.replace(Set.of(ip("192.0.2.1")), LOADED_AT);

        // when
        registry.replace(Set.of(ip("192.0.2.2")), LOADED_AT.plusSeconds(60));

        // then
        assertSoftly(softly -> {
            softly.assertThat(registry.isExitNode(ip("192.0.2.1"))).isFalse();
            softly.assertThat(registry.isExitNode(ip("192.0.2.2"))).isTrue();
            softly.assertThat(registry.lastUpdated()).isEqualTo(LOADED_AT.plusSeconds(60));
        });
    }
}
