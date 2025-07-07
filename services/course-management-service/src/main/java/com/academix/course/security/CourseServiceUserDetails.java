package com.academix.course.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseServiceUserDetails implements UserDetails {

    private Long id; // The user ID extracted from the JWT
    private String username; // The username (subject) extracted from the JWT
    private String password; // Not used for JWT authentication; kept to satisfy UserDetails interface
    private List<GrantedAuthority> authorities; // Roles extracted from the JWT
    private boolean enabled; // Always true if JWT is valid

    // Constructor to build from JWT claims in JwtAuthFilter
    public CourseServiceUserDetails(Long id, String username, List<GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = ""; // No password needed for JWT validation
        this.authorities = authorities;
        this.enabled = true; // If token is valid and not expired, user is considered enabled
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Token expiration is handled by JWT validation itself, not by account status
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Assuming valid token implies account is not locked for this service's purpose
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Handled by JWT token expiration
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}