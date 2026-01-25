package com.odc.paymentservice.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminHandleWithdrawalRequest {
    // Cho phép override thời gian xử lý nếu muốn, optional
//    private String processedAt; // ISO-8601, ví dụ "2025-12-08T10:00:00"
    private String adminNote;
}