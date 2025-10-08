package com.odc.emailservice.dto.request;

import lombok.Getter;

@Getter
public class ConfirmOtpRequest {
    private String email;
    private String otp;
}
