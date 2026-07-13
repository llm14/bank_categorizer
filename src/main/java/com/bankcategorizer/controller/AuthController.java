package com.bankcategorizer.controller;

import com.bankcategorizer.dto.LoginRequest;
import com.bankcategorizer.dto.LoginResponse;
import com.bankcategorizer.service.TokenStore;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login/logout endpoints for the single-user credential pair configured via
 * {@code AUTH_USERNAME}/{@code AUTH_PASSWORD} (see {@link com.bankcategorizer.config.SecurityConfig}).
 * Both endpoints live under {@code /api/v1/auth}; only {@code /login} is reachable without a
 * valid bearer token — {@code /logout} requires one, same as every other {@code /api/v1/**}
 * endpoint.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthenticationManager authenticationManager;
    private final TokenStore tokenStore;

    public AuthController(AuthenticationManager authenticationManager, TokenStore tokenStore) {
        this.authenticationManager = authenticationManager;
        this.tokenStore = tokenStore;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // Throws AuthenticationException (e.g. BadCredentialsException) on invalid credentials,
        // handled centrally by GlobalExceptionHandler into a 401 with the standard error shape.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        String token = tokenStore.issueToken();
        return ResponseEntity.ok(new LoginResponse(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        String token = authorizationHeader.startsWith(BEARER_PREFIX)
                ? authorizationHeader.substring(BEARER_PREFIX.length())
                : authorizationHeader;
        tokenStore.invalidate(token);
        return ResponseEntity.noContent().build();
    }
}
