package com.example.torexit;

import static com.example.torexit.Fixtures.EXIT_1;
import static com.example.torexit.Fixtures.EXIT_2;
import static com.example.torexit.Fixtures.EXIT_3;
import static com.example.torexit.Fixtures.SAMPLE_EXIT_LIST;
import static com.example.torexit.Fixtures.ip;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class ExitListParserTest {

    @Test
    void readsAllExitAddresses() {
        // when
        Set<Ipv4Address> addresses = ExitListParser.parse(SAMPLE_EXIT_LIST);

        // then
        assertThat(addresses).containsExactlyInAnyOrder(ip(EXIT_1), ip(EXIT_2), ip(EXIT_3));
    }

    @Test
    void ignoresOtherLinesAndBlankLines() {
        // given
        String content = "\nSomethingElse 1.2.3.4\n\nExitNode ABC\n";

        // when
        Set<Ipv4Address> addresses = ExitListParser.parse(content);

        // then
        assertThat(addresses).isEmpty();
    }

    @Test
    void ignoresInvalidAndNonIpv4Addresses() {
        // given
        String content = """
                ExitAddress 999.1.1.1 2026-11-10 09:30:05
                ExitAddress 2001:db8::1 2026-11-10 09:30:05
                ExitAddress not-an-ip
                ExitAddress
                ExitAddress 192.0.2.9 2026-11-10 09:30:05
                """;

        // when
        Set<Ipv4Address> addresses = ExitListParser.parse(content);

        // then
        assertThat(addresses).containsExactly(ip("192.0.2.9"));
    }

    @Test
    void handlesWindowsLineEndings() {
        // when
        Set<Ipv4Address> addresses = ExitListParser.parse("ExitAddress 192.0.2.1 2026-11-10 09:30:05\r\n");

        // then
        assertThat(addresses).containsExactly(ip(EXIT_1));
    }

    @Test
    void emptyOrMissingContentGivesEmptySet() {
        // when / then
        assertThat(ExitListParser.parse("")).isEmpty();
        assertThat(ExitListParser.parse(null)).isEmpty();
    }
}
