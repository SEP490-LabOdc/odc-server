package com.odc.userservice.security;

import com.odc.common.exception.ResourceNotFoundException;
import com.odc.common.util.JwtUtil;
import com.odc.userservice.entity.User;
import com.odc.userservice.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    // Paths that should bypass JWT filter for efficiency
    private static final String[] EXCLUDED_PREFIXES = {
            "/api/v1/auth/",
            "/actuator/",
            "/user-service/"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Log request details for debugging
        log.debug("Processing request: {} {}", request.getMethod(), request.getRequestURI());

        // Check if request comes from API Gateway with X-User-ID header
        String userId = request.getHeader("X-User-ID");
        if (userId != null && !userId.isEmpty()) {
            log.debug("Found X-User-ID header: {}", userId);

            User user = userRepository
                    .findById(UUID.fromString(userId))
                    .orElseThrow(() -> new ResourceNotFoundException("Not found userId!"));

            // Set authentication context
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    UUID.fromString(userId), null,
                    AuthorityUtils.createAuthorityList(user.getRole().getName().toUpperCase()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            // 2. Nếu không có X-User-ID thì check Authorization Bearer token
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = jwtUtil.extractUsername(token);

                if (jwtUtil.validateToken(token, email)) {

                    User user = userRepository.findByEmail(email)
                            .orElseThrow(() -> new ResourceNotFoundException("Not found user!"));

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            user.getId(),
                            null,
                            AuthorityUtils.createAuthorityList(user.getRole().getName().toUpperCase()));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String prefix : EXCLUDED_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
