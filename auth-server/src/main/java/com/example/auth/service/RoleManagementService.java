package com.example.auth.service;

import com.example.auth.repository.PermissionRepository;
import com.example.auth.repository.RoleRepository;
import com.example.common.model.Permission;
import com.example.common.model.Role;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleManagementService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Autowired
    public RoleManagementService(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional
    public Role createRole(String name, Set<Long> permissionIds) {
        if (roleRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Role already exists with name: " + name);
        }
        
        Role role = new Role();
        role.setName(name);
        
        Set<Permission> permissions = new HashSet<>();
        if (permissionIds != null && !permissionIds.isEmpty()) {
            permissions = permissionIds.stream()
                    .map(permissionId -> permissionRepository.findById(permissionId)
                            .orElseThrow(() -> new EntityNotFoundException("Permission not found with id: " + permissionId)))
                    .collect(Collectors.toSet());
        }
        role.setPermissions(permissions);
        
        return roleRepository.save(role);
    }

    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Role getRoleById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + id));
    }

    @Transactional
    public Role assignPermissionsToRole(Long roleId, Set<Long> permissionIds) {
        Role role = getRoleById(roleId);
        
        Set<Permission> permissions = permissionIds.stream()
                .map(permissionId -> permissionRepository.findById(permissionId)
                        .orElseThrow(() -> new EntityNotFoundException("Permission not found with id: " + permissionId)))
                .collect(Collectors.toSet());
        
        role.getPermissions().addAll(permissions);
        return roleRepository.save(role);
    }

    @Transactional
    public Role removePermissionFromRole(Long roleId, Long permissionId) {
        Role role = getRoleById(roleId);
        
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new EntityNotFoundException("Permission not found with id: " + permissionId));
        
        role.getPermissions().remove(permission);
        return roleRepository.save(role);
    }

    @Transactional
    public void deleteRole(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new EntityNotFoundException("Role not found with id: " + id);
        }
        roleRepository.deleteById(id);
    }
}
