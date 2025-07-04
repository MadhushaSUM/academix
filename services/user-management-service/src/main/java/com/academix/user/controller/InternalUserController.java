package com.academix.user.controller;

import com.academix.user.dto.InternalUserDto;
import com.academix.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for internal (service-to-service) user data retrieval.
 * These endpoints are secured by API Key authentication.
 */
@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
@Slf4j
public class InternalUserController {

    private final UserService userService;

    /**
     * Retrieves internal user data by user ID.
     * Requires API Key authentication.
     */
    @GetMapping("/by-id/{userId}")
    public ResponseEntity<InternalUserDto> getInternalUserById(@PathVariable Long userId) {
        log.info("Internal API request: Fetching user by ID: {}", userId);
        return userService.getInternalUserById(userId)
                .map(userDto -> new ResponseEntity<>(userDto, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Retrieves internal user data by username.
     * Requires API Key authentication.
     */
    @GetMapping("/by-username")
    public ResponseEntity<InternalUserDto> getInternalUserByUsername(@RequestParam String username) {
        log.info("Internal API request: Fetching user by username: {}", username);
        return userService.getInternalUserByUsername(username)
                .map(userDto -> new ResponseEntity<>(userDto, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Retrieves internal user data by email.
     * Requires API Key authentication.
     */
    @GetMapping("/by-email")
    public ResponseEntity<InternalUserDto> getInternalUserByEmail(@RequestParam String email) {
        log.info("Internal API request: Fetching user by email: {}", email);
        return userService.getInternalUserByEmail(email)
                .map(userDto -> new ResponseEntity<>(userDto, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}