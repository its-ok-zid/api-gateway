package com.zidtech.gateway.filter;

import com.zidtech.gateway.config.RouteValidator;
import com.zidtech.security.JwtTokenService;
import com.zidtech.security.ParsedToken;
import com.zidtech.security.TokenType;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtAuthenticationGatewayFilterFactory.Config> {

    private final RouteValidator routeValidator;
    private final JwtTokenService jwtTokenService;

    public JwtAuthenticationGatewayFilterFactory(RouteValidator routeValidator, JwtTokenService jwtTokenService) {
        super(Config.class);
        this.routeValidator = routeValidator;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // 1. Check if route requires JWT verification
            if (routeValidator.isSecured.test(request)) {

                // 2. Validate Authorization header presence
                if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    return writeErrorResponse(exchange, "Missing Authorization Header", HttpStatus.UNAUTHORIZED);
                }

                String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    return writeErrorResponse(exchange, "Invalid Authorization Header format", HttpStatus.UNAUTHORIZED);
                }

                String token = authHeader.substring(7);

                try {
                    // 3. Parse and cryptographically verify the Access Token
                    ParsedToken parsedToken = jwtTokenService.parse(token, TokenType.ACCESS);

                    // 4. Extract principal data from ParsedToken record
                    String userId = parsedToken.subject();
                    String roles = String.join(",", parsedToken.roles());

                    // 5. Enrich request with trusted internal downstream headers
                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header("X-User-Id", userId)
                            .header("X-User-Role", roles)
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());

                } catch (Exception ex) {
                    return writeErrorResponse(exchange, "Access token is invalid or expired", HttpStatus.UNAUTHORIZED);
                }
            }

            // Public endpoint -> proceed without token inspection
            return chain.filter(exchange);
        };
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String responseBody = "{\"success\":false,\"code\":\"" + status.name() + "\",\"message\":\"" + message + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
        // Configuration hook for Spring Cloud Gateway Factory
    }
}