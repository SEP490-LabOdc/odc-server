package com.odc.apigateway.security;

import com.odc.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtUtil jwtUtil;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String authToken = authentication.getCredentials().toString();
        String username;

        try {
            username = jwtUtil.extractUsername(authToken);
        } catch (Exception e) {
            log.error("JWT token validation failed: {}", e.getMessage());
            return Mono.empty();
        }

        if (username != null && jwtUtil.validateToken(authToken, username)) {
            // Extract roles from JWT claims (if available)
            List<String> roles = extractRolesFromToken(authToken);
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);

            return Mono.just(auth);
        }

        return Mono.empty();
    }

    private List<String> extractRolesFromToken(String token) {
        try {
            // Extract roles from JWT claims
            // This is a simplified implementation
            return List.of("ROLE_USER");
        } catch (Exception e) {
            log.warn("Could not extract roles from token: {}", e.getMessage());
            return List.of("ROLE_USER");
        }
    }
}
