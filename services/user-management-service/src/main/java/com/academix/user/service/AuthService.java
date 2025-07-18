package com.academix.user.service;

import com.academix.user.dto.LoginRequest;
import com.academix.user.dto.LoginResponse;
import com.academix.user.dto.RegisterRequest;
import com.academix.user.model.Role;
import com.academix.user.model.User;
import com.academix.user.repository.RoleRepository;
import com.academix.user.repository.UserRepository;
import com.academix.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    /**
     * Registers a new user with the system.
     * @param request The registration request containing username, password, and email.
     * @return The registered User object.
     * @throws IllegalArgumentException if username or email already exists.
     */
    public User registerUser(RegisterRequest request) {
        log.info("Attempting to register user: {}", request.getUsername());

        // Check if username already exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            log.warn("Registration failed: Username '{}' already exists.", request.getUsername());
            throw new IllegalArgumentException("Username already exists.");
        }

        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Registration failed: Email '{}' already exists.", request.getEmail());
            throw new IllegalArgumentException("Email already exists.");
        }

        // Assign default role (e.g., STUDENT)
        // Retrieve the Role entity from the database
        Role studentRole = roleRepository.findByName(Role.RoleName.ROLE_STUDENT)
                .orElseThrow(() -> new IllegalStateException("Default role 'ROLE_STUDENT' not found. Please initialize roles in the database."));

        Set<Role> roles = new HashSet<>(Collections.singletonList(studentRole));

        // Build the new User entity
        User newUser = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword())) // Encode password before saving
                .email(request.getEmail())
                .firstName(request.getFirstName()) // Add firstName
                .lastName(request.getLastName())   // Add lastName
                .roles(roles)
                .build();

        // Save the user to the database
        User savedUser = userRepository.save(newUser);
        log.info("User '{}' registered successfully with ID: {}", savedUser.getUsername(), savedUser.getId());
        return savedUser;
    }

    /**
     * Authenticates a user and generates a JWT token.
     * @param request The login request containing username and password.
     * @return A LoginResponse containing the JWT token.
     * @throws AuthenticationException if authentication fails.
     */
    public LoginResponse authenticateUser(LoginRequest request) {
        log.info("Attempting to authenticate user: {}", request.getUsername());
        try {
            // Authenticate the user using Spring Security's AuthenticationManager
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            // If authentication is successful, generate a JWT token
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String jwt = jwtUtil.generateToken(userDetails);
            log.info("User '{}' authenticated successfully. JWT generated.", request.getUsername());
            return new LoginResponse(jwt);

        } catch (AuthenticationException e) {
            log.error("Authentication failed for user '{}': {}", request.getUsername(), e.getMessage());
            throw e; // Re-throw the authentication exception
        }
    }
}
