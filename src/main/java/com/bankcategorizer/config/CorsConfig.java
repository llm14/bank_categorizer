package com.bankcategorizer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Enables cross-origin requests from the frontend's origin so a browser-based UI (built
 * separately, see the "Stage 5 — Frontend" user stories) can call the API. Only
 * {@code /api/v1/**} is opened up here; {@code /actuator/health} is opened up separately via the
 * {@code management.endpoints.web.cors.*} properties, since actuator endpoints are served by
 * their own {@code WebMvcEndpointHandlerMapping}. No other endpoint allows cross-origin requests.
 *
 * <p>The allowed origin is env-var-driven ({@code FRONTEND_ORIGIN}, resolved per Spring profile in
 * {@code application.yml} into {@code app.cors.allowed-origin}) rather than hardcoded or
 * wildcarded, following the same per-profile pattern already used for datasource configuration.
 *
 * <p>Exposes a {@link CorsConfigurationSource} bean (rather than a {@code WebMvcConfigurer}) so
 * {@link SecurityConfig} can wire it into Spring Security's own {@code CorsFilter}, which runs
 * early in the security filter chain — before the {@code AuthorizationFilter} — so CORS preflight
 * requests and disallowed-origin rejections are handled the same way they were before
 * authentication existed, without needing a bearer token first.
 */
@Configuration
public class CorsConfig {

    private final String allowedOrigin;

    public CorsConfig(@Value("${app.cors.allowed-origin}") String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigin));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // Per the Fetch/CORS spec, "Access-Control-Allow-Headers: *" does NOT cover the
        // Authorization header - browsers require it listed explicitly even alongside a
        // wildcard - so it's listed here rather than relying on "*" alone (verified with a real
        // cross-origin request carrying an Authorization header, not just by reading this config).
        configuration.setAllowedHeaders(List.of("Authorization", "*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**", configuration);
        return source;
    }
}
