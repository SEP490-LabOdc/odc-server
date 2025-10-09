package com.odc.userservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class UserLoginResponse {
    private String accessToken, refreshToken;
}
