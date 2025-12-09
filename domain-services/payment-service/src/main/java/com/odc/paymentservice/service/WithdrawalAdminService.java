package com.odc.paymentservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.paymentservice.dto.request.AdminHandleWithdrawalRequest;
import com.odc.paymentservice.dto.request.WithdrawalFilterRequest;
import com.odc.paymentservice.dto.response.WithdrawalResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface WithdrawalAdminService {
    ApiResponse<Page<WithdrawalResponse>> list(WithdrawalFilterRequest filter);

    ApiResponse<WithdrawalResponse> detail(UUID id);

    ApiResponse<WithdrawalResponse> approve(UUID id, AdminHandleWithdrawalRequest req);

    ApiResponse<WithdrawalResponse> reject(UUID id, AdminHandleWithdrawalRequest req);
}