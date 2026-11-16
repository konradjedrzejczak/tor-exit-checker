package com.example.torexit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class Ipv4AddressTest {

    @ParameterizedTest
    @ValueSource(strings = {"0.0.0.0", "1.2.3.4", "10.0.0.1", "192.0.2.255", "255.255.255.255"})
    void parsesValidAddresses(String text) {
        // when
        var address = Ipv4Address.tryParse(text);

        // then
        assertThat(address).hasValueSatisfying(parsed -> assertThat(parsed).hasToString(text));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "",
            "1.2.3",                  // too few parts
            "1.2.3.4.5",              // too many parts
            "256.1.1.1",              // out of range
            "1.2.3.999",
            "01.2.3.4",               // leading zero (read as octal by some systems)
            "1.2.3.04",
            "1.2.3.4 ",               // whitespace
            " 1.2.3.4",
            "1.2.3.4\n",
            "1.2.3.a",
            "1..3.4",
            "0x7f.0.0.1",             // hex
            "3232235777",             // a single number
            "::1",                    // IPv6
            "2001:db8::1",
            "::ffff:192.0.2.1",       // IPv6 that Java would turn into an IPv4 address
            "1.2.3.4/24",             // CIDR
            "1.2.3.4:80",             // with port
            "localhost",
            "٣.٣.٣.٣" // non-ASCII digits
    })
    void rejectsEverythingElse(String text) {
        // when
        var address = Ipv4Address.tryParse(text);

        // then
        assertThat(address).isEmpty();
    }

    @Test
    void parseThrowsForInvalidInputWithoutEchoingIt() {
        // when / then
        assertThatThrownBy(() -> Ipv4Address.parse("<script>"))
                .isInstanceOf(InvalidIpAddressException.class)
                .hasMessageNotContaining("script");
    }

    @Test
    void equalAddressesAreInterchangeableInSets() {
        // given
        Set<Ipv4Address> set = Set.of(Ipv4Address.parse("192.0.2.1"));

        // when / then
        assertThat(set).contains(Ipv4Address.parse("192.0.2.1")).doesNotContain(Ipv4Address.parse("192.0.2.2"));
    }
}
