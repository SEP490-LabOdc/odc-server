package com.odc.notificationservice.security;

import com.odc.common.exception.UnauthenticatedException;
import com.odc.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthChannelInterceptor implements ChannelInterceptor {
    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = jwtUtil.extractUsername(token);
                if (jwtUtil.validateToken(token, email)) {
                    String role = jwtUtil.extractClaim(token, claims -> claims.get("role")).toString();
                    UUID userId = UUID.fromString(jwtUtil.extractClaim(token, claims -> claims.get("userId")).toString());

                    accessor.setUser(new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            AuthorityUtils.createAuthorityList(role.toUpperCase())
                    ));
                }
            } else {
                throw new UnauthenticatedException("Invalid token");
            }
        }

        return message;
    }
}
