package com.odc.apigateway.config;

import com.odc.apigateway.security.CustomServerAccessDeniedHandler;
import com.odc.apigateway.security.CustomServerAuthenticationEntryPoint;
import com.odc.apigateway.security.JwtAuthenticationManager;
import com.odc.apigateway.security.SecurityContextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // Grouped path patterns for clearer authorization rules
    private static final String[] PERMIT_ALL_PATHS = {
            "/actuator/**",
            "/api/v1/auth/**",
            "/api/v1/companies/register",
            "/api/v1/companies/for-update",
            "/api/v1/otp/**",
            "/api/v1/users", // Allow GET all users without auth (base path)
            "/v3/api-docs/**",
            "/swagger/**",
            "/user-service/v3/api-docs/**",
            "/company-service/v3/api-docs/**",
            "/ws/**"
    };
    private static final String[] AUTHENTICATED_PATHS = {
            "/api/v1/users/**", // Other user endpoints still need auth
            "/api/v1/businesses/**",
            "/api/v1/talents/**"
    };
    private static final String[] GET_PERMIT_PATHS = {
            "/api/v1/companies"
    };
    private static final String[] POST_PERMIT_PATHS = {
            "/api/v1/files/upload"
    };
    private static final String[] OPTIONS_ANY_PATH = {"/**"};
    private final JwtAuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final CustomServerAccessDeniedHandler customAccessDeniedHandler;
    private final CustomServerAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .authenticationManager(authenticationManager)
                .securityContextRepository(securityContextRepository)
                .authorizeExchange(exchanges -> {
                    // Public endpoints
                    exchanges.pathMatchers(PERMIT_ALL_PATHS).permitAll();
                    exchanges.pathMatchers(HttpMethod.GET, GET_PERMIT_PATHS).permitAll();
                    exchanges.pathMatchers(HttpMethod.POST, POST_PERMIT_PATHS).permitAll();
                    exchanges.pathMatchers(HttpMethod.OPTIONS, OPTIONS_ANY_PATH).permitAll();

                    // Protected endpoints
                    exchanges.pathMatchers(AUTHENTICATED_PATHS).authenticated();
                    exchanges.anyExchange().authenticated();
                })
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .exceptionHandling(
                        exceptions -> exceptions
                                .accessDeniedHandler(customAccessDeniedHandler)
                                .authenticationEntryPoint(customAuthenticationEntryPoint))
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Collections.singletonList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
