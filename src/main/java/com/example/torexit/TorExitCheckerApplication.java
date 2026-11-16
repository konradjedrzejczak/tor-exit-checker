package com.example.torexit;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan // finds TorExitProperties
@EnableScheduling // needed for @Scheduled in ExitListRefresher
public class TorExitCheckerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TorExitCheckerApplication.class, args);
    }

    // The service asks this clock for the time instead of calling Instant.now(),
    // so tests can move time forward without waiting.
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
