package com.example.torexit;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /A.B.C.D  : 200 + JSON if the address is a Tor exit node, 404 (empty body) if not.
 * HEAD /A.B.C.D : the same status codes with an empty body.
 *
 * <p>There is no separate HEAD method: Spring answers HEAD requests with the GET handler, and the web server
 * leaves out the body. (The end-to-end test checks this on a real server.)
 *
 * <p>Other answers: 400 if the path is not an IPv4 address (see {@link InvalidIpAddressException}), 503 if the
 * exit list is not loaded yet (see {@link ExitListUnavailableException}).
 */
@RestController
public class ExitNodeController {

    private final ExitNodeRegistry registry;

    public ExitNodeController(ExitNodeRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/{ip}")
    public ResponseEntity<ExitNodeResponse> check(@PathVariable("ip") String text) {
        Ipv4Address ip = Ipv4Address.parse(text); // 400 if it is not a valid IPv4 address
        if (!registry.isExitNode(ip)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new ExitNodeResponse(ip.toString(), registry.lastUpdated()));
    }
}
