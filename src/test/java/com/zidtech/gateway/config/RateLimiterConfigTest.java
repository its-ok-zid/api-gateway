package com.zidtech.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

class RateLimiterConfigTest {

    private final RateLimiterConfig config = new RateLimiterConfig();

    @Test
    @DisplayName("Should resolve IP address from remote address")
    void resolveIpAddress() {
        KeyResolver keyResolver = config.ipKeyResolver();
        
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/orders")
                .remoteAddress(new InetSocketAddress("192.168.1.100", 8080))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("192.168.1.100")
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle missing remote address gracefully")
    void resolveEmptyWhenNoRemoteAddress() {
        KeyResolver keyResolver = config.ipKeyResolver();
        
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/orders").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(keyResolver.resolve(exchange))
                .verifyComplete();
    }
}