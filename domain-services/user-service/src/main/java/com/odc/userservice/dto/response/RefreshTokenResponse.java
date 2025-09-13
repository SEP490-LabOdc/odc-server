package com.odc.userservice.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RefreshTokenResponse {
    private String accessToken;
}
