package com.odc.apigateway.security;

import com.odc.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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
            String role = jwtUtil.extractClaim(authToken, claims -> claims.get("role", String.class));

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username, null, AuthorityUtils.createAuthorityList(role.toUpperCase()));

            return Mono.just(auth);
        }

        return Mono.empty();
    }
}
