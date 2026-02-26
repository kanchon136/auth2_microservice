package com.example.resource.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * OAuth2AuthorizedClientManager: এটি অটোমেটিক ওথ সার্ভার থেকে টোকেন আনা,
     * ক্যাশ করা এবং মেয়াদ শেষ হলে রিনিউ করার কাজ করে।
     */
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {

        // আমরা এখানে শুধু 'client_credentials' প্রোভাইডার ব্যবহার করছি
        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials()
                        .build();

        DefaultOAuth2AuthorizedClientManager authorizedClientManager =
                new DefaultOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientRepository);

        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

    /**
     * WebClient: এটি অন্য সার্ভিসকে কল করার জন্য ব্যবহার করা হবে।
     * এখানে আমরা একটি ফিল্টার যোগ করেছি যা প্রতি রিকোয়েস্টের সাথে 'Bearer Token' পাঠিয়ে দিবে।
     */
    @Bean
    public WebClient webClient(OAuth2AuthorizedClientManager authorizedClientManager) {
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2ClientFilter =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);

        // YAML ফাইলে আপনি যে নাম দিবেন (যেমন: internal-service) সেটি এখানে ডিফল্ট করে দিন
        oauth2ClientFilter.setDefaultClientRegistrationId("internal-service");

        return WebClient.builder()
                .apply(oauth2ClientFilter.oauth2Configuration())
                .build();
    }
}
