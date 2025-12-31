package com.odc.paymentservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.paymentservice.dto.request.CreateWithdrawalRequest;
import com.odc.paymentservice.dto.request.UpdateBankInfoRequest;
import com.odc.paymentservice.dto.response.WalletResponse;
import com.odc.paymentservice.dto.response.WithdrawalResponse;
import com.odc.paymentservice.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {
    private final WalletService walletService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet() {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(walletService.getMyWallet(userId));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<WithdrawalResponse>> createWithdrawalRequest(
            @Valid @RequestBody CreateWithdrawalRequest request) {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(walletService.createWithdrawalRequest(userId, request));
    }

    @PostMapping("/bank-info")
    public ResponseEntity<ApiResponse<WalletResponse>> updateBankInfos(
            @Valid @RequestBody UpdateBankInfoRequest request) {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(walletService.addBankInfo(userId, request));
    }
}