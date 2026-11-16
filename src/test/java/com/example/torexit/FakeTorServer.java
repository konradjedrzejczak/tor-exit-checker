package com.example.torexit;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * A tiny local HTTP server that plays the part of check.torproject.org in the end-to-end tests, so they need no
 * internet. It uses the JDK's built-in server, so there is no extra dependency.
 */
final class FakeTorServer implements AutoCloseable {

    private final HttpServer server;

    /** Starts a server that answers /exit-addresses with the given text. */
    FakeTorServer(String exitList) {
        byte[] body = exitList.getBytes(StandardCharsets.UTF_8);
        try {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.createContext("/exit-addresses", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
    }

    URI uri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/exit-addresses");
    }

    /** An address nothing listens on: connecting to it fails immediately, like an unreachable Tor Project. */
    static URI unreachableUri() {
        try (ServerSocket socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            return URI.create("http://127.0.0.1:" + socket.getLocalPort() + "/exit-addresses");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
