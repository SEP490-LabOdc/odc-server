package com.odc.userservice.dto.request;

import lombok.Getter;

import java.util.UUID;

@Getter
public class RefreshTokenRequest {
    private UUID userId;
    private String refreshToken;
}
