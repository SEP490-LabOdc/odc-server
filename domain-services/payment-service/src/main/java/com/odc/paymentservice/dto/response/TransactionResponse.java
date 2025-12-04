package com.odc.paymentservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TransactionResponse {
    private UUID id;
    private BigDecimal amount;
    private String type; // DEPOSIT, WITHDRAWAL, TRANSFER, FEE
    private String direction; // CREDIT, DEBIT
    private String description;
    private String status; // SUCCESS, FAILED, PENDING
    private BigDecimal balanceAfter;

    // Thông tin liên quan
    private UUID walletId;
    private UUID refId; // PaymentRequest ID
    private String refType;

    // Thông tin project (nếu có)
    private UUID projectId;
    private UUID milestoneId;
    private UUID companyId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}