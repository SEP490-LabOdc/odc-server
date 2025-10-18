package com.odc.apigateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/user-service")
    public Mono<ResponseEntity<Map<String, Object>>> userServiceFallback() {
        return Mono.just(createFallbackResponse("User Service không khả dụng"));
    }

    @GetMapping("/business-service")
    public Mono<ResponseEntity<Map<String, Object>>> businessServiceFallback() {
        return Mono.just(createFallbackResponse("Business Service không khả dụng"));
    }

    @GetMapping("/talent-pool-service")
    public Mono<ResponseEntity<Map<String, Object>>> talentPoolServiceFallback() {
        return Mono.just(createFallbackResponse("Talent Pool Service không khả dụng"));
    }

    private ResponseEntity<Map<String, Object>> createFallbackResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());
        response.put("errorCode", "SERVICE_UNAVAILABLE");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}