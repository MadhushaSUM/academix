package com.academix.user.controller;

import com.academix.user.dto.ChangePasswordRequestDto;
import com.academix.user.dto.MessageResponseDto;
import com.academix.user.model.User;
import com.academix.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for user-specific actions
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final AuthService authService;
    // private final UserService userService; // Will be used for /users/me later

    /**
     * Allows an authenticated user to change their own password.
     * Endpoint: PATCH /api/v1/users/me/password
     *
     * @param userDetails The authenticated user's details (injected by Spring Security).
     * @param request The ChangePasswordRequestDto containing current and new passwords.
     * @return ResponseEntity with a success message.
     */
    @PatchMapping("/me/password")
    public ResponseEntity<MessageResponseDto> changeMyPassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequestDto request) {

        if (!(userDetails instanceof User)) {
            log.error("Authenticated principal is not an instance of User entity. Type: {}", userDetails.getClass().getName());
            return new ResponseEntity<>(new MessageResponseDto("Authentication principal type mismatch."), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        User currentUser = (User) userDetails;
        log.info("Received password change request for user: {}", currentUser.getUsername());
        authService.changePassword(currentUser.getId(), request);
        return new ResponseEntity<>(new MessageResponseDto("Password changed successfully."), HttpStatus.OK);
    }

    // Other /users/me endpoints (GET, PATCH profile) will be added here
}