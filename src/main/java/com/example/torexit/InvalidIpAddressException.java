package com.example.torexit;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The text is not a valid IPv4 address. Mapped to 400.
 * The input is deliberately not part of the message: it ends up in responses and logs, and echoing untrusted
 * text back is an avoidable risk.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidIpAddressException extends IllegalArgumentException {

    public InvalidIpAddressException() {
        super("Not a valid IPv4 address");
    }
}
