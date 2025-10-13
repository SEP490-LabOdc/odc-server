package com.odc.emailservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.emailservice.dto.request.ConfirmOtpRequest;
import com.odc.emailservice.dto.request.SendOtpRequest;
import com.odc.emailservice.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/otp")
@RequiredArgsConstructor
public class OtpController {
    private final OtpService otpService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@RequestBody SendOtpRequest sendOtpRequest) {
        otpService.sendOtpRequest(sendOtpRequest);
        return ResponseEntity.ok(ApiResponse.success("Gửi mã OTP thành công.", null));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@RequestBody ConfirmOtpRequest request) {
        otpService.confirmOtpRequest(request);
        return ResponseEntity.ok(ApiResponse.success("Xác minh mã OTP thành công.", null));
    }
}
