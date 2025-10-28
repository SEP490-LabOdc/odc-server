package com.odc.apigateway.filter;

import com.odc.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final JwtUtil jwtUtil;

    @Autowired
    public AuthenticationFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (isSecured(request)) {
                if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    return onError(exchange, "Authorization header is missing", HttpStatus.UNAUTHORIZED);
                }

                String authHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    return onError(exchange, "Authorization header is invalid", HttpStatus.UNAUTHORIZED);
                }

                String token = authHeader.substring(7);
                try {
                    String username = jwtUtil.extractUsername(token);
                    if (!jwtUtil.validateToken(token, username)) {
                        return onError(exchange, "JWT token is invalid", HttpStatus.UNAUTHORIZED);
                    }

                    // Add user info to request headers
                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header("X-User-ID", username)
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());

                } catch (Exception e) {
                    log.error("JWT validation error: {}", e.getMessage());
                    return onError(exchange, "JWT token validation failed", HttpStatus.UNAUTHORIZED);
                }
            }

            return chain.filter(exchange);
        };
    }

    private boolean isSecured(ServerHttpRequest request) {
        List<String> publicEndpoints = List.of(
                "/api/v1/auth/login",
                "/api/v1/auth/register",
                "/api/v1/auth/refresh",
                "/actuator",
                "/v3/api-docs/merged",
                "/api/v1/companies/register",
                "/api/v1/companies/for-update",
                "/api/v1/otp",
                "/api/v1/files",
                "/ws/"
        );

        return publicEndpoints.stream()
                .noneMatch(endpoint -> request.getURI().getPath().startsWith(endpoint));
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().add("Content-Type", "application/json");

        String body = String.format("{\"error\":\"%s\",\"status\":%d}", err, httpStatus.value());
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    public static class Config {
        // Configuration properties if needed
    }
}
