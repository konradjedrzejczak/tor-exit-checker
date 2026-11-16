package com.example.torexit;

import java.util.HashSet;
import java.util.Set;

/**
 * Reads the Tor Project's exit-addresses file. It looks like this:
 *
 * <pre>
 * ExitNode 0011BD2485AD45D984EC4159C88FC066E5E3300E
 * Published 2026-11-10 08:15:42
 * LastStatus 2026-11-10 09:02:11
 * ExitAddress 192.0.2.1 2026-11-10 09:30:05
 * </pre>
 *
 * Only the "ExitAddress" lines matter here; the IP is the second word.
 */
final class ExitListParser {

    private ExitListParser() {
    }

    /** Returns all valid IPv4 exit addresses. Other lines, and addresses that are not IPv4, are ignored. */
    static Set<Ipv4Address> parse(String content) {
        Set<Ipv4Address> addresses = new HashSet<>();
        if (content == null) {
            return addresses;
        }
        for (String line : content.lines().toList()) {
            String[] words = line.strip().split("\\s+");
            if (words.length >= 2 && words[0].equals("ExitAddress")) {
                Ipv4Address.tryParse(words[1]).ifPresent(addresses::add);
            }
        }
        return addresses;
    }
}
