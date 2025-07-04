package com.academix.user.config;

import com.academix.user.security.apikey.ApiKeyAuthFilter;
import com.academix.user.security.apikey.ApiKeyAuthenticationProvider;
import com.academix.user.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Collections;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthFilter jwtAuthFilter;
    private final ApiKeyAuthenticationProvider apiKeyAuthenticationProvider;

    /**
     * Configures the password encoder bean.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures the DaoAuthenticationProvider to use our CustomUserDetailsService
     * and the configured PasswordEncoder.
     */
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Exposes the AuthenticationManager bean for JWT/DaoAuthentication.
     * This manager uses the DaoAuthenticationProvider.
     */
    @Bean
    public AuthenticationManager jwtAuthenticationManager() {
        return new ProviderManager(Collections.singletonList(daoAuthenticationProvider()));
    }

    /**
     * Exposes the AuthenticationManager bean specifically for API Key authentication.
     * This manager uses only the ApiKeyAuthenticationProvider.
     */
    @Bean
    public AuthenticationManager apiKeyAuthenticationManager() {
        return new ProviderManager(Collections.singletonList(apiKeyAuthenticationProvider));
    }

    /**
     * Defines the ApiKeyAuthFilter bean.
     * It needs to be a bean so Spring can manage its lifecycle and inject its AuthenticationManager.
     */
    @Bean
    public ApiKeyAuthFilter apiKeyAuthFilter() {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter();
        filter.setAuthenticationManager(apiKeyAuthenticationManager()); // Set the dedicated API Key manager
        return filter;
    }

    /**
     * 1. Security Filter Chain for Internal APIs (highest precedence).
     * This chain applies ONLY to requests matching "/api/internal/**" paths.
     * It uses API Key authentication.
     */
    @Bean
    @Order(1) // This chain will be processed first
    public SecurityFilterChain internalApiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/internal/**") // This chain applies ONLY to /api/internal/** paths
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for API
                .authorizeHttpRequests(authorize -> authorize
                                .requestMatchers("/api/internal/**").authenticated() // All internal endpoints must be authenticated
                        // .anyRequest().denyAll() // Optional: Deny any other requests not matching this matcher within this chain
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(apiKeyAuthFilter(), UsernamePasswordAuthenticationFilter.class); // Add our custom filter

        // If you need custom error handling for API Key auth, you might re-introduce JwtAuthenticationEntryPoint here
        // .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(jwtAuthenticationEntryPoint));

        return http.build();
    }

    /**
     * 2. Security Filter Chain for Public/User-facing APIs (lower precedence).
     * This chain handles all other requests not matched by the first chain.
     * It uses JWT authentication.
     */
    @Bean
    @Order(2) // This chain will be processed second
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable) //TODO: configure CORS properly instead of disabling in prod
                .authorizeHttpRequests(authorize -> authorize
                        // Public endpoints - accessible without authentication
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh-token").permitAll()
                        .requestMatchers("/actuator/**").permitAll() //TODO: restrict this in prod
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Use stateless sessions (no HttpSession)
                )
                .authenticationProvider(daoAuthenticationProvider()) // Register custom authentication provider
                // Add custom JWT filter before Spring Security's UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}