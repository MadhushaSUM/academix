package com.academix.user.filter;

import com.academix.user.model.User;
import com.academix.user.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
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

    @Autowired(required = false)
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip filter for public routes
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Validate that request came through the gateway
        String gatewayAuth = request.getHeader(GATEWAY_AUTH_HEADER);
        if (!GATEWAY_AUTH_VALUE.equals(gatewayAuth)) {
            log.warn("Request to protected endpoint {} without proper gateway authentication", path);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\":\"Direct access forbidden. Requests must come through API Gateway.\"}");
            response.setContentType("application/json");
            return;
        }

        // Extract user information from headers set by gateway
        String userId = request.getHeader(USER_ID_HEADER);
        String username = request.getHeader(USERNAME_HEADER);
        String rolesStr = request.getHeader(ROLES_HEADER);

        if (!StringUtils.hasText(userId) || !StringUtils.hasText(username)) {
            log.warn("Missing required user headers in request to {}", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Invalid authentication headers\"}");
            response.setContentType("application/json");
            return;
        }

        try {
            // Create User object - try to fetch from database first, fallback to gateway info
            User user = getUserFromDatabaseOrCreateFromHeaders(Long.parseLong(userId), username, rolesStr);

            // Parse roles and create authorities
            List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesStr.split(","))
                    .filter(StringUtils::hasText)
                    .map(role -> new SimpleGrantedAuthority(role.trim()))
                    .collect(Collectors.toList());

            // Create authentication object with user details
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("Successfully authenticated user {} (ID: {}) with roles: {}", username, userId, rolesStr);

        } catch (NumberFormatException e) {
            log.error("Invalid user ID format: {}", userId);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Invalid user ID format\"}");
            response.setContentType("application/json");
            return;
        } catch (Exception e) {
            log.error("Error processing authentication for user {}: {}", username, e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Authentication processing error\"}");
            response.setContentType("application/json");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private User getUserFromDatabaseOrCreateFromHeaders(Long userId, String username, String rolesStr) {
        // If UserService is available, try to fetch user from database
        if (userService != null) {
            try {
                Optional<User> dbUser = userService.findById(userId);
                if (dbUser.isPresent()) {
                    log.debug("Found user {} in database", username);
                    return dbUser.get();
                }
            } catch (Exception e) {
                log.warn("Failed to fetch user {} from database, creating from headers: {}", username, e.getMessage());
            }
        }

        // Fallback: create User object from gateway headers
        log.debug("Creating user object from gateway headers for user: {}", username);
        return User.builder()
                .id(userId)
                .username(username)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .roles(new HashSet<>())
                .createdAt(LocalDateTime.now()) // Placeholder
                .updatedAt(LocalDateTime.now()) // Placeholder
                .build();
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/auth/") ||
                path.startsWith("/actuator/");
    }
}
