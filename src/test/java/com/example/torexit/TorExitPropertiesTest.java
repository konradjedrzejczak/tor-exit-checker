package com.example.torexit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class TorExitPropertiesTest {

    private static final URI URL = URI.create("https://check.torproject.org/exit-addresses");
    private static final Duration OK = Duration.ofSeconds(5);

    @Test
    void acceptsValidSettings() {
        // when
        TorExitProperties properties = new TorExitProperties(URL, Duration.ofMinutes(30), OK, OK, OK);

        // then
        assertThat(properties.cacheTtl()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void rejectsAUrlThatIsNotHttp() {
        // when / then
        assertThatThrownBy(() -> new TorExitProperties(URI.create("ftp://example.com/list"), OK, OK, OK, OK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("url");
        assertThatThrownBy(() -> new TorExitProperties(null, OK, OK, OK, OK))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingZeroOrNegativeDurations() {
        // when / then
        assertThatThrownBy(() -> new TorExitProperties(URL, Duration.ZERO, OK, OK, OK))
                .hasMessageContaining("cache-ttl");
        assertThatThrownBy(() -> new TorExitProperties(URL, OK, Duration.ofSeconds(-1), OK, OK))
                .hasMessageContaining("check-interval");
        assertThatThrownBy(() -> new TorExitProperties(URL, OK, OK, null, OK))
                .hasMessageContaining("connect-timeout");
        assertThatThrownBy(() -> new TorExitProperties(URL, OK, OK, OK, Duration.ZERO))
                .hasMessageContaining("read-timeout");
    }

    // ---- binding from property names, as in application.yml ----

    @Configuration
    @EnableConfigurationProperties(TorExitProperties.class)
    static class PropertiesConfig {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(PropertiesConfig.class);

    @Test
    void bindsValuesFromConfiguration() {
        runner.withPropertyValues(
                        "tor.exit-list.url=https://example.com/list",
                        "tor.exit-list.cache-ttl=PT15M",
                        "tor.exit-list.check-interval=PT30S",
                        "tor.exit-list.connect-timeout=PT2S",
                        "tor.exit-list.read-timeout=PT10S")
                .run(context -> {
                    // then
                    TorExitProperties properties = context.getBean(TorExitProperties.class);
                    assertThat(properties.url()).isEqualTo(URI.create("https://example.com/list"));
                    assertThat(properties.cacheTtl()).isEqualTo(Duration.ofMinutes(15));
                    assertThat(properties.checkInterval()).isEqualTo(Duration.ofSeconds(30));
                });
    }

    @Test
    void applicationFailsToStartWhenASettingIsInvalid() {
        runner.withPropertyValues(
                        "tor.exit-list.url=https://example.com/list",
                        "tor.exit-list.cache-ttl=PT0S",
                        "tor.exit-list.check-interval=PT30S",
                        "tor.exit-list.connect-timeout=PT2S",
                        "tor.exit-list.read-timeout=PT10S")
                .run(context -> assertThat(context).hasFailed());
    }
}
