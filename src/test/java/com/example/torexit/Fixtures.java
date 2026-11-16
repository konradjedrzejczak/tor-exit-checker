package com.example.torexit;

/** Test data shared between test classes. All addresses come from the documentation ranges (RFC 5737). */
final class Fixtures {

    /** A small exit list in the Tor Project's format: three exit addresses of two relays. */
    static final String SAMPLE_EXIT_LIST = """
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

    static final String EXIT_1 = "192.0.2.1";
    static final String EXIT_2 = "198.51.100.7";
    static final String EXIT_3 = "198.51.100.8";
    static final String NOT_EXIT = "203.0.113.99";

    private Fixtures() {
    }

    static Ipv4Address ip(String text) {
        return Ipv4Address.parse(text);
    }
}
