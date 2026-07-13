package com.bankcategorizer.config;

import com.bankcategorizer.service.TokenStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Validates an {@code Authorization: Bearer <token>} header against {@link TokenStore} and, if
 * valid, populates the {@link org.springframework.security.core.context.SecurityContext} so
 * downstream {@code authorizeHttpRequests} rules see the request as authenticated. Missing or
 * invalid tokens simply leave the context unauthenticated - {@link SecurityConfig}'s
 * {@code authorizeHttpRequests}/{@code exceptionHandling} then reject the request with a 401
 * (via {@link com.bankcategorizer.exception.RestAuthenticationEntryPoint}) for any endpoint that
 * requires authentication.
 *
 * <p>Deliberately <em>not</em> a scanned {@code @Component}: {@code @WebMvcTest} slices include
 * any {@code Filter} bean found via component scanning regardless of the sliced controller, which
 * would fail those slices' context loading (this filter's {@link TokenStore} dependency, a
 * {@code @Service}, isn't part of a controller slice). It's instead wired explicitly as a
 * {@code @Bean} inside {@link SecurityConfig}, which itself isn't picked up by
 * {@code @WebMvcTest}'s exclude filter, keeping it out of slice test contexts entirely.
 */
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenStore tokenStore;

    public TokenAuthenticationFilter(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            if (tokenStore.isValid(token)) {
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        "authenticated-user", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
