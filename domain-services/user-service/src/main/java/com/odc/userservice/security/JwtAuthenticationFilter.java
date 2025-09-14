package com.odc.userservice.security;

import com.odc.common.exception.ResourceNotFoundException;
import com.odc.userservice.entity.User;
import com.odc.userservice.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private UserRepository userRepository;

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
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId, null,
                    AuthorityUtils.createAuthorityList(user.getRole().getName().toUpperCase()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip filter for auth endpoints and actuator
        return path.startsWith("/api/v1/auth/") || path.startsWith("/actuator/");
    }
}
