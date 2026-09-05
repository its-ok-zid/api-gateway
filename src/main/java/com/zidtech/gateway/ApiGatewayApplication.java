package com.zidtech.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.reactive.ReactiveManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import reactor.core.publisher.Hooks;

@SpringBootApplication(
        scanBasePackages = "com.zidtech",
        exclude = {
                SecurityAutoConfiguration.class,
                ReactiveSecurityAutoConfiguration.class,
                ReactiveManagementWebSecurityAutoConfiguration.class
        }
)
@EnableDiscoveryClient
public class ApiGatewayApplication {
    public static void main(String[] args) {
        Hooks.enableAutomaticContextPropagation();
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}