package com.odc.notificationservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.notificationservice.dto.request.RegisterDeviceTokenRequest;
import com.odc.notificationservice.dto.response.GetDeviceTokenResponse;
import com.odc.notificationservice.service.DeviceTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/device-tokens")
@RequiredArgsConstructor
public class DeviceTokenController {
    private final DeviceTokenService deviceTokenService;

    @GetMapping("/{userId}")
    @PreAuthorize("#userId == principal or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<GetDeviceTokenResponse>>> getDeviceTokens(@PathVariable UUID userId) {
        return ResponseEntity.ok(deviceTokenService.getDeviceTokensByUserId(userId));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> registerDeviceToken(@RequestBody RegisterDeviceTokenRequest registerDeviceTokenRequest) {
        return ResponseEntity.ok(deviceTokenService.registerDeviceToken(registerDeviceTokenRequest));
    }

    @DeleteMapping("/{token}")
    public ResponseEntity<ApiResponse<Void>> deleteDeviceToken(@PathVariable("token") String token) {
        return ResponseEntity.ok(deviceTokenService.deleteDeviceToken(token));
    }
}

