package com.bankcategorizer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Backs FE-1 (CORS support) with a real, full-context regression test rather than eyeballing the
 * {@code WebMvcConfigurer} bean. Runs against a real, disposable PostgreSQL instance (via
 * Testcontainers) since actuator's health endpoint is wired into a full application context, and
 * exercises requests through the real {@code DispatcherServlet}/handler mapping chain via
 * {@link MockMvc} so CORS preflight/rejection handling is genuinely triggered, not mocked.
 *
 * <p>{@code /api/v1/**} gets its CORS handling from {@link com.bankcategorizer.config.CorsConfig}
 * (a {@code WebMvcConfigurer} registered against the app's own {@code RequestMappingHandlerMapping}).
 * {@code /actuator/health} is served by a separate {@code WebMvcEndpointHandlerMapping} that only
 * honors the {@code management.endpoints.web.cors.*} properties, so it's verified independently
 * here to prove both wiring paths actually work end-to-end, not just one of them.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class CorsConfigurationIntegrationTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";
    private static final String DISALLOWED_ORIGIN = "http://evil.example.com";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowedOrigin_apiRequest_getsCorsHeader() throws Exception {
        mockMvc.perform(get("/api/v1/categories").header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
    }

    @Test
    void disallowedOrigin_apiRequest_isRejectedWithoutCorsHeader() throws Exception {
        mockMvc.perform(get("/api/v1/categories").header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void preflightRequest_allowedOrigin_isHandledWithCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/v1/categories")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("PATCH")));
    }

    @Test
    void allowedOrigin_actuatorHealth_getsCorsHeader() throws Exception {
        mockMvc.perform(get("/actuator/health").header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
    }

    @Test
    void disallowedOrigin_actuatorHealth_isRejectedWithoutCorsHeader() throws Exception {
        mockMvc.perform(get("/actuator/health").header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
