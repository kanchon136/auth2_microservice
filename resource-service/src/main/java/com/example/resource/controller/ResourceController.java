package com.example.resource.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ResourceController {

    @GetMapping("/public/info")
    public Map<String, Object> publicEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "This is a public endpoint that doesn't require authentication");
        response.put("service", "Resource Service");
        return response;
    }

    @GetMapping("/user/info")
    @PreAuthorize("hasRole('USER')")
    public Map<String, Object> userInfo(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        String username = jwt.getClaimAsString("sub");
        response.put("message", "Protected resource accessed successfully");
        response.put("username", username);
        response.put("service", "Resource Service");
        response.put("roles", jwt.getClaimAsStringList("roles"));
        return response;
    }

    @GetMapping("/admin/info")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> adminInfo(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        String username = jwt.getClaimAsString("sub");
        response.put("message", "Admin resource accessed successfully");
        response.put("username", username);
        response.put("service", "Resource Service");
        response.put("roles", jwt.getClaimAsStringList("roles"));
        return response;
    }

    //check for internal service to service communication
    @PreAuthorize("hasAuthority('SCOPE_internal:read')")
    @GetMapping("/internal/data")
    public String getInternalData() {
        return "This is secure service-to-service data";
    }
}
