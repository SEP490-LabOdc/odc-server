package com.odc.companyservice.config;

import jakarta.ws.rs.HttpMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean(name = "CompanyService_SecurityFilterChain")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // *** DÒNG MỚI: Cho phép mọi request tới /api/v1/companies/** ***
                        .requestMatchers("/api/v1/companies/**").permitAll()

                        // Cho phép truy cập công khai vào các endpoint của Actuator (health check)
                        .requestMatchers("/actuator/**").permitAll()

                        // Cho phép truy cập công khai vào các endpoint của Swagger/OpenAPI
                        .requestMatchers("/company-service/v3/api-docs/**").permitAll()
                        .requestMatchers("/company-service/swagger/**").permitAll()

                        // Bất kỳ yêu cầu nào khác đều cần phải được xác thực
                        .anyRequest().authenticated()
                )
                // Vô hiệu hóa CSRF vì đây là API stateless
                .csrf(csrf -> csrf.disable())
                // Vô hiệu hóa Form Login mặc định
                .formLogin(form -> form.disable())
                // Vô hiệu hóa HTTP Basic Authentication
                .httpBasic(basic -> basic.disable())
                // Cấu hình Session Management thành STATELESS, không sử dụng session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Không có .addFilterBefore() và .exceptionHandling() như yêu cầu

        return http.build();
    }
}