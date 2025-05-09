package com.example.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private boolean enabled;
    private Set<RoleResponse> roles = new HashSet<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleResponse {
        private Long id;
        private String name;
        private Set<PermissionResponse> permissions = new HashSet<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionResponse {
        private Long id;
        private String name;
    }
}
