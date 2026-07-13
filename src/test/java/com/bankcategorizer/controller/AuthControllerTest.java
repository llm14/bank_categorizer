package com.bankcategorizer.controller;

import com.bankcategorizer.config.ClockConfig;
import com.bankcategorizer.service.TokenStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Security filters (see SecurityConfig, added by US-13) are disabled here: this slice tests
// controller logic against mocked collaborators, not the real filter chain - the real
// authenticate-then-call-a-protected-endpoint flow is covered by the Testcontainers-backed
// AuthenticationIntegrationTest instead.
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ClockConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private TokenStore tokenStore;

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        given(authenticationManager.authenticate(any()))
                .willReturn(new UsernamePasswordAuthenticationToken("admin", "admin"));
        given(tokenStore.issueToken()).willReturn("abc123token");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"admin"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("abc123token"));
    }

    @Test
    void login_blankUsername_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","password":"admin"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_blankPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_invalidCredentials_returns401WithGenericErrorBody() throws Exception {
        willThrow(new BadCredentialsException("Bad credentials"))
                .given(authenticationManager).authenticate(any());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void logout_validToken_returns204AndInvalidatesToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer abc123token"))
                .andExpect(status().isNoContent());

        verify(tokenStore).invalidate("abc123token");
    }
}
