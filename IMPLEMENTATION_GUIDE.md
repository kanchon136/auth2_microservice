# Spring Boot OAuth2 Implementation Guide

This guide provides specific implementation examples for enhancing your Spring Boot OAuth2 microservices architecture with improved token handling and permission-based access control.

## Table of Contents

1. [Adding Password Grant Support](#adding-password-grant-support)
2. [Improving JWT Token Claims](#improving-jwt-token-claims)
3. [Enhancing Permission-Based Access Control](#enhancing-permission-based-access-control)
4. [Complete Implementation Examples](#complete-implementation-examples)

## Adding Password Grant Support

The Password Grant flow allows clients to obtain tokens directly using username and password. This is useful for native applications where the authorization code flow is not practical.

### Step 1: Update AuthServerConfig.java

```java
@Bean
public RegisteredClientRepository registeredClientRepository() {
    RegisteredClient resourceServer1Client = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("resource-server-1")
            .clientSecret(passwordEncoder().encode("resource-server-1-secret"))
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .authorizationGrantType(AuthorizationGrantType.PASSWORD) // Add this line
            .redirectUri("http://localhost:8081/login/oauth2/code/resource-server-1")
            .redirectUri("http://localhost:8081/authorized")
            .scope(OidcScopes.OPENID)
            .scope("read")
            .scope("write")
            .tokenSettings(TokenSettings.builder()
                    .accessTokenTimeToLive(Duration.ofHours(1))
                    .refreshTokenTimeToLive(Duration.ofDays(30))
                    .build())
            .clientSettings(ClientSettings.builder()
                    .requireAuthorizationConsent(true)
                    .build())
            .build();

    RegisteredClient gatewayClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("api-gateway")
            .clientSecret(passwordEncoder().encode("gateway-secret"))
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .authorizationGrantType(AuthorizationGrantType.PASSWORD) // Add this line
            .redirectUri("http://localhost:8090/login/oauth2/code/gateway")
            .redirectUri("http://localhost:8090/authorized")
            .scope(OidcScopes.OPENID)
            .scope("read")
            .scope("write")
            .tokenSettings(TokenSettings.builder()
                    .accessTokenTimeToLive(Duration.ofHours(1))
                    .refreshTokenTimeToLive(Duration.ofDays(30))
                    .build())
            .clientSettings(ClientSettings.builder()
                    .requireAuthorizationConsent(true)
                    .build())
            .build();

    return new InMemoryRegisteredClientRepository(resourceServer1Client, gatewayClient);
}
```

### Step 2: Ensure UserService implements UserDetailsService

Make sure your UserService properly implements UserDetailsService to support password grant authentication:

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

## Improving JWT Token Claims

The current implementation adds roles to the JWT token but doesn't properly separate roles from permissions. Here's how to improve it:

### Update jwtCustomizer method in AuthServerConfig.java

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

## Enhancing Permission-Based Access Control

To fully leverage permission-based access control, update the resource server configuration and controllers:

### Step 1: Update JwtAuthenticationConverter in ResourceServerConfig.java

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

### Step 2: Create Permission-Based Controller Methods

Add permission-based endpoints to your ResourceController:

```java
@RestController
@RequestMapping("/api")
public class ResourceController {

    // Existing role-based endpoints...

    // Permission-based endpoints
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

## Complete Implementation Examples

### Complete AuthServerConfig.java

```java
package com.example.auth.config;

import com.example.auth.service.UserService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class AuthServerConfig {

    @Autowired
    private UserService userService;

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults());
        
        http.exceptionHandling(exceptions -> 
                exceptions.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")));
        
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> 
                authorize
                    .requestMatchers("/users/**").hasRole("SUPERADMIN")
                    .requestMatchers("/roles/**").hasRole("SUPERADMIN")
                    .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults())
            .csrf(csrf -> csrf.ignoringRequestMatchers("/users/**", "/roles/**"));
        
        return http.build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient resourceServer1Client = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("resource-server-1")
                .clientSecret(passwordEncoder().encode("resource-server-1-secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(AuthorizationGrantType.PASSWORD) // Added password grant
                .redirectUri("http://localhost:8081/login/oauth2/code/resource-server-1")
                .redirectUri("http://localhost:8081/authorized")
                .scope(OidcScopes.OPENID)
                .scope("read")
                .scope("write")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofDays(30))
                        .build())
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(true)
                        .build())
                .build();

        RegisteredClient gatewayClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("api-gateway")
                .clientSecret(passwordEncoder().encode("gateway-secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(AuthorizationGrantType.PASSWORD) // Added password grant
                .redirectUri("http://localhost:8090/login/oauth2/code/gateway")
                .redirectUri("http://localhost:8090/authorized")
                .scope(OidcScopes.OPENID)
                .scope("read")
                .scope("write")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofDays(30))
                        .build())
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(true)
                        .build())
                .build();

        return new InMemoryRegisteredClientRepository(resourceServer1Client, gatewayClient);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:9000")
                .build();
    }

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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### Complete ResourceServerConfig.java

```java
package com.example.resource.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ResourceServerConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/user/**").hasRole("USER")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );
        
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities"); // Use the combined authorities claim
        grantedAuthoritiesConverter.setAuthorityPrefix(""); // No prefix needed as we're handling it in the token
        
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }
}
```

### Complete ResourceController.java with Permission-Based Access

```java
package com.example.resource.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

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

    // Permission-based endpoints
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

These implementation examples provide a complete solution for enhancing your Spring Boot OAuth2 microservices architecture with improved token handling and permission-based access control.
