package com.odc.apigateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odc.common.constant.ApiConstants;
import com.odc.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class CustomServerAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatusCode.valueOf(401)); // 401
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApiResponse<Object> body = ApiResponse.error(
                "Bạn chưa đăng nhập. Vui lòng đăng nhập để tiếp tục.",
                ApiConstants.AUTHENTICATION_ERROR);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            byte[] bytes = ("{\"success\":false,\"message\":\"You are not authenticated. Please login.\",\"errorCode\":\""
                    + ApiConstants.AUTHENTICATION_ERROR + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        }
    }
}
