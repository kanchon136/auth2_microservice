package com.example.auth.controller;

import com.example.auth.dto.RoleAssignmentRequest;
import com.example.auth.dto.UserCreateRequest;
import com.example.auth.dto.UserResponse;
import com.example.auth.service.UserManagementService;
import com.example.common.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserManagementService userManagementService;

    @Autowired
    public UserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<UserResponse> createUser(@RequestBody UserCreateRequest request) {
        User user = userManagementService.createUser(
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                request.getRoles()
        );
        return new ResponseEntity<>(mapToUserResponse(user), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<User> users = userManagementService.getAllUsers();
        List<UserResponse> userResponses = users.stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userResponses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN') or authentication.principal.username == #username")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userManagementService.getUserById(id);
        return ResponseEntity.ok(mapToUserResponse(user));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<UserResponse> assignRolesToUser(
            @PathVariable Long id,
            @RequestBody RoleAssignmentRequest request) {
        User user = userManagementService.assignRolesToUser(id, request.getRoleIds());
        return ResponseEntity.ok(mapToUserResponse(user));
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<UserResponse> removeRoleFromUser(
            @PathVariable Long id,
            @PathVariable Long roleId) {
        User user = userManagementService.removeRoleFromUser(id, roleId);
        return ResponseEntity.ok(mapToUserResponse(user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userManagementService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    private UserResponse mapToUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isEnabled(),
                user.getRoles().stream()
                        .map(role -> new UserResponse.RoleResponse(
                                role.getId(),
                                role.getName(),
                                role.getPermissions().stream()
                                        .map(permission -> new UserResponse.PermissionResponse(
                                                permission.getId(),
                                                permission.getName()
                                        ))
                                        .collect(Collectors.toSet())
                        ))
                        .collect(Collectors.toSet())
        );
    }
}
