package com.odc.paymentservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.paymentservice.dto.request.CreateWithdrawalRequest;
import com.odc.paymentservice.dto.response.WalletResponse;
import com.odc.paymentservice.dto.response.WithdrawalResponse;

import java.util.UUID;

public interface WalletService {
    ApiResponse<WalletResponse> getMyWallet(UUID userId);
    ApiResponse<WithdrawalResponse> createWithdrawalRequest(UUID userId, CreateWithdrawalRequest request);
}
