package com.odc.projectservice.config;

import com.odc.common.exception.CustomAccessDeniedHandler;
import com.odc.common.exception.CustomAuthenticationEntryPoint;
import com.odc.projectservice.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    // Grouped public paths for clarity
    private static final String[] PERMIT_ALL_PATHS = {
            "/api/v1/auth/**",
            "/actuator/**",
            "/project-service/v3/api-docs/**",
            "/project-service/swagger/**",
            "/api/v1/project-applications/apply",
            "/api/v1/projects/hiring"
    };

    private static final String[] GET_ALL_PATHS = {
            "/api/v1/skills/**"
    };
    private static final String[] POST_ALL_PATHS = {
            "/api/v1/skills/search"
    };
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
//                        .requestMatchers(PERMIT_ALL_PATHS).permitAll()
                                .requestMatchers(HttpMethod.GET, GET_ALL_PATHS).permitAll()
                                .requestMatchers(HttpMethod.POST, POST_ALL_PATHS).permitAll()
                                .anyRequest().permitAll()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler(customAccessDeniedHandler)
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                );

        return http.build();
    }
}
