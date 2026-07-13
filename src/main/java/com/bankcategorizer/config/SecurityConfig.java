package com.bankcategorizer.config;

import com.bankcategorizer.exception.RestAuthenticationEntryPoint;
import com.bankcategorizer.service.TokenStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Wires US-13's single-user, bearer-token login: a single hardcoded credential pair
 * ({@code AUTH_USERNAME}/{@code AUTH_PASSWORD}, see {@code application.yml}) checked via Spring
 * Security's normal {@link UserDetailsService}/{@link PasswordEncoder} machinery (the configured
 * password is BCrypt-hashed at startup rather than compared as plaintext) instead of a one-off
 * hand-rolled comparison.
 *
 * <p>Stateless: no {@code HttpSession}, no CSRF (this is a JSON API, not a browser form-post
 * app) - auth state lives entirely in {@link com.bankcategorizer.service.TokenStore}, checked on
 * every request by {@link TokenAuthenticationFilter}. {@code /api/v1/auth/login} and
 * {@code /actuator/health} are the only endpoints reachable without a valid bearer token;
 * everything else under {@code /api/v1/**} (including {@code /api/v1/auth/logout}) requires one.
 *
 * <p>CORS is wired here via the {@link CorsConfigurationSource} bean from {@link CorsConfig} so
 * Spring Security's own {@code CorsFilter} (which runs early in the chain, before the
 * {@code AuthorizationFilter}) keeps handling preflight/disallowed-origin requests exactly as it
 * did before authentication existed, without requiring a token first.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource,
                           RestAuthenticationEntryPoint restAuthenticationEntryPoint) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
    }

    // Wired as an explicit @Bean (rather than a scanned @Component) so it's only ever
    // instantiated as part of this configuration - see TokenAuthenticationFilter's javadoc for
    // why a scanned @Component would break every @WebMvcTest slice's context loading.
    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter(TokenStore tokenStore) {
        return new TokenAuthenticationFilter(tokenStore);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, TokenAuthenticationFilter tokenAuthenticationFilter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling.authenticationEntryPoint(restAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder,
                                                  @Value("${app.auth.username}") String username,
                                                  @Value("${app.auth.password}") String password) {
        UserDetails user = User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    // No explicit AuthenticationProvider bean needed: Spring Security's
    // InitializeUserDetailsBeanManagerConfigurer automatically builds a DaoAuthenticationProvider
    // from the UserDetailsService + PasswordEncoder beans above.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
