package com.example.torexit;

import java.time.Instant;

/**
 * JSON body of GET /{ip} for an exit node, for example:
 *
 * <pre>
 * { "ip": "192.0.2.1", "listUpdatedAt": "2026-11-10T11:30:02Z" }
 * </pre>
 *
 * listUpdatedAt tells the client how fresh the data behind the answer is.
 */
public record ExitNodeResponse(String ip, Instant listUpdatedAt) {
}
