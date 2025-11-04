package com.odc.projectservice.client;

import com.odc.common.dto.ApiResponse;
import com.odc.userservice.dto.response.GetUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {

    @LoadBalanced
    private final RestTemplate restTemplate;

    @Value("${services.user-service.url:http://user-service}")
    private String userServiceUrl;

    public GetUserResponse getUserById(UUID userId) {
        String url = userServiceUrl + "/api/v1/users/" + userId;

        try {
            ResponseEntity<ApiResponse<GetUserResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ApiResponse<GetUserResponse>>() {
                    }
            );

            if (response.getBody() != null && response.getBody().isSuccess() && response.getBody().getData() != null) {
                return response.getBody().getData();
            }
        } catch (HttpClientErrorException.NotFound e) {
            // User không tồn tại - trả về null thay vì throw exception
            log.warn("User với ID {} không tồn tại trong user-service", userId);
            return null;
        } catch (HttpClientErrorException e) {
            // Các lỗi HTTP khác (401, 403, 500, ...)
            log.error("Lỗi khi gọi user-service cho user {}: {} - {}", userId, e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (RestClientException e) {
            // Lỗi kết nối hoặc các lỗi khác
            log.error("Lỗi kết nối đến user-service khi lấy user {}: {}", userId, e.getMessage());
            return null;
        }

        return null;
    }
}