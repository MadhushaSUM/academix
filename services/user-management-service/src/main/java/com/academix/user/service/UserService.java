package com.academix.user.service;

import com.academix.user.dto.AdminUpdateUserRequestDto;
import com.academix.user.dto.InternalUserDto;
import com.academix.user.dto.UpdateProfileRequestDto;
import com.academix.user.dto.UserDto;
import com.academix.user.exception.UserAlreadyExistsException;
import com.academix.user.model.Role;
import com.academix.user.model.User;
import com.academix.user.repository.UserRepository;
import com.academix.user.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    @Transactional(readOnly = true)
    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    @Transactional(readOnly = true)
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    @Transactional(readOnly = true)
    public UserDto getUserProfile(Long userId) {
        log.info("Fetching profile for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return convertToDto(user);
    }

    @Transactional
    public UserDto updateMyProfile(Long userId, UpdateProfileRequestDto request) {
        log.info("Updating profile for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Update fields if provided in the request
        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName().trim());
        }

        // Handle email change with caution
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String newEmail = request.getEmail().trim();
            if (!user.getEmail().equalsIgnoreCase(newEmail)) {
                if (userRepository.existsByEmail(newEmail)) {
                    throw new UserAlreadyExistsException("Email '" + newEmail + "' is already registered by another user.");
                }
                user.setEmail(newEmail);
                //TODO: For prod, email verification here
            }
        }

        User updatedUser = userRepository.save(user);
        log.info("Profile updated successfully for user ID: {}", userId);
        return convertToDto(updatedUser);
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(Pageable pageable) {
        log.info("Fetching all users with page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findAll(pageable)
                .map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long userId) {
        log.info("Fetching user by ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return convertToDto(user);
    }

    @Transactional
    public UserDto updateUserByAdmin(Long userId, AdminUpdateUserRequestDto request) {
        log.info("Admin updating user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName().trim());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String newEmail = request.getEmail().trim();
            if (!user.getEmail().equalsIgnoreCase(newEmail)) {
                if (userRepository.existsByEmail(newEmail)) {
                    throw new UserAlreadyExistsException("Email '" + newEmail + "' is already registered by another user.");
                }
                user.setEmail(newEmail);
            }
        }

        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        // Update roles
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<Role> newRoles = new HashSet<>();
            for (String roleName : request.getRoles()) {
                // Ensure role names are in uppercase for consistency with Enum
                Role.RoleName enumRoleName = Role.RoleName.valueOf(roleName.toUpperCase());
                Role role = roleService.findByName(enumRoleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));
                newRoles.add(role);
            }
            user.setRoles(newRoles);
        } else {
            throw new IllegalArgumentException("User must have at least one role.");
        }

        User updatedUser = userRepository.save(user);
        log.info("User ID {} updated by admin.", userId);
        return convertToDto(updatedUser);
    }

    @Transactional
    public void deleteUser(Long userId) {
        log.info("Attempting to delete/deactivate user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setEnabled(false);
        // and revoke all refresh tokens associated with this user for immediate logout.
        authService.revokeAllRefreshTokensForUser(user);
        userRepository.save(user);
        log.info("User ID {} deactivated successfully (soft-deleted).", userId);
    }

    @Transactional(readOnly = true)
    public Optional<InternalUserDto> getInternalUserById(Long userId) {
        log.debug("Internal API: Fetching user by ID: {}", userId);
        return userRepository.findById(userId)
                .map(this::convertToInternalDto);
    }

    @Transactional(readOnly = true)
    public Optional<InternalUserDto> getInternalUserByUsername(String username) {
        log.debug("Internal API: Fetching user by username: {}", username);
        return userRepository.findByUsername(username)
                .map(this::convertToInternalDto);
    }

    @Transactional(readOnly = true)
    public Optional<InternalUserDto> getInternalUserByEmail(String email) {
        log.debug("Internal API: Fetching user by email: {}", email);
        return userRepository.findByEmail(email)
                .map(this::convertToInternalDto);
    }

    private UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private InternalUserDto convertToInternalDto(User user) {
        return InternalUserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name()) // Convert RoleName enum to String (e.g., "USER", "ADMIN")
                        .collect(Collectors.toSet()))
                .build();
    }
}