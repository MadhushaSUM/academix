package com.academix.apigateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Skip authentication for public routes
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // Extract token and process
        String bearerToken = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            return validateAndProcessToken(token, exchange, chain);
        } else {
            // No valid token - return 401
            return unauthorizedResponse(exchange);
        }
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/api/v1/auth/") ||
                path.startsWith("/actuator/") ||
                path.startsWith("/eureka/");
    }

    private Mono<Void> validateAndProcessToken(String token, ServerWebExchange exchange, WebFilterChain chain) {
        return jwtUtil.validateToken(token)
                .flatMap(isValid -> {
                    if (!isValid) {
                        return unauthorizedResponse(exchange);
                    }
                    return processValidToken(token, exchange, chain);
                })
                .onErrorResume(error -> unauthorizedResponse(exchange));
    }

    private Mono<Void> processValidToken(String token, ServerWebExchange exchange, WebFilterChain chain) {
        return jwtUtil.extractAllClaimsReactive(token)
                .flatMap(claims -> {
                    String username = claims.getSubject();
                    String userId = claims.get("userId", String.class);
                    List<String> roles = claims.get("roles", List.class);

                    // Create authentication object
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(username, null, authorities);

                    // Add custom headers for downstream services
                    ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                            .header("X-User-Id", userId)
                            .header("X-User-Username", username)
                            .header("X-User-Roles", String.join(",", roles))
                            .header("X-Gateway-Auth", "validated")
                            .build();

                    ServerWebExchange modifiedExchange = exchange.mutate()
                            .request(modifiedRequest)
                            .build();

                    return chain.filter(modifiedExchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                });
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}