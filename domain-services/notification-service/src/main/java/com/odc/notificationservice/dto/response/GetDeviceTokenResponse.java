package com.odc.notificationservice.dto.response;

import lombok.Builder;
import lombok.Setter;

@Setter
@Builder
public class GetDeviceTokenResponse {
    public String token, platform;
}
