package com.example.torexit;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The exit list has not been downloaded yet, so we cannot answer.
 * Mapped to 503 and deliberately not to 404: "I don't know" must not look like "it is not an exit node".
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ExitListUnavailableException extends RuntimeException {

    public ExitListUnavailableException() {
        super("The Tor exit list has not been loaded yet, try again shortly");
    }
}
