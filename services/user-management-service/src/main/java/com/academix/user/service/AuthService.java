package com.academix.user.service;

import com.academix.user.dto.AuthResponseDto;
import com.academix.user.dto.LoginRequestDto;
import com.academix.user.dto.RegisterRequestDto;
import com.academix.user.exception.InvalidCredentialsException;
import com.academix.user.exception.ResourceNotFoundException;
import com.academix.user.exception.UserAlreadyExistsException;
import com.academix.user.model.Role;
import com.academix.user.model.User;
import com.academix.user.repository.UserRepository;
import com.academix.user.util.JwtTokenProvider; // We'll create this in Phase 4
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j // For logging
public class AuthService {

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponseDto registerUser(RegisterRequestDto request) {
        log.info("Attempting to register user: {}", request.getUsername());

        // 1. Check if username or email already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username '" + request.getUsername() + "' is already taken.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email '" + request.getEmail() + "' is already registered.");
        }

        // 2. Find the default role (e.g., ROLE_USER)
        Role userRole = roleService.findByName(Role.RoleName.ROLE_USER)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", Role.RoleName.ROLE_USER.name()));

        // 3. Create new user entity
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .roles(Collections.singleton(userRole))
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        // 4. Save user to database
        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getUsername());

        // 5. Authenticate user and generate token immediately after registration
        // This re-uses the login logic to get an AuthResponseDto
        return loginUser(LoginRequestDto.builder()
                .identifier(request.getUsername())
                .password(request.getPassword())
                .build());
    }

    @Transactional(readOnly = true)
    public AuthResponseDto loginUser(LoginRequestDto request) {
        log.info("Attempting to log in user: {}", request.getIdentifier());

        try {
            // 1. Authenticate user using Spring Security's AuthenticationManager
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getIdentifier(), request.getPassword())
            );

            // 2. Set authentication in SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 3. Generate JWT token
            String jwt = jwtTokenProvider.generateToken(authentication);
            User user = (User) authentication.getPrincipal();

            log.info("User logged in successfully: {}", user.getUsername());

            // 4. Build and return AuthResponseDto
            return AuthResponseDto.builder()
                    .accessToken(jwt)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getJwtExpirationInMillis() / 1000)
                    .userId(user.getId().toString())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .build();

        } catch (AuthenticationException e) {
            log.warn("Authentication failed for user {}: {}", request.getIdentifier(), e.getMessage());
            throw new InvalidCredentialsException("Invalid username/email or password.");
        }
    }
}