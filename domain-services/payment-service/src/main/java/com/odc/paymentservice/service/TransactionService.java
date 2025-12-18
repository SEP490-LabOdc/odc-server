package com.odc.paymentservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.paymentservice.dto.response.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TransactionService {
    ApiResponse<Page<TransactionResponse>> getTransactionsByProjectId(
            UUID projectId,
            Pageable pageable
    );

    ApiResponse<TransactionResponse> getTransactionDetail(UUID transactionId);

    ApiResponse<Page<TransactionResponse>> getAllTransactions(Pageable pageable);

    ApiResponse<Page<TransactionResponse>> getMyTransactions(Pageable pageable);

}