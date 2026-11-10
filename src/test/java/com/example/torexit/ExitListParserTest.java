package com.example.torexit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExitListParserTest {

    private static final String SAMPLE = """
            ExitNode 0011BD2485AD45D984EC4159C88FC066E5E3300E
            Published 2026-11-10 08:15:42
            LastStatus 2026-11-10 09:02:11
            ExitAddress 192.0.2.1 2026-11-10 09:30:05
            ExitNode 00A1B2C3D4E5F60718293A4B5C6D7E8F90A1B2C3
            Published 2026-11-09 21:00:00
            LastStatus 2026-11-10 09:02:11
            ExitAddress 198.51.100.7 2026-11-10 09:31:44
            ExitAddress 198.51.100.8 2026-11-10 09:31:44
            """;

    @Test
    void readsAllExitAddresses() {
        assertThat(ExitListParser.parse(SAMPLE)).containsExactlyInAnyOrder("192.0.2.1", "198.51.100.7", "198.51.100.8");
    }

    @Test
    void ignoresOtherLinesAndBlankLines() {
        String content = "\nSomethingElse 1.2.3.4\n\nExitNode ABC\n";

        assertThat(ExitListParser.parse(content)).isEmpty();
    }

    @Test
    void ignoresInvalidAndNonIpv4Addresses() {
        String content = """
                ExitAddress 999.1.1.1 2026-11-10 09:30:05
                ExitAddress 2001:db8::1 2026-11-10 09:30:05
                ExitAddress not-an-ip
                ExitAddress
                ExitAddress 192.0.2.9 2026-11-10 09:30:05
                """;

        assertThat(ExitListParser.parse(content)).containsExactly("192.0.2.9");
    }

    @Test
    void handlesWindowsLineEndings() {
        assertThat(ExitListParser.parse("ExitAddress 192.0.2.1 2026-11-10 09:30:05\r\n")).containsExactly("192.0.2.1");
    }

    @Test
    void emptyOrMissingContentGivesEmptySet() {
        assertThat(ExitListParser.parse("")).isEmpty();
        assertThat(ExitListParser.parse(null)).isEmpty();
    }
}
