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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

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

    // this configuration is custom login page
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/css/**", "/js/**", "/oauth2/**").permitAll() // Login page r static files permit kora
                        .requestMatchers("/roles/**", "/users/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")         // Amader custom login page path
                      //  .loginProcessingUrl("/login") // Form submit hobar path
                        .permitAll()
                )
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                        .logoutSuccessUrl("http://localhost:8090/")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers("/oauth2/**", "/roles/**", "/users/**", "/logout"));

        return http.build();
    }

// commended code are the spring security default loginPage

//
//    @Bean
//    @Order(2)
//    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .authorizeHttpRequests(authorize -> authorize
//                        .requestMatchers("/login", "/oauth2/**").permitAll()
//                        .requestMatchers("/roles/**", "/users/**").authenticated()
//                        .anyRequest().authenticated())
//                .formLogin(Customizer.withDefaults())
//                // --- এই লাইনটি যোগ করুন যাতে টোকেন দিয়ে রিকোয়েস্ট এক্সেপ্ট করে ---
//                .oauth2ResourceServer(resourceServer -> resourceServer
//                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
//                // --------------------------------------------------------
//                .logout(logout -> logout
//                        .logoutRequestMatcher(new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/logout", "GET"))
//                        .logoutSuccessUrl("http://localhost:8090/")
//                        .deleteCookies("JSESSIONID")
//                        .invalidateHttpSession(true)
//                        .clearAuthentication(true)
//                )
//                .csrf(csrf -> csrf.ignoringRequestMatchers("/oauth2/**", "/roles/**", "/users/**", "/logout"));
//        return http.build();
//    }


    @Bean
    public RegisteredClientRepository registeredClientRepository(
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository repository =
                new org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository(
                jdbcTemplate);
      // it is used for normal request pass without the gateway OR gateway config as a resource service
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

        // it is for service to service communication OR authentication
        String serviceClientId = "internal-service-client";
        if (repository.findByClientId(serviceClientId) == null) {
            RegisteredClient serviceClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(serviceClientId)
                    .clientSecret(passwordEncoder.encode("internal-secret")) // আপনার সিক্রেট
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS) // শুধুমাত্র ক্লায়েন্ট ক্রেডেনশিয়াল
                    .scope("internal:read")
                    .scope("internal:write")
                    .clientSettings(clientSettings())
                    .build();
            repository.save(serviceClient);
        }

       // it is for when request pass the gateway and gateway using as auth2 client(gateway maintain the full login makanisom)
        String gatewayClientId = "api-gateway";
        if (repository.findByClientId(gatewayClientId) == null) {
            RegisteredClient gatewayClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(gatewayClientId)
                    .clientSecret(passwordEncoder.encode("gateway-secret"))
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .redirectUri("http://localhost:8090/login/oauth2/code/api-gateway")
                    .postLogoutRedirectUri("http://localhost:8090/")
                    .scope(OidcScopes.OPENID)
                    .scope(OidcScopes.PROFILE) // প্রোফাইল স্কোপ যোগ করা ভালো
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
        return ClientSettings.builder().requireProofKey(false) // PKCE চাইলে true করতে পারেন
                .requireAuthorizationConsent(false) // ইউজারের কাছে প্রতিবার পারমিশন চাইবে না
                .build();
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().issuer("http://localhost:9000").build();
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

//    @Bean
//    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
//        return (context) -> {
//            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
//                Authentication principal = context.getPrincipal();
//                Set<String> authorities = principal.getAuthorities().stream()
//                        .map(GrantedAuthority::getAuthority)
//                        .collect(Collectors.toSet());
//                log.info("authorities=====>" + authorities);
//                // Use 'authorities' as the standard claim name for Spring Resource Server
//                context.getClaims().claim("authorities", authorities);
//            }
//        };
//    }


    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return (context) -> {
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                Authentication principal = context.getPrincipal();

                Set<String> authorities;

                // যদি এটি সার্ভিস-টু-সার্ভিস (Client Credentials) কল হয়
                if (context.getAuthorizationGrantType().equals(AuthorizationGrantType.CLIENT_CREDENTIALS)) {
                    // ক্লায়েন্টকে দেওয়া স্কোপগুলোকেই (internal:read ইত্যাদি) অথরিটি হিসেবে সেট করছি
                    authorities = context.getAuthorizedScopes().stream()
                            .map(scope -> "SCOPE_" + scope) // রিসোর্স সার্ভার সহজে চেনার জন্য SCOPE_ যোগ করা ভালো
                            .collect(Collectors.toSet());
                } else {
                    // এটি ইউজার লগইন (Authorization Code) এর জন্য
                    authorities = principal.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toSet());
                }

                log.info("Final authorities for token =====> " + authorities);

                // টোকেনে 'authorities' ক্লেইম সেট করা হচ্ছে
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


    /**
     * [সবশেষ বিন - JwtAuthenticationConverter]
     * এটি তখন কাজে লাগে যখন আপনার অথ সার্ভার নিজেও একটি "Resource Server" হিসেবে কাজ করে।
     * * কাজ: যখন আপনি টোকেন দিয়ে অথ সার্ভারের কোনো সিকিউর এপিআই (যেমন: /users বা /roles) কল করবেন,
     * তখন এটি টোকেন থেকে 'authorities' ক্লেইমটি খুঁজে বের করে এবং স্প্রিং সিকিউরিটির চেনার উপযোগী
     * Authorities-এ রূপান্তর করে। এর ফলে আপনি কন্ট্রোলারে @PreAuthorize ব্যবহার করতে পারেন।
     */
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
