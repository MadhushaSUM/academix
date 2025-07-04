package com.academix.user.security.apikey;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Custom Authentication Token for API Key based authentication.
 * It holds the API key string initially (unauthenticated)
 * and then a principal (e.g., "internal-service") when authenticated.
 */
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final String apiKey;
    private final Object principal;

    /**
     * Constructor for unauthenticated state (when filter extracts API key).
     *
     * @param apiKey The API key extracted from the request.
     */
    public ApiKeyAuthenticationToken(String apiKey) {
        super(null);
        this.apiKey = apiKey;
        this.principal = null;
        setAuthenticated(false);
    }

    /**
     * Constructor for authenticated state (after provider successfully authenticates).
     *
     * @param apiKey The API key (optional, can be null after auth).
     * @param principal The authenticated principal (e.g., service name).
     * @param authorities The authorities granted to this principal.
     */
    public ApiKeyAuthenticationToken(String apiKey, Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.apiKey = apiKey;
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return apiKey;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}