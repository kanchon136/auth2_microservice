# Spring Boot OAuth2 Microservices Guide

This comprehensive guide explains how to work with the Spring Boot OAuth2 microservices architecture, focusing on token acquisition and role/permission-based access control.

## Table of Contents

1. [Project Overview](#project-overview)
2. [Authentication Flows](#authentication-flows)
   - [Authorization Code Flow](#authorization-code-flow)
   - [Client Credentials Flow](#client-credentials-flow)
   - [Adding Password Grant Flow](#adding-password-grant-flow)
3. [Role and Permission-Based Access Control](#role-and-permission-based-access-control)
   - [Current Implementation](#current-implementation)
   - [Enhancing Permission-Based Access](#enhancing-permission-based-access)
4. [Postman Instructions](#postman-instructions)
   - [Authorization Code Flow](#authorization-code-flow-in-postman)
   - [Client Credentials Flow](#client-credentials-flow-in-postman)
   - [Password Grant Flow](#password-grant-flow-in-postman)
5. [Project Modifications](#project-modifications)
   - [Adding Password Grant Support](#adding-password-grant-support)
   - [Improving JWT Token Claims](#improving-jwt-token-claims)
   - [Enhancing Permission-Based Access Control](#enhancing-permission-based-access-control)

## Project Overview

The project consists of the following microservices:

- **Auth Server**: OAuth2 authorization server that issues tokens
- **API Gateway**: Routes requests to appropriate services and handles authentication
- **Resource Service**: Protected API resources that require authentication
- **Common**: Shared models and utilities

The authentication and authorization are implemented using:

- Spring Security OAuth2
- JWT tokens
- Role and permission-based access control
- PostgreSQL database for user, role, and permission storage

## Authentication Flows

### Authorization Code Flow

The Authorization Code flow is designed for web applications where the client secret can be securely stored. It involves the following steps:

1. The client redirects the user to the authorization server
2. The user authenticates and grants permissions
3. The authorization server redirects back to the client with an authorization code
4. The client exchanges the code for an access token

This flow is currently configured for both the resource-server-1 and api-gateway clients.

### Client Credentials Flow

The Client Credentials flow is designed for service-to-service communication where no user is involved. It involves the following steps:

1. The client authenticates with its client ID and secret
2. The authorization server issues an access token

This flow is currently configured for both the resource-server-1 and api-gateway clients.

### Adding Password Grant Flow

The Password Grant flow allows clients to obtain tokens directly using username and password. This flow is not currently configured in the project but can be added (see [Project Modifications](#adding-password-grant-support)).

## Role and Permission-Based Access Control

### Current Implementation

The project implements role-based access control using:

1. **Database Model**:
   - User entity with many-to-many relationship to roles
   - Role entity with many-to-many relationship to permissions
   - Permission entity representing granular permissions

2. **JWT Token**:
   - Roles are added to the JWT token as claims
   - The resource server extracts roles from the token for authorization

3. **Method-Level Security**:
   - @PreAuthorize annotations with hasRole checks
   - URL-based security configuration

### Enhancing Permission-Based Access Control

The current implementation focuses on role-based access but can be enhanced to better utilize permissions:

1. **JWT Token Enhancement**:
   - Add permissions as separate claims in the JWT token
   - Properly distinguish between roles and permissions

2. **Method-Level Security Enhancement**:
   - Use hasAuthority checks for permission-based access
   - Combine role and permission checks for fine-grained control

## Postman Instructions

### Authorization Code Flow in Postman

1. **Step 1**: Obtain Authorization Code
   - Open a browser and navigate to:
   ```
   http://localhost:9000/oauth2/authorize?response_type=code&client_id=resource-server-1&scope=read%20write&redirect_uri=http://localhost:8081/login/oauth2/code/resource-server-1
   ```
   - Log in with username (e.g., "admin") and password ("password")
   - After successful login, you'll be redirected to a URL containing the authorization code

2. **Step 2**: Exchange Code for Token
   - In Postman, create a POST request to `http://localhost:9000/oauth2/token`
   - In the Authorization tab, select Basic Auth:
     - Username: resource-server-1
     - Password: resource-server-1-secret
   - In the Body tab (x-www-form-urlencoded):
     - grant_type: authorization_code
     - code: [your authorization code]
     - redirect_uri: http://localhost:8081/login/oauth2/code/resource-server-1
   - Send the request to receive the access token

### Client Credentials Flow in Postman

1. In Postman, create a POST request to `http://localhost:9000/oauth2/token`
2. In the Authorization tab, select Basic Auth:
   - Username: resource-server-1
   - Password: resource-server-1-secret
3. In the Body tab (x-www-form-urlencoded):
   - grant_type: client_credentials
   - scope: read write
4. Send the request to receive the access token

**Note**: The client credentials flow authenticates the client application itself, not a user. Therefore, the token will not contain user-specific roles or permissions.

### Password Grant Flow in Postman

*Note: This flow requires modifications to the project (see [Adding Password Grant Support](#adding-password-grant-support)).*

1. In Postman, create a POST request to `http://localhost:9000/oauth2/token`
2. In the Authorization tab, select Basic Auth:
   - Username: resource-server-1
   - Password: resource-server-1-secret
3. In the Body tab (x-www-form-urlencoded):
   - grant_type: password
   - username: admin
   - password: password
   - scope: read write
4. Send the request to receive the access token

## Project Modifications

### Adding Password Grant Support

To add Password Grant support, modify the `AuthServerConfig.java` file:

```java
// In the registeredClientRepository method
RegisteredClient resourceServer1Client = RegisteredClient.withId(UUID.randomUUID().toString())
        // ... existing configuration ...
        .authorizationGrantType(AuthorizationGrantType.PASSWORD) // Add this line
        // ... rest of the configuration ...
        .build();

RegisteredClient gatewayClient = RegisteredClient.withId(UUID.randomUUID().toString())
        // ... existing configuration ...
        .authorizationGrantType(AuthorizationGrantType.PASSWORD) // Add this line
        // ... rest of the configuration ...
        .build();
```

### Improving JWT Token Claims

To improve JWT token claims, modify the `jwtCustomizer` method in `AuthServerConfig.java`:

```java
@Bean
public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
    return context -> {
        if (context.getTokenType() == OAuth2TokenType.ACCESS_TOKEN) {
            Authentication principal = context.getPrincipal();
            Set<String> authorities = principal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());
            
            // Separate roles and permissions
            Set<String> roles = authorities.stream()
                    .filter(auth -> auth.startsWith("ROLE_"))
                    .map(auth -> auth.substring(5)) // Remove "ROLE_" prefix
                    .collect(Collectors.toSet());
            
            Set<String> permissions = authorities.stream()
                    .filter(auth -> !auth.startsWith("ROLE_"))
                    .collect(Collectors.toSet());
            
            context.getClaims().claim("roles", roles);
            context.getClaims().claim("permissions", permissions);
            context.getClaims().claim("authorities", authorities);
        }
    };
}
```

### Enhancing Permission-Based Access Control

To enhance permission-based access control, modify the `ResourceController.java` file to use permissions:

```java
@GetMapping("/write/data")
@PreAuthorize("hasAuthority('WRITE')")
public Map<String, Object> writeData(@AuthenticationPrincipal Jwt jwt) {
    Map<String, Object> response = new HashMap<>();
    response.put("message", "Write operation successful");
    response.put("username", jwt.getClaimAsString("sub"));
    response.put("permissions", jwt.getClaimAsStringList("permissions"));
    return response;
}

@GetMapping("/delete/data")
@PreAuthorize("hasAuthority('DELETE')")
public Map<String, Object> deleteData(@AuthenticationPrincipal Jwt jwt) {
    Map<String, Object> response = new HashMap<>();
    response.put("message", "Delete operation successful");
    response.put("username", jwt.getClaimAsString("sub"));
    response.put("permissions", jwt.getClaimAsStringList("permissions"));
    return response;
}

@GetMapping("/admin/management")
@PreAuthorize("hasAuthority('ADMIN')")
public Map<String, Object> adminManagement(@AuthenticationPrincipal Jwt jwt) {
    Map<String, Object> response = new HashMap<>();
    response.put("message", "Admin management operation successful");
    response.put("username", jwt.getClaimAsString("sub"));
    response.put("permissions", jwt.getClaimAsStringList("permissions"));
    return response;
}
```

Also, update the `jwtAuthenticationConverter` method in `ResourceServerConfig.java`:

```java
@Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
    grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities"); // Use the combined authorities claim
    grantedAuthoritiesConverter.setAuthorityPrefix(""); // No prefix needed as we're handling it in the token
    
    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
    return jwtAuthenticationConverter;
}
```
