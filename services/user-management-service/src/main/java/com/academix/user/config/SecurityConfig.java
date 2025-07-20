package com.academix.user.config;

import com.academix.user.filter.GatewayAuthenticationFilter;
import com.academix.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private UserRepository userRepository;

    private final GatewayAuthenticationFilter gatewayAuthenticationFilter;

    public SecurityConfig(GatewayAuthenticationFilter gatewayAuthenticationFilter) {
        this.gatewayAuthenticationFilter = gatewayAuthenticationFilter;
    }

    /**
     * Configures the security filter chain for HTTP requests.
     * @param http HttpSecurity object to configure.
     * @return The configured SecurityFilterChain.
     * @throws Exception if an error occurs during configuration.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public authentication endpoints
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        // Private endpoints - require gateway authentication
                        .requestMatchers("/users/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(gatewayAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Defines a custom UserDetailsService to load user details from our UserRepository.
     * Spring Security will use this to retrieve user information during authentication.
     * @return An implementation of UserDetailsService.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    /**
     * Provides the AuthenticationProvider to be used by the AuthenticationManager.
     * It uses our UserDetailsService and PasswordEncoder.
     * @return An AuthenticationProvider instance.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService()); // Set our custom UserDetailsService
        authProvider.setPasswordEncoder(passwordEncoder()); // Set the password encoder
        return authProvider;
    }

    /**
     * Provides the AuthenticationManager bean.
     * This bean is used to perform authentication.
     * @param config AuthenticationConfiguration to get the AuthenticationManager.
     * @return The AuthenticationManager.
     * @throws Exception if an error occurs.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Provides a BCryptPasswordEncoder bean for password hashing.
     * @return A BCryptPasswordEncoder instance.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
