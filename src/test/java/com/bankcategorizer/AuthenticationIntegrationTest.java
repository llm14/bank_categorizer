package com.bankcategorizer;

import com.bankcategorizer.dto.CategoryResponse;
import com.bankcategorizer.dto.ErrorResponse;
import com.bankcategorizer.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end coverage of US-13's login flow against a real, disposable PostgreSQL instance (via
 * Testcontainers) and the real embedded HTTP stack (no MockMvc, no mocked collaborators) - the
 * same style as {@link ImportCategorizeSpendIntegrationTest}, but exercising the auth flow itself
 * rather than treating it as a precondition.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthenticationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private ResponseEntity<LoginResponse> login(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("username", username, "password", password), headers);
        return restTemplate.postForEntity(baseUrl() + "/api/v1/auth/login", request, LoginResponse.class);
    }

    @Test
    void login_validCredentials_returns200WithToken() {
        ResponseEntity<LoginResponse> response = login("admin", "admin");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        LoginResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.token()).isNotBlank();
    }

    @Test
    void login_invalidCredentials_returns401WithGenericErrorBody() {
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                baseUrl() + "/api/v1/auth/login",
                new HttpEntity<>(Map.of("username", "admin", "password", "wrong-password"), jsonHeaders()),
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(401);
        assertThat(body.message()).isEqualTo("Invalid username or password");
    }

    @Test
    void login_blankUsername_returns400() {
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                baseUrl() + "/api/v1/auth/login",
                new HttpEntity<>(Map.of("username", "", "password", "admin"), jsonHeaders()),
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() {
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                baseUrl() + "/api/v1/categories", ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(401);
    }

    @Test
    void protectedEndpoint_withInvalidToken_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("clearly-not-a-real-token");

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                baseUrl() + "/api/v1/categories", HttpMethod.GET, new HttpEntity<>(headers), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpoint_withValidToken_returns200() {
        String token = login("admin", "admin").getBody().token();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<List<CategoryResponse>> response = restTemplate.exchange(
                baseUrl() + "/api/v1/categories", HttpMethod.GET, new HttpEntity<>(headers),
                new ParameterizedTypeReference<List<CategoryResponse>>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void logout_invalidatesToken_soSubsequentRequestReturns401() {
        String token = login("admin", "admin").getBody().token();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<Void> logoutResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/auth/logout", HttpMethod.POST, new HttpEntity<>(headers), Void.class);
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<ErrorResponse> afterLogout = restTemplate.exchange(
                baseUrl() + "/api/v1/categories", HttpMethod.GET, new HttpEntity<>(headers), ErrorResponse.class);
        assertThat(afterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logout_withoutToken_returns401() {
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                baseUrl() + "/api/v1/auth/logout", null, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void actuatorHealth_reachableWithoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl() + "/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
