package com.example.auth.config;

import com.example.auth.repository.PermissionRepository;
import com.example.auth.repository.RoleRepository;
import com.example.auth.repository.UserRepository;
import com.example.common.enums.RecordStatus;
import com.example.common.model.Permission;
import com.example.common.model.Role;
import com.example.common.model.User;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataInitializer(UserRepository userRepository, RoleRepository roleRepository,
                           PermissionRepository permissionRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initializing security data...");

        // ১. অ্যাকশন ভিত্তিক গ্লোবাল পারমিশন (Full Words)
        Permission readPermission = createPermissionIfNotFound("READ");
        Permission writePermission = createPermissionIfNotFound("WRITE");
        Permission updatePermission = createPermissionIfNotFound("UPDATE");
        Permission deletePermission = createPermissionIfNotFound("DELETE");

        // ২. মডিউল ভিত্তিক স্পেসিফিক পারমিশন (Full Words)
        Permission userManagement = createPermissionIfNotFound("USER_MANAGEMENT");
        Permission roleManagement = createPermissionIfNotFound("ROLE_MANAGEMENT");

        // --- রোল তৈরি ও পারমিশন এসাইন ---

        // USER: শুধু দেখার ক্ষমতা
        Role userRole = createRoleIfNotFound("USER",
                new HashSet<>(Set.of(readPermission)));

        // ADMIN: জেনারেল CRUD ক্ষমতা (ম্যানেজমেন্ট বাদে)
        Role adminRole = createRoleIfNotFound("ADMIN",
                new HashSet<>(Set.of(readPermission, writePermission, updatePermission, deletePermission)));

        // SUPERADMIN: সব কিছুর ফুল এক্সেস
        Role superAdminRole = createRoleIfNotFound("SUPERADMIN",
                new HashSet<>(Set.of(
                        readPermission, writePermission, updatePermission, deletePermission,
                        userManagement, roleManagement
                )));

        // --- ইউজার তৈরি ---
        if (!userRepository.existsByUsername("user")) {
            createUser("user", "user@example.com", "password", Set.of(userRole));
        }

        if (!userRepository.existsByUsername("admin")) {
            createUser("admin", "admin@example.com", "password", Set.of(adminRole));
        }

        if (!userRepository.existsByUsername("superadmin")) {
            createUser("superadmin", "superadmin@example.com", "password", Set.of(superAdminRole));
        }

        log.info("Security data initialization completed successfully.");
    }

    private void createUser(String username, String email, String password, Set<Role> roles) {
        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .roles(roles)
                .enabled(true)
                .createdDateTime(LocalDateTime.now())
                .recordStatus(RecordStatus.ACTIVE)
                .build();
        userRepository.save(user);
    }

    private Permission createPermissionIfNotFound(String name) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> {
                    Permission permission = Permission.builder()
                            .name(name)
                            .createdDateTime(LocalDateTime.now())
                            .recordStatus(RecordStatus.ACTIVE)
                            .build();
                    return permissionRepository.save(permission);
                });
    }

    private Role createRoleIfNotFound(String name, Set<Permission> permissions) {
        return roleRepository.findByName(name)
                .map(role -> {
                    role.setPermissions(permissions);
                    return roleRepository.save(role);
                })
                .orElseGet(() -> {
                    Role role = Role.builder()
                            .name(name)
                            .recordStatus(RecordStatus.ACTIVE)
                            .createdDateTime(LocalDateTime.now())
                            .permissions(permissions)
                            .build();
                    return roleRepository.save(role);
                });
    }
}
