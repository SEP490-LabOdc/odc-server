package com.odc.apigateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odc.common.constant.ApiConstants;
import com.odc.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CustomServerAccessDeniedHandler implements ServerAccessDeniedHandler {
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        ServerHttpResponse response = exchange.getResponse();

        response.setStatusCode(HttpStatusCode.valueOf(403)); // 403
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApiResponse<Object> body = ApiResponse.error(
                "Bạn không được phép truy cập vào nội dung này.",
                ApiConstants.AUTHORIZATION_ERROR
        );
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            byte[] bytes = ("{\"success\":false,\"message\":\"You do not have permission to access this resource.\",\"errorCode\":\"" + ApiConstants.AUTHORIZATION_ERROR + "\"}")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        }
    }
}
