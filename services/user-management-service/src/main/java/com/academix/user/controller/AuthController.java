package com.academix.user.controller;

import com.academix.user.dto.LoginRequest;
import com.academix.user.dto.LoginResponse;
import com.academix.user.dto.RegisterRequest;
import com.academix.user.model.User;
import com.academix.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Handles user registration requests.
     * @param request The registration request payload.
     * @return ResponseEntity with the registered user details or an error message.
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request) {
        try {
            User registeredUser = authService.registerUser(request);
            // Return a simplified response for registration, avoiding sensitive data
            return ResponseEntity.status(HttpStatus.CREATED).body("User " + registeredUser.getUsername() + " registered successfully.");
        } catch (IllegalArgumentException e) {
            log.error("Registration error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage()); // 409 Conflict for existing user/email
        } catch (Exception e) {
            log.error("An unexpected error occurred during registration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    /**
     * Handles user login requests.
     * @param request The login request payload.
     * @return ResponseEntity with the JWT token upon successful login or an error message.
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.authenticateUser(request);
            return ResponseEntity.ok(response); // Return 200 OK with JWT
        } catch (BadCredentialsException e) {
            log.warn("Login failed for user '{}': Invalid credentials.", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password."); // 401 Unauthorized
        } catch (Exception e) {
            log.error("An unexpected error occurred during login for user '{}': {}", request.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred during login.");
        }
    }

    @GetMapping("/hello")
    public ResponseEntity<String> sayHello() {
        log.debug("/auth/hello Endpoint triggered");
        return ResponseEntity.ok("Hello from user-management-service");
    }
}
