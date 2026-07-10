package com.bankcategorizer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Enables cross-origin requests from the frontend's origin so a browser-based UI (built
 * separately, see the "Stage 5 — Frontend" user stories) can call the API. Only
 * {@code /api/v1/**} is opened up here; {@code /actuator/health} is opened up separately via the
 * {@code management.endpoints.web.cors.*} properties, since actuator endpoints are served by
 * their own {@code WebMvcEndpointHandlerMapping} rather than the {@code RequestMappingHandlerMapping}
 * that this {@link WebMvcConfigurer} configures. No other endpoint allows cross-origin requests.
 *
 * <p>The allowed origin is env-var-driven ({@code FRONTEND_ORIGIN}, resolved per Spring profile in
 * {@code application.yml} into {@code app.cors.allowed-origin}) rather than hardcoded or
 * wildcarded, following the same per-profile pattern already used for datasource configuration.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String allowedOrigin;

    public CorsConfig(@Value("${app.cors.allowed-origin}") String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/v1/**")
                .allowedOrigins(allowedOrigin)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
