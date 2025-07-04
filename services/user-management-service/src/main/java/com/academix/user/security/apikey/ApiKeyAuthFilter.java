package com.academix.user.security.apikey;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;

/**
 * Custom Spring Security Filter to handle API Key authentication.
 * It expects the API key in the "X-API-KEY" header.
 */
@Slf4j
public class ApiKeyAuthFilter extends AbstractAuthenticationProcessingFilter {

    private static final String API_KEY_HEADER_NAME = "X-API-KEY";

    public ApiKeyAuthFilter() {
        // Specify the URL paths this filter should apply to
        // We'll configure this in SecurityConfig, but it needs a default matcher
        super(new AntPathRequestMatcher("/api/internal/**"));
        setAuthenticationSuccessHandler((request, response, authentication) -> {
            // No-op. Just continue the filter chain.
            log.debug("API Key authentication successful for principal: {}", authentication.getPrincipal());
            request.setAttribute("API_KEY_AUTHENTICATED", true); // Optional: Mark request as authenticated
            response.setStatus(HttpServletResponse.SC_OK); // Explicitly set 200 OK (default)
            // Do not call onAuthenticationSuccess of super class
        });
        setAuthenticationFailureHandler((request, response, exception) -> {
            log.warn("API Key authentication failed: {}", exception.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
            response.getWriter().write("Unauthorized: " + exception.getMessage());
        });
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {

        String apiKey = request.getHeader(API_KEY_HEADER_NAME);
        log.debug("Attempting API Key authentication. API Key provided: {}", apiKey != null ? "Yes" : "No");

        if (apiKey == null || apiKey.isEmpty()) {
            throw new BadCredentialsException("Missing or empty X-API-KEY header.");
        }

        // Create an unauthenticated token with the extracted API key
        ApiKeyAuthenticationToken authRequest = new ApiKeyAuthenticationToken(apiKey);

        // Delegate to the AuthenticationManager to authenticate the token
        return this.getAuthenticationManager().authenticate(authRequest);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        log.debug("API Key authentication successful. Setting SecurityContext.");
        SecurityContextHolder.getContext().setAuthentication(authResult);
        chain.doFilter(request, response); // Continue the filter chain
    }
}