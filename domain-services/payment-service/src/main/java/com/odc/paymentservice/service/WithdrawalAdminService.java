package com.odc.paymentservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.paymentservice.dto.request.AdminHandleWithdrawalRequest;
import com.odc.paymentservice.dto.request.WithdrawalFilterRequest;
import com.odc.paymentservice.dto.response.WithdrawalResponse;

import java.util.UUID;

public interface WithdrawalAdminService {
    ApiResponse<PaginatedResult<WithdrawalResponse>> list(WithdrawalFilterRequest filter);

    ApiResponse<WithdrawalResponse> detail(UUID id);

    ApiResponse<WithdrawalResponse> approve(UUID id, AdminHandleWithdrawalRequest req);

    ApiResponse<WithdrawalResponse> reject(UUID id, AdminHandleWithdrawalRequest req);
}