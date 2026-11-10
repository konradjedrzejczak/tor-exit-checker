package com.example.torexit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class Ipv4ValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"0.0.0.0", "1.2.3.4", "10.0.0.1", "192.0.2.255", "255.255.255.255"})
    void acceptsValidAddresses(String ip) {
        assertThat(Ipv4Validator.isValid(ip)).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "",
            "1.2.3",          // too few parts
            "1.2.3.4.5",      // too many parts
            "256.1.1.1",      // out of range
            "1.2.3.999",
            "01.2.3.4",       // leading zero
            "1.2.3.4 ",       // whitespace
            "1.2.3.4\n",
            "1.2.3.a",
            "1..3.4",
            "::1",            // IPv6
            "1.2.3.4/24",     // CIDR
            "1.2.3.4:80",
            "localhost",
            "٣.٣.٣.٣" // non-ASCII digits
    })
    void rejectsEverythingElse(String text) {
        assertThat(Ipv4Validator.isValid(text)).isFalse();
    }
}
