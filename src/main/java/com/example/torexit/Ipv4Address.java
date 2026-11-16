package com.example.torexit;

import com.google.common.net.InetAddresses;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Objects;
import java.util.Optional;

/**
 * An IPv4 address in its canonical dotted-decimal form, e.g. {@code 192.0.2.1}.
 *
 * <p>The only way to get an instance is {@link #parse} / {@link #tryParse}, so an {@code Ipv4Address} is always
 * valid and has exactly one text form. That is what lets the rest of the code compare addresses with
 * {@code equals} (and keep them in a {@code Set}) without worrying about "01.2.3.4" vs "1.2.3.4".
 *
 * <p>The text is parsed by Guava's {@link InetAddresses}, which only reads address literals and never does a
 * DNS lookup (unlike {@code InetAddress.getByName}).
 */
public final class Ipv4Address {

    private final String value;

    private Ipv4Address(String value) {
        this.value = value;
    }

    /** Returns the address, or empty if the text is not a valid IPv4 address. */
    public static Optional<Ipv4Address> tryParse(String text) {
        if (text == null || !InetAddresses.isInetAddress(text)) {
            return Optional.empty();
        }
        InetAddress parsed = InetAddresses.forString(text);

        // Guava accepts more than we want: IPv6, IPv4-mapped IPv6 ("::ffff:1.2.3.4", which Java turns into an
        // Inet4Address) and even non-ASCII digits. Insisting that the canonical form equals the input rejects
        // all of them in one check: only plain dotted-decimal text passes.
        if (!(parsed instanceof Inet4Address) || !parsed.getHostAddress().equals(text)) {
            return Optional.empty();
        }
        return Optional.of(new Ipv4Address(text));
    }

    /** @throws InvalidIpAddressException if the text is not a valid IPv4 address (mapped to HTTP 400) */
    public static Ipv4Address parse(String text) {
        return tryParse(text).orElseThrow(InvalidIpAddressException::new);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Ipv4Address that && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
