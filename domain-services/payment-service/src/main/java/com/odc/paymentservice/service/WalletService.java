package com.odc.paymentservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.paymentservice.dto.response.WalletResponse;

import java.util.UUID;

public interface WalletService {
    ApiResponse<WalletResponse> getMyWallet(UUID userId);
}
