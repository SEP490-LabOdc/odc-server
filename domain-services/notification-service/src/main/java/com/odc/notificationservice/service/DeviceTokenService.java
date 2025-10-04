package com.odc.notificationservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.notificationservice.dto.request.RegisterDeviceTokenRequest;
import com.odc.notificationservice.dto.response.GetDeviceTokenResponse;

import java.util.List;
import java.util.UUID;

public interface DeviceTokenService {
    ApiResponse<Void> registerDeviceToken(RegisterDeviceTokenRequest request);
    ApiResponse<List<GetDeviceTokenResponse>> getDeviceTokensByUserId(UUID userId);
    ApiResponse<Void> deleteDeviceToken(String token);
}
