package com.academix.user.controller;

import com.academix.user.dto.*;
import com.academix.user.model.User;
import com.academix.user.service.AuthService;
import com.academix.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * Retrieves a paginated list of all users. Accessible only by ADMIN role.
     *
     * @param pageable Pagination and sorting information (e.g., ?page=0&size=10&sort=username,asc)
     * @return ResponseEntity with a Page of UserDto.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserDto>> getAllUsers(Pageable pageable) {
        log.info("Admin request to get all users. Page: {}, Size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<UserDto> usersPage = userService.getAllUsers(pageable);
        return new ResponseEntity<>(usersPage, HttpStatus.OK);
    }

    /**
     * Retrieves a specific user by ID. Accessible only by ADMIN role.
     *
     * @param userId The ID of the user to retrieve.
     * @return ResponseEntity with UserDto.
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long userId) {
        log.info("Admin request to get user by ID: {}", userId);
        UserDto user = userService.getUserById(userId);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    /**
     * Allows an ADMIN to update a specific user's details.
     *
     * @param userId The ID of the user to update.
     * @param request The AdminUpdateUserRequestDto with updated fields.
     * @return ResponseEntity with the updated UserDto.
     */
    @PatchMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateUserByAdmin(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUpdateUserRequestDto request) {
        log.info("Admin request to update user ID: {}", userId);
        UserDto updatedUser = userService.updateUserByAdmin(userId, request);
        return new ResponseEntity<>(updatedUser, HttpStatus.OK);
    }

    /**
     * Allows an ADMIN to delete (deactivate) a user.
     *
     * @param userId The ID of the user to delete/deactivate.
     * @return ResponseEntity with a success message.
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponseDto> deleteUser(@PathVariable Long userId) {
        log.info("Admin request to delete (deactivate) user ID: {}", userId);
        userService.deleteUser(userId);
        return new ResponseEntity<>(new MessageResponseDto("User deactivated successfully."), HttpStatus.OK);
    }
}