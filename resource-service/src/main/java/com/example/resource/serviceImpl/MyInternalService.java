package com.example.resource.serviceImpl;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class MyInternalService {
    private final WebClient webClient;

    public MyInternalService(WebClient webClient) {
        this.webClient = webClient;
    }

    public String callOtherService() {
        return webClient.get()
                .uri("http://other-service-url/api/data")
                .retrieve()
                .bodyToMono(String.class)
                .block(); // সিঙ্ক্রোনাস কলের জন্য
    }
}
