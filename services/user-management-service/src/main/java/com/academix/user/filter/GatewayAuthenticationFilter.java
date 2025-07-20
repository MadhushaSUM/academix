package com.academix.user.filter;

import com.academix.user.service.UserService;
import com.academix.user.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    private static final String GATEWAY_AUTH_HEADER = "X-Gateway-Auth";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USERNAME_HEADER = "X-User-Username";
    private static final String ROLES_HEADER = "X-User-Roles";
    private static final String GATEWAY_AUTH_VALUE = "validated";

    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // --- Security Check 1: Validate that request came through the trusted gateway ---
        String gatewayAuth = request.getHeader(GATEWAY_AUTH_HEADER);
        if (!GATEWAY_AUTH_VALUE.equals(gatewayAuth)) {
            log.warn("Direct access attempt to protected endpoint {} without proper gateway authentication header.", path);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403 Forbidden
            response.getWriter().write("{\"error\":\"Direct access forbidden. Requests must come through API Gateway.\"}");
            response.setContentType("application/json");
            return;
        }

        // --- Security Check 2: Extract and validate user information from headers ---
        String userIdStr = request.getHeader(USER_ID_HEADER);
        String username = request.getHeader(USERNAME_HEADER);
        String rolesStr = request.getHeader(ROLES_HEADER);

        if (!StringUtils.hasText(userIdStr) || !StringUtils.hasText(username) || !StringUtils.hasText(rolesStr)) {
            log.warn("Missing or empty required user headers (ID, Username, Roles) in request to {}. userId: {}, username: {}, roles: {}",
                    path, userIdStr, username, rolesStr);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
            response.getWriter().write("{\"error\":\"Invalid or missing authentication headers from gateway.\"}");
            response.setContentType("application/json");
            return;
        }

        try {
            Long userId = Long.parseLong(userIdStr);

            // --- Security Check 3: Verify user existence and roles against the database ---
            Optional<User> dbUserOptional = userService.findById(userId);

            if (dbUserOptional.isEmpty() || !dbUserOptional.get().getUsername().equals(username)) {
                log.warn("User with ID {} or username {} from gateway headers not found or mismatch in database. Rejecting request to {}.", userId, username, path);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
                response.getWriter().write("{\"error\":\"User not found or credentials mismatch.\"}");
                response.setContentType("application/json");
                return;
            }

            User user = dbUserOptional.get();

            // Parse roles and create authorities
            List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesStr.split(","))
                    .filter(StringUtils::hasText) // Filter out empty strings if any
                    .map(String::trim) // Trim whitespace
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            // Create authentication object with user details
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, authorities);

            // Set the authentication in Spring Security's context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("Successfully authenticated user {} (ID: {}) with roles: {}", username, userId, rolesStr);

        } catch (NumberFormatException e) {
            log.error("Invalid user ID format received from gateway: {} for path {}", userIdStr, path);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400 Bad Request
            response.getWriter().write("{\"error\":\"Invalid user ID format in headers.\"}");
            response.setContentType("application/json");
            return;
        } catch (Exception e) {
            log.error("Unexpected error during authentication processing for user {}: {}", username, e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500 Internal Server Error
            response.getWriter().write("{\"error\":\"Internal authentication error.\"}");
            response.setContentType("application/json");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Helper method to determine if a path should bypass authentication.
     * @param path The request URI path.
     * @return true if the path is public, false otherwise.
     */
    private boolean isPublicPath(String path) {
        return path.startsWith("/auth/") || path.startsWith("/actuator/");
    }
}
