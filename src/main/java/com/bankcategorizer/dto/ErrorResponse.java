package com.bankcategorizer.dto;

import java.time.OffsetDateTime;

/**
 * Consistent error body returned by {@link com.bankcategorizer.exception.GlobalExceptionHandler}.
 */
public record ErrorResponse(OffsetDateTime timestamp, int status, String error, String message) {
}
