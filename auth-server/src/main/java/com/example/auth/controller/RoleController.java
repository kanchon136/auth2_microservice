package com.example.auth.controller;

import com.example.auth.dto.PermissionAssignmentRequest;
import com.example.auth.dto.RoleCreateRequest;
import com.example.auth.dto.RoleResponse;
import com.example.auth.service.RoleManagementService;
import com.example.common.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/roles")
public class RoleController {

    private final RoleManagementService roleManagementService;

    @Autowired
    public RoleController(RoleManagementService roleManagementService) {
        this.roleManagementService = roleManagementService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<RoleResponse> createRole(@RequestBody RoleCreateRequest request) {
        Role role = roleManagementService.createRole(
                request.getName(),
                request.getPermissions()
        );
        return new ResponseEntity<>(mapToRoleResponse(role), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        List<Role> roles = roleManagementService.getAllRoles();
        List<RoleResponse> roleResponses = roles.stream()
                .map(this::mapToRoleResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(roleResponses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable Long id) {
        Role role = roleManagementService.getRoleById(id);
        return ResponseEntity.ok(mapToRoleResponse(role));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<RoleResponse> assignPermissionsToRole(
            @PathVariable Long id,
            @RequestBody PermissionAssignmentRequest request) {
        Role role = roleManagementService.assignPermissionsToRole(id, request.getPermissionIds());
        return ResponseEntity.ok(mapToRoleResponse(role));
    }

    @DeleteMapping("/{id}/permissions/{permissionId}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<RoleResponse> removePermissionFromRole(
            @PathVariable Long id,
            @PathVariable Long permissionId) {
        Role role = roleManagementService.removePermissionFromRole(id, permissionId);
        return ResponseEntity.ok(mapToRoleResponse(role));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleManagementService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    private RoleResponse mapToRoleResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getPermissions().stream()
                        .map(permission -> new RoleResponse.PermissionResponse(
                                permission.getId(),
                                permission.getName()
                        ))
                        .collect(Collectors.toSet())
        );
    }
}
