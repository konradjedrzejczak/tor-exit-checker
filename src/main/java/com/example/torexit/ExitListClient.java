package com.example.torexit;

/** Downloads the raw exit list. An interface, so tests can replace the real HTTP call with a fake. */
@FunctionalInterface
public interface ExitListClient {

    /** Returns the file content; throws a RuntimeException if the download fails. */
    String fetch();
}
