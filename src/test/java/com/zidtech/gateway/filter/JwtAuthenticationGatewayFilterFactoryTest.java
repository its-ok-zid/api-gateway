package com.zidtech.gateway.filter;

import com.zidtech.gateway.config.RouteValidator;
import com.zidtech.security.JwtTokenService;
import com.zidtech.security.ParsedToken;
import com.zidtech.security.TokenType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationGatewayFilterFactoryTest {

    // Using real RouteValidator to test exact routing paths
    private final RouteValidator routeValidator = new RouteValidator();

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private GatewayFilterChain chain;

    private JwtAuthenticationGatewayFilterFactory filterFactory;
    private GatewayFilter filter;

    @BeforeEach
    void setUp() {
        filterFactory = new JwtAuthenticationGatewayFilterFactory(routeValidator, jwtTokenService);
        filter = filterFactory.apply(new JwtAuthenticationGatewayFilterFactory.Config());
        lenient().when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("Should bypass token check for unsecured open endpoints")
    void bypassOpenEndpoints() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain, times(1)).filter(exchange);
        verifyNoInteractions(jwtTokenService);
    }

    @Test
    @DisplayName("Should reject request if Authorization header is missing on secured route")
    void rejectMissingAuthorizationHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/orders/123").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(jwtTokenService);
    }

    @Test
    @DisplayName("Should reject request if Authorization header is incorrectly formatted")
    void rejectInvalidAuthorizationFormat() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/orders/123")
                .header(HttpHeaders.AUTHORIZATION, "Basic token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(jwtTokenService);
    }

    @Test
    @DisplayName("Should reject request if token is invalid or expired")
    void rejectInvalidToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/orders/123")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtTokenService.parse("invalid-token", TokenType.ACCESS))
                .thenThrow(new RuntimeException("Expired token"));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should mutate request and inject headers if token is valid")
    void acceptAndEnrichValidToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/orders/123")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ParsedToken parsedToken = new ParsedToken(
                "user-123", "zidan", Set.of("CUSTOMER", "SELLER"), TokenType.ACCESS, UUID.randomUUID(), null, Instant.now()
        );

        when(jwtTokenService.parse("valid-token", TokenType.ACCESS)).thenReturn(parsedToken);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain, times(1)).filter(captor.capture());

        ServerWebExchange mutatedExchange = captor.getValue();
        HttpHeaders headers = mutatedExchange.getRequest().getHeaders();

        assertThat(headers.getFirst("X-User-Id")).isEqualTo("user-123");
        assertThat(headers.getFirst("X-User-Role")).contains("CUSTOMER", "SELLER");
    }
}