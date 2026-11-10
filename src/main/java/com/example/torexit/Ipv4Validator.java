package com.example.torexit;

import java.util.regex.Pattern;

/**
 * Checks that text is an IPv4 address in the usual dotted form, e.g. 192.0.2.1.
 *
 * <p>A regex is used instead of InetAddress.getByName because that method accepts odd forms such as "1.2"
 * and may even try a DNS lookup. Each octet is 0-255 without leading zeros ("01" is rejected, because some
 * systems read it as octal), so every valid address has exactly one text form and can be compared as a String.
 */
final class Ipv4Validator {

    private static final String OCTET = "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)";
    private static final Pattern IPV4 = Pattern.compile(OCTET + "(\\." + OCTET + "){3}");

    private Ipv4Validator() {
    }

    static boolean isValid(String text) {
        // matches() has to match the WHOLE text, so "1.2.3.4abc" or "1.2.3.4\n" are rejected
        return text != null && IPV4.matcher(text).matches();
    }
}
