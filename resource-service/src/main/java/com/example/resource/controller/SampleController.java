package com.example.resource.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sample")
public class SampleController {

    @GetMapping("/public")
    public ResponseEntity<Map<String, Object>> publicEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "This is a public endpoint that doesn't require authentication");
        response.put("resource", "Sample Resource");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> userEndpoint(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "This is a USER protected endpoint");
        response.put("username", jwt.getClaimAsString("sub"));
        response.put("roles", jwt.getClaimAsStringList("roles"));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> adminEndpoint(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "This is an ADMIN protected endpoint");
        response.put("username", jwt.getClaimAsString("sub"));
        response.put("roles", jwt.getClaimAsStringList("roles"));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/superadmin")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<Map<String, Object>> superAdminEndpoint(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "This is a SUPERADMIN protected endpoint");
        response.put("username", jwt.getClaimAsString("sub"));
        response.put("roles", jwt.getClaimAsStringList("roles"));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/write")
    @PreAuthorize("hasAuthority('WRITE')")
    public ResponseEntity<Map<String, Object>> writeEndpoint(@RequestBody Map<String, Object> data, @AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Write operation successful");
        response.put("data", data);
        response.put("username", jwt.getClaimAsString("sub"));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('DELETE')")
    public ResponseEntity<Map<String, Object>> deleteEndpoint(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Delete operation successful");
        response.put("id", id);
        response.put("username", jwt.getClaimAsString("sub"));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user-management")
    @PreAuthorize("hasAuthority('USER_MANAGEMENT')")
    public ResponseEntity<Map<String, Object>> userManagementEndpoint(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User management operation successful");
        response.put("username", jwt.getClaimAsString("sub"));
        return ResponseEntity.ok(response);
    }
}
