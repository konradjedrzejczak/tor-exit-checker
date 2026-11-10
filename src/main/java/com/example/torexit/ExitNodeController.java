package com.example.torexit;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * GET /A.B.C.D  : 200 + JSON if the address is a Tor exit node, 404 (empty body) if not.
 * HEAD /A.B.C.D : the same status codes with an empty body.
 *
 * <p>There is no separate HEAD method: Spring answers HEAD with the GET handler and removes the body itself.
 *
 * <p>Other answers: 400 if the path is not an IPv4 address, 503 if the exit list is not loaded yet
 * (see {@link ExitListUnavailableException}).
 */
@RestController
public class ExitNodeController {

    private final ExitNodeService exitNodeService;

    public ExitNodeController(ExitNodeService exitNodeService) {
        this.exitNodeService = exitNodeService;
    }

    @GetMapping("/{ip}")
    public ResponseEntity<ExitNodeResponse> check(@PathVariable("ip") String ip) {
        if (!Ipv4Validator.isValid(ip)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid IPv4 address");
        }
        if (!exitNodeService.isExitNode(ip)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new ExitNodeResponse(ip, exitNodeService.lastUpdated()));
    }
}
