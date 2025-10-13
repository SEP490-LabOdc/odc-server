package com.odc.emailservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.emailservice.dto.request.ConfirmOtpRequest;
import com.odc.emailservice.dto.request.SendOtpRequest;

public interface OtpService {
    void sendOtpRequest(SendOtpRequest request);
    void sendOtpRequest(String request);
    void confirmOtpRequest(ConfirmOtpRequest request);
}
