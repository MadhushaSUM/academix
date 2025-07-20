package com.academix.user.controller;

import com.academix.user.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/users")
public class UserController {


    @GetMapping("/me")
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN') or hasRole('ROLE_STUDENT')")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal User user) {
        log.info("Getting current user info for user: {} (ID: {})",
                user.getUsername(), user.getId());

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("firstName", user.getFirstName());
        userInfo.put("lastName", user.getLastName());
        userInfo.put("authorities", user.getAuthorities());
        userInfo.put("enabled", user.isEnabled());
        userInfo.put("createdAt", user.getCreatedAt());
        userInfo.put("message", "User authenticated successfully through API Gateway");

        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN') or hasRole('ROLE_STUDENT')")
    public ResponseEntity<Map<String, Object>> getUserProfile(@AuthenticationPrincipal User user) {
        log.info("Getting user profile for user: {} (ID: {})",
                user.getUsername(), user.getId());

        // In a real application, you might want to fetch fresh data from database
        // But since we have the User object, we can use it directly
        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", user.getId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("firstName", user.getFirstName());
        profile.put("lastName", user.getLastName());
        profile.put("roles", user.getRoles());
        profile.put("authorities", user.getAuthorities());
        profile.put("enabled", user.isEnabled());
        profile.put("createdAt", user.getCreatedAt());
        profile.put("updatedAt", user.getUpdatedAt());

        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN') or hasRole('ROLE_STUDENT')")
    public ResponseEntity<Map<String, Object>> updateUserProfile(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> updateRequest) {

        log.info("Updating profile for user: {} (ID: {})",
                user.getUsername(), user.getId());

        // In a real application, you would update user data in database
        // You would typically inject a UserService and call userService.updateUser(user.getId(), updateRequest)
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Profile updated successfully");
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("updatedFields", updateRequest.keySet());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllUsers(@AuthenticationPrincipal User user) {
        log.info("Admin {} requesting all users", user.getUsername());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "This endpoint is only accessible to administrators");
        response.put("requestedBy", user.getUsername());
        response.put("requesterId", user.getId());
        // In real app, you would inject UserService and return userService.getAllUsers()

        return ResponseEntity.ok(response);
    }
}