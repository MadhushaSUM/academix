package com.academix.user.controller;

import com.academix.user.dto.ChangePasswordRequestDto;
import com.academix.user.dto.MessageResponseDto;
import com.academix.user.dto.UpdateProfileRequestDto;
import com.academix.user.dto.UserDto;
import com.academix.user.model.User;
import com.academix.user.service.AuthService;
import com.academix.user.service.UserService;
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
    private final UserService userService;

    /**
     * Allows an authenticated user to change their own password.
     *
     * @param userDetails The authenticated user's details (injected by Spring Security).
     * @param request The ChangePasswordRequestDto containing current and new passwords.
     * @return ResponseEntity with a success message.
     */
    @PatchMapping("/me/password")
    public ResponseEntity<MessageResponseDto> changeMyPassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequestDto request) {

        // Ensure userDetails is an instance of our User model
        if (!(userDetails instanceof User)) {
            log.error("Authenticated principal is not an instance of User entity. Type: {}", userDetails.getClass().getName());
            return new ResponseEntity<>(new MessageResponseDto("Authentication principal type mismatch."), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        User currentUser = (User) userDetails;
        log.info("Received password change request for user: {}", currentUser.getUsername());
        authService.changePassword(currentUser.getId(), request);
        return new ResponseEntity<>(new MessageResponseDto("Password changed successfully."), HttpStatus.OK);
    }

    /**
     * Retrieves the profile information of the currently authenticated user.
     *
     * @param userDetails The authenticated user's details.
     * @return ResponseEntity with UserDto.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        // Ensure userDetails is an instance of our User model
        if (!(userDetails instanceof User)) {
            log.error("Authenticated principal is not an instance of User entity. Type: {}", userDetails.getClass().getName());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        User currentUser = (User) userDetails;
        log.info("Fetching profile for authenticated user: {}", currentUser.getUsername());
        UserDto userProfile = userService.getUserProfile(currentUser.getId());
        return new ResponseEntity<>(userProfile, HttpStatus.OK);
    }

    /**
     * Allows the currently authenticated user to update their own profile.
     *
     * @param userDetails The authenticated user's details.
     * @param request The UpdateProfileRequestDto with updated fields.
     * @return ResponseEntity with the updated UserDto.
     */
    @PatchMapping("/me")
    public ResponseEntity<UserDto> updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequestDto request) {

        // Ensure userDetails is an instance of our User model
        if (!(userDetails instanceof User)) {
            log.error("Authenticated principal is not an instance of User entity. Type: {}", userDetails.getClass().getName());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        User currentUser = (User) userDetails;
        log.info("Received profile update request for user: {}", currentUser.getUsername());
        UserDto updatedUserProfile = userService.updateMyProfile(currentUser.getId(), request);
        return new ResponseEntity<>(updatedUserProfile, HttpStatus.OK);
    }
}