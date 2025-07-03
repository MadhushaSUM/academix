package com.academix.user.controller;

import com.academix.user.dto.AuthResponseDto;
import com.academix.user.dto.LoginRequestDto;
import com.academix.user.dto.RegisterRequestDto;
import com.academix.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for user authentication and registration.
 * Handles API requests related to user signup, login, password reset, etc.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j // For logging
public class AuthController {

    private final AuthService authService;

    /**
     * Handles user registration requests.
     * Endpoint: POST /api/v1/auth/register
     *
     * @param request The RegisterRequestDto containing user registration details.
     * @return ResponseEntity with AuthResponseDto on success, or error details on failure.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> registerUser(@Valid @RequestBody RegisterRequestDto request) {
        log.info("Received registration request for username: {}", request.getUsername());
        AuthResponseDto response = authService.registerUser(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Handles user login requests.
     * Endpoint: POST /api/v1/auth/login
     *
     * @param request The LoginRequestDto containing user login credentials.
     * @return ResponseEntity with AuthResponseDto on success, or error details on failure.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> loginUser(@Valid @RequestBody LoginRequestDto request) {
        log.info("Received login request for identifier: {}", request.getIdentifier());
        AuthResponseDto response = authService.loginUser(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // - Token Refresh (/refresh-token)
    // - Forgot Password (/forgot-password)
    // - Reset Password (/reset-password)
}