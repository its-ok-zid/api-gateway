package com.zidtech.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RouteValidatorTest {

    private final RouteValidator routeValidator = new RouteValidator();

    @Test
    @DisplayName("Open endpoints should not be secured")
    void openEndpointsAreNotSecured() {
        MockServerHttpRequest req1 = MockServerHttpRequest.get("/api/v1/auth/login").build();
        MockServerHttpRequest req2 = MockServerHttpRequest.get("/api/v1/auth/register").build();
        MockServerHttpRequest req3 = MockServerHttpRequest.get("/actuator/health").build();
        MockServerHttpRequest req4 = MockServerHttpRequest.get("/eureka/web").build();

        assertThat(routeValidator.isSecured.test(req1)).isFalse();
        assertThat(routeValidator.isSecured.test(req2)).isFalse();
        assertThat(routeValidator.isSecured.test(req3)).isFalse();
        assertThat(routeValidator.isSecured.test(req4)).isFalse();
    }

    @Test
    @DisplayName("Domain endpoints should be secured")
    void domainEndpointsAreSecured() {
        MockServerHttpRequest req1 = MockServerHttpRequest.get("/api/v1/orders/1").build();
        MockServerHttpRequest req2 = MockServerHttpRequest.get("/api/v1/products").build();

        assertThat(routeValidator.isSecured.test(req1)).isTrue();
        assertThat(routeValidator.isSecured.test(req2)).isTrue();
    }
}