package com.example.torexit;

import static com.example.torexit.Fixtures.EXIT_1;
import static com.example.torexit.Fixtures.NOT_EXIT;
import static com.example.torexit.Fixtures.SAMPLE_EXIT_LIST;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.example.torexit.TestHttpClient.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * End to end: the real application on a real port, a real HTTP client, and a local server standing in for the
 * Tor Project. Nothing is mocked.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TorExitCheckerEndToEndTest {

    // Started when the class is loaded, so it is already running when Spring reads the property below.
    private static final FakeTorServer TOR = new FakeTorServer(SAMPLE_EXIT_LIST);

    @DynamicPropertySource
    static void pointTheServiceAtTheFakeTorServer(DynamicPropertyRegistry registry) {
        registry.add("tor.exit-list.url", () -> TOR.uri().toString());
    }

    @AfterAll
    static void stopTheFakeTorServer() {
        TOR.close();
    }

    @LocalServerPort
    private int port;

    @Autowired
    private ExitListRefresher refresher;

    private TestHttpClient http;

    @BeforeEach
    void givenTheExitListIsLoaded() {
        // The background job does this by itself right after start-up; calling it here means the tests never
        // have to wait for it.
        refresher.refreshIfExpired();
        http = new TestHttpClient(port);
    }

    @Test
    void headIs200WithEmptyBodyForAnExitNode() {
        // when
        Response response = http.send("HEAD", "/" + EXIT_1);

        // then
        assertSoftly(softly -> {
            softly.assertThat(response.status()).isEqualTo(200);
            softly.assertThat(response.body()).isEmpty();
        });
    }

    @Test
    void headIs404WithEmptyBodyForAnAddressThatIsNotAnExitNode() {
        // when
        Response response = http.send("HEAD", "/" + NOT_EXIT);

        // then
        assertSoftly(softly -> {
            softly.assertThat(response.status()).isEqualTo(404);
            softly.assertThat(response.body()).isEmpty();
        });
    }

    @Test
    void getReturnsJsonForAnExitNode() {
        // when
        Response response = http.send("GET", "/" + EXIT_1);

        // then
        assertSoftly(softly -> {
            softly.assertThat(response.status()).isEqualTo(200);
            softly.assertThat(response.header("Content-Type")).startsWith("application/json");
            softly.assertThat(response.body()).contains("\"ip\":\"" + EXIT_1 + "\"").contains("\"listUpdatedAt\":\"");
        });
    }

    @Test
    void getIs404WithEmptyBodyForAnAddressThatIsNotAnExitNode() {
        // when
        Response response = http.send("GET", "/" + NOT_EXIT);

        // then
        assertSoftly(softly -> {
            softly.assertThat(response.status()).isEqualTo(404);
            softly.assertThat(response.body()).isEmpty();
        });
    }

    @Test
    void invalidAddressesAreRejectedWith400() {
        for (String invalid : new String[] {"999.1.1.1", "1.2.3", "01.2.3.4", "not-an-ip", "1.2.3.4.5"}) {
            // when
            Response get = http.send("GET", "/" + invalid);
            Response head = http.send("HEAD", "/" + invalid);

            // then
            assertSoftly(softly -> {
                softly.assertThat(get.status()).as("GET /%s", invalid).isEqualTo(400);
                softly.assertThat(head.status()).as("HEAD /%s", invalid).isEqualTo(400);
            });
        }
    }

    @Test
    void otherMethodsAreNotAllowed() {
        // when / then
        assertSoftly(softly -> {
            softly.assertThat(http.send("POST", "/" + EXIT_1).status()).isEqualTo(405);
            softly.assertThat(http.send("DELETE", "/" + EXIT_1).status()).isEqualTo(405);
        });
    }

    @Test
    void healthIsUpOnceTheListIsLoaded() {
        // when
        Response response = http.send("GET", "/actuator/health");

        // then
        assertSoftly(softly -> {
            softly.assertThat(response.status()).isEqualTo(200);
            softly.assertThat(response.body()).isEqualTo("{\"status\":\"UP\"}");
        });
    }
}
