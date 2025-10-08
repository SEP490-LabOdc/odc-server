package com.odc.notificationservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.BusinessException;
import com.odc.common.exception.ResourceNotFoundException;
import com.odc.notificationservice.dto.request.RegisterDeviceTokenRequest;
import com.odc.notificationservice.dto.response.GetDeviceTokenResponse;
import com.odc.notificationservice.entity.DeviceToken;
import com.odc.notificationservice.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceTokenServiceImpl implements DeviceTokenService {
    private final DeviceTokenRepository deviceTokenRepository;

    @Override
    public ApiResponse<Void> registerDeviceToken(RegisterDeviceTokenRequest request) {
        if (deviceTokenRepository.existsByToken(request.getDeviceToken())) {
            throw new BusinessException("Device token đã được đăng ký.");
        }

        DeviceToken deviceToken = DeviceToken.builder()
                .token(request.getDeviceToken())
                .userId(request.getUserId())
                .platform(request.getPlatform())
                .build();

        deviceTokenRepository.save(deviceToken);

        return ApiResponse.success("Device token đã được đăng ký thành công.", null);
    }

    @Override
    public ApiResponse<List<GetDeviceTokenResponse>> getDeviceTokensByUserId(UUID userId) {
        return ApiResponse.success(deviceTokenRepository.findAllByUserId(userId).get().stream()
                .map(deviceToken -> GetDeviceTokenResponse
                        .builder()
                        .token(deviceToken.getToken())
                        .platform(deviceToken.getPlatform())
                        .build())
                .toList());
    }

    @Override
    public ApiResponse<Void> deleteDeviceToken(String token) {
        DeviceToken deviceToken = deviceTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy device token."));

        deviceTokenRepository.delete(deviceToken);

        return ApiResponse.success("Đã xóa device token thành công.", null);
    }
}
