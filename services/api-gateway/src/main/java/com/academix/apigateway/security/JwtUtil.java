package com.academix.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration.ms}")
    private Long jwtExpirationMs;

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Mono<String> extractUsername(String token) {
        return Mono.fromCallable(() -> extractClaim(token, Claims::getSubject));
    }

    public Mono<String> extractUserId(String token) {
        return Mono.fromCallable(() -> extractClaim(token, claims -> claims.get("userId", String.class)));
    }

    public Mono<Date> extractExpiration(String token) {
        return Mono.fromCallable(() -> extractClaim(token, Claims::getExpiration));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Mono<Boolean> isTokenExpired(String token) {
        return extractExpiration(token)
                .map(expiration -> expiration.before(new Date()));
    }

    public Mono<Boolean> validateToken(String token, String username) {
        return extractUsername(token)
                .flatMap(extractedUsername -> {
                    if (!extractedUsername.equals(username)) {
                        return Mono.just(false);
                    }
                    return isTokenExpired(token).map(expired -> !expired);
                });
    }

    public Mono<Boolean> validateToken(String token) {
        return Mono.fromCallable(() -> {
            try {
                Jwts.parserBuilder()
                        .setSigningKey(getSigningKey())
                        .build()
                        .parseClaimsJws(token);

                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }

    public Mono<Claims> extractAllClaimsReactive(String token) {
        return Mono.fromCallable(() -> extractAllClaims(token));
    }
}
