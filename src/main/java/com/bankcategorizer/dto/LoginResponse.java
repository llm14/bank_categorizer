package com.bankcategorizer.dto;

/**
 * Response body for {@code POST /api/v1/auth/login}: an opaque bearer token to send as
 * {@code Authorization: Bearer <token>} on every subsequent {@code /api/v1/**} request.
 */
public record LoginResponse(String token) {
}
