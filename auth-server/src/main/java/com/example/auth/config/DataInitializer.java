package com.example.auth.config;

import com.example.auth.repository.PermissionRepository;
import com.example.auth.repository.RoleRepository;
import com.example.auth.repository.UserRepository;
import com.example.common.model.Permission;
import com.example.common.model.Role;
import com.example.common.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataInitializer(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Permission readPermission = createPermissionIfNotFound("READ");
        Permission writePermission = createPermissionIfNotFound("WRITE");
        Permission deletePermission = createPermissionIfNotFound("DELETE");
        Permission adminPermission = createPermissionIfNotFound("ADMIN");
        Permission superAdminPermission = createPermissionIfNotFound("SUPERADMIN");
        Permission userManagementPermission = createPermissionIfNotFound("USER_MANAGEMENT");
        Permission roleManagementPermission = createPermissionIfNotFound("ROLE_MANAGEMENT");

        Set<Permission> userPermissions = new HashSet<>(Arrays.asList(readPermission));
        Role userRole = createRoleIfNotFound("USER", userPermissions);

        Set<Permission> adminPermissions = new HashSet<>(Arrays.asList(
                readPermission, writePermission, deletePermission, adminPermission));
        Role adminRole = createRoleIfNotFound("ADMIN", adminPermissions);

        Set<Permission> superAdminPermissions = new HashSet<>(Arrays.asList(
                readPermission, writePermission, deletePermission, adminPermission, 
                superAdminPermission, userManagementPermission, roleManagementPermission));
        Role superAdminRole = createRoleIfNotFound("SUPERADMIN", superAdminPermissions);

        if (!userRepository.existsByUsername("user")) {
            User user = new User();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("password"));
            user.setEmail("user@example.com");
            user.setRoles(new HashSet<>(Arrays.asList(userRole)));
            userRepository.save(user);
        }

        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("password"));
            admin.setEmail("admin@example.com");
            admin.setRoles(new HashSet<>(Arrays.asList(adminRole)));
            userRepository.save(admin);
        }

        if (!userRepository.existsByUsername("superadmin")) {
            User superAdmin = new User();
            superAdmin.setUsername("superadmin");
            superAdmin.setPassword(passwordEncoder.encode("password"));
            superAdmin.setEmail("superadmin@example.com");
            superAdmin.setRoles(new HashSet<>(Arrays.asList(superAdminRole)));
            userRepository.save(superAdmin);
        }
    }

    private Permission createPermissionIfNotFound(String name) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> {
                    Permission permission = new Permission();
                    permission.setName(name);
                    return permissionRepository.save(permission);
                });
    }

    private Role createRoleIfNotFound(String name, Set<Permission> permissions) {
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(name);
                    role.setPermissions(permissions);
                    return roleRepository.save(role);
                });
    }
}
