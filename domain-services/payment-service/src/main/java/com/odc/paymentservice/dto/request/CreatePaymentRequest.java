package com.odc.paymentservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.UUID;

@Getter
public class CreatePaymentRequest {
    @NotNull(message = "Milestone ID không được để trống")
    private UUID milestoneId;

    @NotNull(message = "Project ID không được để trống")
    private UUID projectId;

    @NotNull(message = "Company ID không được để trống")
    private UUID companyId;

    private String milestoneTitle; // Dùng làm mô tả đơn hàng

    @Min(value = 1000, message = "Số tiền phải lớn hơn 1.000 VND")
    private Long amount;

    // URL để PayOS redirect về sau khi thanh toán xong hoặc hủy
    private String returnUrl;
    private String cancelUrl;
}
