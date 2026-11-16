package com.example.torexit;

import static com.example.torexit.Fixtures.EXIT_1;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.example.torexit.TestHttpClient.Response;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * What a client sees when the Tor Project cannot be reached and no list was ever downloaded: the service
 * answers 503 ("I can't tell") and reports itself as not healthy. It must never answer 404, because that
 * would claim the address is not an exit node.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TorExitCheckerWithoutListEndToEndTest {

    @DynamicPropertySource
    static void pointTheServiceAtAnUnreachableAddress(DynamicPropertyRegistry registry) {
        String unreachable = FakeTorServer.unreachableUri().toString();
        registry.add("tor.exit-list.url", () -> unreachable);
    }

    @LocalServerPort
    private int port;

    @Test
    void answers503ForBothMethods() {
        // given
        TestHttpClient http = new TestHttpClient(port);

        // when
        Response get = http.send("GET", "/" + EXIT_1);
        Response head = http.send("HEAD", "/" + EXIT_1);

        // then
        assertSoftly(softly -> {
            softly.assertThat(get.status()).isEqualTo(503);
            softly.assertThat(head.status()).isEqualTo(503);
        });
    }

    @Test
    void healthIsDown() {
        // when
        Response response = new TestHttpClient(port).send("GET", "/actuator/health");

        // then
        assertSoftly(softly -> {
            softly.assertThat(response.status()).isEqualTo(503);
            softly.assertThat(response.body()).contains("\"torExitList\":{\"status\":\"DOWN\"");
        });
    }
}
