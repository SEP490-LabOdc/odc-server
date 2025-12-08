package com.odc.paymentservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.paymentservice.dto.request.AdminHandleWithdrawalRequest;
import com.odc.paymentservice.dto.request.WithdrawalFilterRequest;
import com.odc.paymentservice.dto.response.WithdrawalResponse;
import com.odc.paymentservice.service.WithdrawalAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/withdrawal-requests")
@RequiredArgsConstructor
public class WithdrawalAdminController {

    private final WithdrawalAdminService withdrawalAdminService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<WithdrawalResponse>>> list(WithdrawalFilterRequest filter) {
        return ResponseEntity.ok(withdrawalAdminService.list(filter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WithdrawalResponse>> detail(@PathVariable UUID id) {
        return ResponseEntity.ok(withdrawalAdminService.detail(id));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<WithdrawalResponse>> approve(
            @PathVariable UUID id,
            @Valid @RequestBody AdminHandleWithdrawalRequest req) {
        return ResponseEntity.ok(withdrawalAdminService.approve(id, req));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<WithdrawalResponse>> reject(
            @PathVariable UUID id,
            @Valid @RequestBody AdminHandleWithdrawalRequest req) {
        return ResponseEntity.ok(withdrawalAdminService.reject(id, req));
    }
}