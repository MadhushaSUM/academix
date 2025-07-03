package com.academix.user.config;

import com.academix.user.model.Role;
import com.academix.user.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes default roles in the database upon application startup.
 * This ensures that roles like USER, ADMIN, STUDENT, INSTRUCTOR are available
 * before users are created.
 */
@Component
@RequiredArgsConstructor
public class RoleInitializer implements CommandLineRunner {

    private final RoleService roleService;

    @Override
    public void run(String... args) throws Exception {
        createRoleIfNotFound(Role.RoleName.ROLE_USER);
        createRoleIfNotFound(Role.RoleName.ROLE_ADMIN);
        createRoleIfNotFound(Role.RoleName.ROLE_STUDENT);
        createRoleIfNotFound(Role.RoleName.ROLE_INSTRUCTOR);
    }

    private void createRoleIfNotFound(Role.RoleName roleName) {
        roleService.findByName(roleName).ifPresentOrElse(
                role -> System.out.println("Role " + roleName + " already exists."),
                () -> {
                    Role newRole = Role.builder().name(roleName).build();
                    roleService.save(newRole);
                    System.out.println("Created role: " + roleName);
                }
        );
    }
}