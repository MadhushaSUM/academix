package com.academix.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    @Enumerated(EnumType.STRING)
    private RoleName name;

    public enum RoleName {
        ROLE_USER,      // Generic user role (can be used for general authenticated users if needed)
        ROLE_ADMIN,     // Administrator role with full system access
        ROLE_STUDENT,   // Student role with access to courses, quizzes, etc.
        ROLE_INSTRUCTOR // Instructor/Teacher role with content creation/management capabilities
    }
}
