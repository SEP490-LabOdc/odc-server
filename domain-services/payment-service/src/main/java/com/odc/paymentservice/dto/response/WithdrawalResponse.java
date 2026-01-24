package com.odc.paymentservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class WithdrawalResponse {
    private UUID id;
    private UUID userId;
    private String fullName, avatarUrl, email;
    private UUID walletId;
    private BigDecimal amount;
    private Map<String, String> bankInfo;
    private String status;
    private String adminNote;
    private LocalDate scheduledAt;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}