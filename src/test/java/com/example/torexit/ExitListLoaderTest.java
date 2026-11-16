package com.example.torexit;

import static com.example.torexit.Fixtures.EXIT_1;
import static com.example.torexit.Fixtures.EXIT_2;
import static com.example.torexit.Fixtures.EXIT_3;
import static com.example.torexit.Fixtures.SAMPLE_EXIT_LIST;
import static com.example.torexit.Fixtures.ip;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ExitListLoaderTest {

    @Test
    void returnsTheAddressesFromTheDownloadedFile() {
        // given
        ExitListLoader loader = new ExitListLoader(() -> SAMPLE_EXIT_LIST);

        // when / then
        assertThat(loader.load()).containsExactlyInAnyOrder(ip(EXIT_1), ip(EXIT_2), ip(EXIT_3));
    }

    @Test
    void failsWhenTheFileContainsNoExitAddresses() {
        // given: e.g. an error page served with status 200
        ExitListLoader loader = new ExitListLoader(() -> "<html>Please try again later</html>");

        // when / then
        assertThatThrownBy(loader::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no exit addresses");
    }

    @Test
    void passesOnDownloadFailures() {
        // given
        ExitListLoader loader = new ExitListLoader(() -> {
            throw new IllegalStateException("connection refused");
        });

        // when / then
        assertThatThrownBy(loader::load).hasMessage("connection refused");
    }
}
