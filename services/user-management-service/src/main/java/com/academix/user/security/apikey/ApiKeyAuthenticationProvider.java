package com.academix.user.security.apikey;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Custom Spring Security AuthenticationProvider for API Key authentication.
 * It validates the provided API key against a configured secret key.
 */
@Component
@Slf4j
public class ApiKeyAuthenticationProvider implements AuthenticationProvider {

    @Value("${academix.security.internal-api-key}")
    private String validApiKey;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ApiKeyAuthenticationToken apiKeyAuth = (ApiKeyAuthenticationToken) authentication;
        String providedApiKey = (String) apiKeyAuth.getCredentials();

        log.debug("API Key Provider: Attempting to authenticate API Key.");

        if (providedApiKey == null || providedApiKey.isEmpty()) {
            throw new BadCredentialsException("No API Key provided.");
        }

        if (providedApiKey.equals(validApiKey)) {
            // Authentication successful.
            // Grant a specific authority for internal services, e.g., "ROLE_INTERNAL_SERVICE"
            // The principal can be a simple string representing the service that used the key.
            log.info("API Key authentication successful.");
            return new ApiKeyAuthenticationToken(
                    providedApiKey,
                    "internal-service", // Principal: Represents the authenticated internal entity
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE")) // Grant a role
            );
        } else {
            log.warn("API Key authentication failed: Invalid key provided.");
            throw new BadCredentialsException("Invalid API Key.");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        // This provider only supports ApiKeyAuthenticationToken
        return ApiKeyAuthenticationToken.class.isAssignableFrom(authentication);
    }
}