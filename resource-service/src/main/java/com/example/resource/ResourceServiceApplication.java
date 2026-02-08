package com.example.resource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EntityScan(basePackages = {"com.example.common.model"})
@EnableDiscoveryClient
public class ResourceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResourceServiceApplication.class, args);
        System.out.println("Resource service is running...!!");
    }
}
