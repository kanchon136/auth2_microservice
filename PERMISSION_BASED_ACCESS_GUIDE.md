# Implementing Permission-Based Access Control

This guide provides detailed instructions on implementing and using permission-based access control in your Spring Boot OAuth2 microservices architecture.

## Table of Contents

1. [Understanding the Permission Model](#understanding-the-permission-model)
2. [Current Implementation](#current-implementation)
3. [Enhanced Implementation](#enhanced-implementation)
4. [Code Examples](#code-examples)
5. [Testing Permission-Based Access](#testing-permission-based-access)

## Understanding the Permission Model

In a role and permission-based access control system:

- **Permissions** represent fine-grained actions that can be performed (e.g., READ, WRITE, DELETE)
- **Roles** are collections of permissions (e.g., USER, ADMIN)
- **Users** are assigned roles, which grant them the associated permissions

This model allows for flexible and granular access control:

```
User -> Roles -> Permissions
```

For example:
- A user with the USER role might have only READ permission
- A user with the ADMIN role might have READ, WRITE, DELETE, and ADMIN permissions

## Current Implementation

The current implementation in your project includes:

### Database Model

```java
// User.java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String username;
    private String password;
    private String email;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
    
    // Getters and setters
}

// Role.java
@Entity
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();
    
    // Getters and setters
}

// Permission.java
@Entity
@Table(name = "permissions")
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    // Getters and setters
}
```

### JWT Token Customization

```java
@Bean
public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
    return context -> {
        if (context.getTokenType() == OAuth2TokenType.ACCESS_TOKEN) {
            Authentication principal = context.getPrincipal();
            Set<String> authorities = principal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());
            context.getClaims().claim("roles", authorities);
        }
    };
}
```

### Resource Server Configuration

```java
@Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
    grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
    grantedAuthoritiesConverter.setAuthorityPrefix("");

    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
    return jwtAuthenticationConverter;
}
```

### Controller Security

```java
@GetMapping("/user/info")
@PreAuthorize("hasRole('USER')")
public Map<String, Object> userInfo(@AuthenticationPrincipal Jwt jwt) {
    // Method implementation
}

@GetMapping("/admin/info")
@PreAuthorize("hasRole('ADMIN')")
public Map<String, Object> adminInfo(@AuthenticationPrincipal Jwt jwt) {
    // Method implementation
}
```

## Enhanced Implementation

To fully leverage permission-based access control, the implementation should be enhanced:

### 1. Improved JWT Token Customization

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

### 2. Updated Resource Server Configuration

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

### 3. Enhanced UserService

```java
@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        Set<GrantedAuthority> authorities = new HashSet<>();
        
        // Add roles with ROLE_ prefix
        for (Role role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            
            // Add permissions directly
            for (Permission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getName()));
            }
        }
        
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }
}
```

## Code Examples

### Permission-Based Controller Methods

```java
@RestController
@RequestMapping("/api")
public class ResourceController {

    // Role-based access control
    @GetMapping("/user/info")
    @PreAuthorize("hasRole('USER')")
    public Map<String, Object> userInfo(@AuthenticationPrincipal Jwt jwt) {
        // Method implementation
    }

    @GetMapping("/admin/info")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> adminInfo(@AuthenticationPrincipal Jwt jwt) {
        // Method implementation
    }

    // Permission-based access control
    @GetMapping("/data/read")
    @PreAuthorize("hasAuthority('READ')")
    public Map<String, Object> readData(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Read operation successful");
        response.put("username", jwt.getClaimAsString("sub"));
        response.put("permissions", jwt.getClaimAsStringList("permissions"));
        return response;
    }

    @PostMapping("/data/write")
    @PreAuthorize("hasAuthority('WRITE')")
    public Map<String, Object> writeData(@RequestBody Map<String, Object> data, @AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Write operation successful");
        response.put("data", data);
        response.put("username", jwt.getClaimAsString("sub"));
        response.put("permissions", jwt.getClaimAsStringList("permissions"));
        return response;
    }

    @DeleteMapping("/data/delete/{id}")
    @PreAuthorize("hasAuthority('DELETE')")
    public Map<String, Object> deleteData(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Delete operation successful");
        response.put("id", id);
        response.put("username", jwt.getClaimAsString("sub"));
        response.put("permissions", jwt.getClaimAsStringList("permissions"));
        return response;
    }

    // Combined role and permission-based access control
    @GetMapping("/admin/management")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('ADMIN')")
    public Map<String, Object> adminManagement(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Admin management operation successful");
        response.put("username", jwt.getClaimAsString("sub"));
        response.put("roles", jwt.getClaimAsStringList("roles"));
        response.put("permissions", jwt.getClaimAsStringList("permissions"));
        return response;
    }
}
```

### Complex Authorization Expressions

Spring Security's `@PreAuthorize` annotation supports complex expressions:

```java
// Require either ADMIN role or both USER role and WRITE permission
@PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and hasAuthority('WRITE'))")

// Require both ADMIN role and DELETE permission
@PreAuthorize("hasRole('ADMIN') and hasAuthority('DELETE')")

// Check if user has any of the specified permissions
@PreAuthorize("hasAnyAuthority('READ', 'WRITE', 'DELETE')")

// Check if user has any of the specified roles
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")

// Access control based on method parameters
@PreAuthorize("hasAuthority('ADMIN') or @userSecurity.isOwner(authentication, #userId)")
```

### Custom Security Evaluator

For more complex authorization logic, you can create custom security evaluators:

```java
@Component("userSecurity")
public class UserSecurityEvaluator {

    @Autowired
    private UserRepository userRepository;

    public boolean isOwner(Authentication authentication, Long userId) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        return user != null && user.getId().equals(userId);
    }
    
    public boolean hasPermissionForResource(Authentication authentication, String resourceType, Long resourceId) {
        // Custom logic to determine if user has permission for a specific resource
        return true; // Implement your custom logic here
    }
}
```

Then use it in your controllers:

```java
@GetMapping("/users/{userId}/profile")
@PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(authentication, #userId)")
public Map<String, Object> getUserProfile(@PathVariable Long userId) {
    // Method implementation
}
```

## Testing Permission-Based Access

### 1. Obtain a Token with User Credentials

Use the Authorization Code flow or Password Grant flow (if implemented) to obtain a token for a specific user.

### 2. Examine the Token Claims

Decode the JWT token (e.g., using jwt.io) to verify that it contains the correct roles and permissions.

### 3. Test Access to Protected Resources

Use Postman to test access to protected resources:

1. Set up a request to a protected endpoint
2. Add the Authorization header with the Bearer token
3. Send the request and verify the response

### 4. Test Different User Roles

Repeat the process with different user roles to verify that access control is working correctly:

- User with USER role should have access to endpoints requiring USER role or READ permission
- User with ADMIN role should have access to endpoints requiring ADMIN role or any permission
- Access should be denied for endpoints requiring permissions the user doesn't have

### 5. Verify Error Responses

When access is denied, verify that the server returns an appropriate error response (HTTP 403 Forbidden).
