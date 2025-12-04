package com.odc.paymentservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.paymentservice.dto.response.WalletResponse;
import com.odc.paymentservice.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}