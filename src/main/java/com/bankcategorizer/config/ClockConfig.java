package com.bankcategorizer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Exposes the system {@link Clock} as a bean so time-dependent services (e.g. period
 * comparisons that need "today") can depend on it rather than calling
 * {@code LocalDate.now()}/{@code YearMonth.now()} directly, keeping them testable with a
 * fixed {@link Clock} in unit tests.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
