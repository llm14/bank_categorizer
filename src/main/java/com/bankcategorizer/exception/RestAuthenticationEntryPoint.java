package com.bankcategorizer.exception;

import com.bankcategorizer.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * Spring Security's default {@link AuthenticationEntryPoint} writes its own error body/format,
 * not this project's {@link ErrorResponse} shape used everywhere else (see
 * {@link GlobalExceptionHandler}). This entry point is invoked directly by Spring Security's
 * {@code ExceptionTranslationFilter} for a missing/invalid bearer token on a protected endpoint -
 * i.e. before the request ever reaches a controller/{@code @RestControllerAdvice} - so it mirrors
 * {@code GlobalExceptionHandler#buildResponse} by hand rather than being able to reuse it
 * directly, keeping every error response in this API looking the same, auth included.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(RestAuthenticationEntryPoint.class);

    private final Clock clock;
    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(Clock clock, ObjectMapper objectMapper) {
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        log.warn("Rejecting unauthenticated request to {} {}: {}", request.getMethod(), request.getRequestURI(), authException.getMessage());

        HttpStatus status = HttpStatus.UNAUTHORIZED;
        ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now(clock), status.value(), status.getReasonPhrase(),
                "Authentication is required to access this resource");

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
