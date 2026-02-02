package com.example.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
 import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class AuthServerConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain authSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults());
        http.exceptionHandling(exception -> exception
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
                .oauth2ResourceServer(resourceServer
                       // -> resourceServer.jwt(Customizer.withDefaults()));
                        -> resourceServer.jwt(jtw-> jtw.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }


    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/oauth2/**").permitAll()
                        .requestMatchers("/roles/**", "/users/**").authenticated() // Admin API is authenticated
                        .anyRequest().authenticated())
                .formLogin(Customizer.withDefaults())
                // Stateless design might set session management to STATELESS, but formLogin
                // implies session
                // For AS, session management is needed for the login process.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/oauth2/**", "/roles/**", "/users/**"));
        return http.build();
    }



    @Bean
    public RegisteredClientRepository registeredClientRepository(
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository repository =
                new org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository(
                jdbcTemplate);

        String clientId = "client";
        if (repository.findByClientId(clientId) == null) {
            RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(clientId)
                    .clientSecret(passwordEncoder().encode("secret"))
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .redirectUri("https://oauthdebugger.com/debug")
                    .scope(OidcScopes.OPENID)
                    .clientSettings(clientSettings())
                    .build();
            repository.save(registeredClient);
        }

        String gatewayClientId = "api-gateway";
        if (repository.findByClientId(gatewayClientId) == null) {
            RegisteredClient gatewayClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(gatewayClientId)
                    .clientSecret(passwordEncoder.encode("gateway-secret"))
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .redirectUri("http://localhost:8090/login/oauth2/code/gateway")
                    .redirectUri("http://localhost:8090/authorized")
                    .scope(OidcScopes.OPENID)
                    .scope("read")
                    .scope("write")
                    .clientSettings(clientSettings())
                    .build();
            repository.save(gatewayClient);
        }

        return repository;
    }

    @Bean
    public org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService authorizationService(
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        return new org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService(jdbcTemplate,
                registeredClientRepository);
    }

    @Bean
    public org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService authorizationConsentService(
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        return new org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService(
                jdbcTemplate, registeredClientRepository);
    }

    @Bean
    public ClientSettings clientSettings() {
        return ClientSettings.builder().requireProofKey(false).build();
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().issuer("http://localhost:9000").build();
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return (context) -> {
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                Authentication principal = context.getPrincipal();
                Set<String> authorities = principal.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet());
                log.info("authorities=====>" + authorities);
                // Use 'authorities' as the standard claim name for Spring Resource Server
                context.getClaims().claim("authorities", authorities);
            }
        };
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = generateRSAKey();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

    private RSAKey generateRSAKey() {
        KeyPair keyPair = generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        return new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(UUID.randomUUID().toString()).build();
    }

    private KeyPair generateKeyPair() {
        KeyPair keyPair;
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            keyPair = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage());
        }
        return keyPair;
    }

    @Bean
    public org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter jwtAuthenticationConverter() {
        org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter authoritiesConverter =
                new org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter();

        // আমরা টোকেন কাস্টমাইজার-এ 'authorities' নাম দিয়েছিলাম, তাই এখানেও সেটিই বলে দিচ্ছি
        authoritiesConverter.setAuthoritiesClaimName("authorities");
        // আপনার টোকেনে যেহেতু অলরেডি ROLE_ADMIN আছে, তাই আলাদা করে প্রিফিক্স দরকার নেই
        authoritiesConverter.setAuthorityPrefix("");

        org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter converter =
                new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }


}
