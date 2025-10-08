package com.odc.notificationservice.dto.request;

import lombok.Getter;

import java.util.UUID;

@Getter
public class RegisterDeviceTokenRequest {
    private UUID userId;
    private String deviceToken, platform;
}
