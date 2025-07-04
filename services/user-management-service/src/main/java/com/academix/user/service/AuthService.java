package com.academix.user.service;

import com.academix.user.dto.*;
import com.academix.user.exception.InvalidCredentialsException;
import com.academix.user.exception.ResourceNotFoundException;
import com.academix.user.exception.UserAlreadyExistsException;
import com.academix.user.model.PasswordResetToken;
import com.academix.user.model.RefreshToken;
import com.academix.user.model.Role;
import com.academix.user.model.User;
import com.academix.user.repository.PasswordResetTokenRepository;
import com.academix.user.repository.RefreshTokenRepository;
import com.academix.user.repository.UserRepository;
import com.academix.user.service.email.EmailService;
import com.academix.user.util.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j // For logging
public class AuthService {

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    private final EmailService emailService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final HttpServletRequest request;

    private final long refreshTokenValidityInDays = 7;
    private final long passwordResetTokenValidityInMinutes = 60;

    @Transactional
    public AuthResponseDto registerUser(RegisterRequestDto registerRequest) {
        log.info("Attempting to register user: {}", registerRequest.getUsername());

        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new UserAlreadyExistsException("Username '" + registerRequest.getUsername() + "' is already taken.");
        }
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new UserAlreadyExistsException("Email '" + registerRequest.getEmail() + "' is already registered.");
        }

        Role userRole = roleService.findByName(Role.RoleName.ROLE_USER)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", Role.RoleName.ROLE_USER.name()));

        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .roles(Collections.singleton(userRole))
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getUsername());

        return authenticateAndGenerateTokens(registerRequest.getUsername(), registerRequest.getPassword(), savedUser);
    }

    @Transactional
    public AuthResponseDto loginUser(LoginRequestDto loginRequest) {
        log.info("Attempting to log in user: {}", loginRequest.getIdentifier());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getIdentifier(), loginRequest.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = (User) authentication.getPrincipal();

            log.info("User logged in successfully: {}", user.getUsername());

            return authenticateAndGenerateTokens(user.getUsername(), loginRequest.getPassword(), user);

        } catch (AuthenticationException e) {
            log.warn("Authentication failed for user {}: {}", loginRequest.getIdentifier(), e.getMessage());
            throw new InvalidCredentialsException("Invalid username/email or password.");
        }
    }

    @Transactional
    public AuthResponseDto refreshAccessToken(RefreshTokenRequestDto refreshRequest) {
        String refreshTokenString = refreshRequest.getRefreshToken();
        log.info("Attempting to refresh token with: {}", refreshTokenString.substring(0, Math.min(refreshTokenString.length(), 10)) + "...");

        // 1. Find refresh token entity
        RefreshToken existingRefreshToken = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token."));

        // 2. Check if it's expired or revoked
        if (existingRefreshToken.getExpiresAt().isBefore(LocalDateTime.now()) || existingRefreshToken.getRevokedAt() != null) {
            // Mark it as revoked if it wasn't already for expiry
            if(existingRefreshToken.getRevokedAt() == null) {
                existingRefreshToken.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(existingRefreshToken);
            }
            throw new InvalidCredentialsException("Refresh token has expired or been revoked. Please log in again.");
        }

        User user = existingRefreshToken.getUser();

        // 3. Generate new JWT access token
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities());
        String newAccessToken = jwtTokenProvider.generateToken(authentication);

        // 4. Invalidate the old refresh token
        existingRefreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(existingRefreshToken);

        // 5. Generate and save a new refresh token for rolling refresh token strategy
        String newRefreshTokenString = UUID.randomUUID().toString();
        LocalDateTime newRefreshTokenExpiryDate = LocalDateTime.now().plusDays(refreshTokenValidityInDays);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .token(newRefreshTokenString)
                .user(user)
                .expiresAt(newRefreshTokenExpiryDate)
                .userAgent(request.getHeader("User-Agent"))
                .ipAddress(request.getRemoteAddr())
                .build();
        refreshTokenRepository.save(newRefreshToken);

        log.info("Token refreshed successfully for user: {}", user.getUsername());

        return AuthResponseDto.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getJwtExpirationInMillis() / 1000)
                .refreshToken(newRefreshTokenString)
                .userId(user.getId().toString())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequestDto changePasswordRequest) {
        log.info("Attempting to change password for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Current password does not match.");
        }

        user.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
        userRepository.save(user);

        // This forces user to log in again from all devices
        refreshTokenRepository.deleteByUser(user);
        log.info("All refresh tokens invalidated for user ID {} after password change.", userId);

        log.info("Password changed successfully for user ID: {}", userId);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequestDto forgotPasswordRequest) {
        log.info("Received forgot password request for email: {}", forgotPasswordRequest.getEmail());

        User user = userRepository.findByEmail(forgotPasswordRequest.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", forgotPasswordRequest.getEmail()));

        // Invalidate any existing password reset tokens for this user to ensure only one is active
        passwordResetTokenRepository.deleteByUserId(user.getId());

        String resetTokenString = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(passwordResetTokenValidityInMinutes);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(resetTokenString)
                .user(user)
                .expiresAt(expiryDate)
                .build();
        passwordResetTokenRepository.save(resetToken);
        log.info("Generated password reset token for user: {}", user.getEmail());

        String resetLink = "http://your-frontend-domain/reset-password?token=" + resetTokenString;
        String emailBody = String.format("Dear %s,\n\nYou have requested to reset your password. " +
                "Please use the following link to reset your password:\n\n%s\n\n" +
                "This link will expire in %d minutes.\n\n" +
                "If you did not request a password reset, please ignore this email.\n\n" +
                "Regards,\nLMS Support Team", user.getFirstName() != null ? user.getFirstName() : user.getUsername(), resetLink, passwordResetTokenValidityInMinutes);

        emailService.sendEmail(user.getEmail(), "LMS Password Reset Request", emailBody);
        log.info("Password reset email sent to: {}", user.getEmail());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDto resetPasswordRequest) {
        log.info("Attempting to reset password with token: {}", resetPasswordRequest.getToken().substring(0, Math.min(resetPasswordRequest.getToken().length(), 10)) + "...");

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(resetPasswordRequest.getToken())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired password reset token."));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now()) || resetToken.getUsedAt() != null) {
            // Mark as used if it wasn't already for expiry
            if(resetToken.getUsedAt() == null) {
                resetToken.setUsedAt(LocalDateTime.now());
                passwordResetTokenRepository.save(resetToken);
            }
            throw new InvalidCredentialsException("Password reset token has expired or already been used. Please request a new one.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(resetPasswordRequest.getNewPassword()));
        userRepository.save(user);

        // Mark the token as used after successful password reset
        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);

        // Invalidate all refresh tokens for this user after password reset
        refreshTokenRepository.deleteByUser(user);
        log.info("All refresh tokens invalidated for user ID {} after password reset.", user.getId());

        log.info("Password reset successfully for user: {}", user.getUsername());
    }

    // Helper method to handle token generation and saving refresh token for both login/register
    private AuthResponseDto authenticateAndGenerateTokens(String identifier, String password, User user) {
        // Generate JWT access token
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(identifier, password));
        String accessToken = jwtTokenProvider.generateToken(authentication);

        // Generate and save refresh token entity
        String refreshTokenString = UUID.randomUUID().toString();
        LocalDateTime refreshTokenExpiryDate = LocalDateTime.now().plusDays(refreshTokenValidityInDays);

        // Clean up any revoked tokens for this user
        refreshTokenRepository.findByUserAndRevokedAtIsNull(user).forEach(rt -> {
            if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
                rt.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(rt);
            }
        });

        RefreshToken newRefreshToken = RefreshToken.builder()
                .token(refreshTokenString)
                .user(user)
                .expiresAt(refreshTokenExpiryDate)
                .userAgent(request.getHeader("User-Agent"))
                .ipAddress(request.getRemoteAddr())
                .build();
        refreshTokenRepository.save(newRefreshToken);

        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getJwtExpirationInMillis() / 1000)
                .refreshToken(refreshTokenString)
                .userId(user.getId().toString())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }
}