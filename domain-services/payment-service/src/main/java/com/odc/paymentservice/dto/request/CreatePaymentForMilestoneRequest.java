package com.odc.paymentservice.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Getter;

import java.util.UUID;

@Getter
public class CreatePaymentForMilestoneRequest {
    private UUID milestoneId;
    private UUID projectId;
    private String milestoneTitle; // Dùng làm mô tả đơn hàng
    @Min(value = 1000, message = "Số tiền phải lớn hơn 1.000 VND")
    private Long amount;
}
