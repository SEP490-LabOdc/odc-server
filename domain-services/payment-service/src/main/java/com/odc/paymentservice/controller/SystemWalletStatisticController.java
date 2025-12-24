package com.odc.paymentservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.paymentservice.dto.response.SystemWalletStatisticResponse;
import com.odc.paymentservice.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/system/wallets")
public class SystemWalletStatisticController {

    private final WalletService service;

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<SystemWalletStatisticResponse>> getStatistic() {
        return ResponseEntity.ok(service.getSystemWalletStatistic());
    }
}

