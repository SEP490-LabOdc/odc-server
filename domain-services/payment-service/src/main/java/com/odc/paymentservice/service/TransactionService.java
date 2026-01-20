package com.odc.paymentservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.paymentservice.dto.response.TransactionResponse;

import java.util.UUID;

public interface TransactionService {
    ApiResponse<PaginatedResult<TransactionResponse>> getTransactionsByProjectId(
            UUID projectId,
            Integer page,
            Integer size);

    ApiResponse<TransactionResponse> getTransactionDetail(UUID transactionId);

    ApiResponse<PaginatedResult<TransactionResponse>> getAllTransactions(Integer page,
                                                                         Integer size);

    ApiResponse<PaginatedResult<TransactionResponse>> getMyTransactions(Integer page,
                                                                        Integer size);

}