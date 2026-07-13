package com.bankcategorizer.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TokenStoreTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-07-13T10:00:00Z");

    @Test
    void issueToken_thenIsValid_returnsTrue() {
        TokenStore tokenStore = new TokenStore(Clock.fixed(FIXED_NOW, ZoneOffset.UTC), Duration.ofHours(24));

        String token = tokenStore.issueToken();

        assertThat(token).isNotBlank();
        assertThat(tokenStore.isValid(token)).isTrue();
    }

    @Test
    void issueToken_calledTwice_returnsDistinctTokens() {
        TokenStore tokenStore = new TokenStore(Clock.fixed(FIXED_NOW, ZoneOffset.UTC), Duration.ofHours(24));

        String first = tokenStore.issueToken();
        String second = tokenStore.issueToken();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void isValid_unknownToken_returnsFalse() {
        TokenStore tokenStore = new TokenStore(Clock.fixed(FIXED_NOW, ZoneOffset.UTC), Duration.ofHours(24));

        assertThat(tokenStore.isValid("nonexistent-token")).isFalse();
    }

    @Test
    void isValid_alreadyExpiredTtl_returnsFalse() {
        // A token issued with a negative TTL is immediately expired relative to "now" on the very
        // next check, exercising the expiry branch without needing to advance a clock mid-test.
        TokenStore tokenStore = new TokenStore(Clock.fixed(FIXED_NOW, ZoneOffset.UTC), Duration.ofSeconds(-1));

        String token = tokenStore.issueToken();

        assertThat(tokenStore.isValid(token)).isFalse();
    }

    @Test
    void invalidate_removesToken() {
        TokenStore tokenStore = new TokenStore(Clock.fixed(FIXED_NOW, ZoneOffset.UTC), Duration.ofHours(24));
        String token = tokenStore.issueToken();

        tokenStore.invalidate(token);

        assertThat(tokenStore.isValid(token)).isFalse();
    }

    @Test
    void invalidate_unknownToken_isNoOp() {
        TokenStore tokenStore = new TokenStore(Clock.fixed(FIXED_NOW, ZoneOffset.UTC), Duration.ofHours(24));

        tokenStore.invalidate("nonexistent-token");

        assertThat(tokenStore.isValid("nonexistent-token")).isFalse();
    }
}
