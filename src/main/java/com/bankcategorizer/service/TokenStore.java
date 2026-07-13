package com.bankcategorizer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side store of issued bearer tokens for the single-user login flow (US-13): a simple
 * in-memory {@code token -> expiry} map, not a self-signed JWT or a database table — no signing
 * library is needed for a single-credential app, and losing all issued tokens on app restart is
 * an acceptable tradeoff for a single-user, local-first tool.
 *
 * <p>Tokens are opaque, {@link SecureRandom}-backed random strings with a fixed (non-sliding)
 * expiry ({@code app.auth.token-ttl}, default 24h) rather than a sliding one.
 */
@Service
public class TokenStore {

    private static final int TOKEN_BYTES = 32;

    private final Clock clock;
    private final Duration tokenTtl;
    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentHashMap<String, Instant> tokenExpiries = new ConcurrentHashMap<>();

    public TokenStore(Clock clock, @Value("${app.auth.token-ttl:PT24H}") Duration tokenTtl) {
        this.clock = clock;
        this.tokenTtl = tokenTtl;
    }

    /**
     * Issues and stores a new random token, valid until now + the configured TTL.
     */
    public String issueToken() {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        tokenExpiries.put(token, Instant.now(clock).plus(tokenTtl));
        return token;
    }

    /**
     * Returns whether the given token is present and not yet expired; expired tokens are
     * evicted as a side effect.
     */
    public boolean isValid(String token) {
        Instant expiry = tokenExpiries.get(token);
        if (expiry == null) {
            return false;
        }
        if (expiry.isBefore(Instant.now(clock))) {
            tokenExpiries.remove(token);
            return false;
        }
        return true;
    }

    /**
     * Invalidates (removes) a token, e.g. on logout. A no-op if the token isn't present.
     */
    public void invalidate(String token) {
        tokenExpiries.remove(token);
    }
}
