package com.example.torexit;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /A.B.C.D  : 200 + JSON if the address is a Tor exit node, 404 (empty body) if not.
 * HEAD /A.B.C.D : the same status codes with an empty body.
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

    @RequestMapping(path = "/{ip}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> head(@PathVariable("ip") String text) {
        return ResponseEntity.status(check(text).getStatusCode()).build();
    }
}
