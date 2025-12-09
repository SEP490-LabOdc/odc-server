package com.odc.paymentservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
public class MilestoneDisbursement {
    @NotNull(message = "User ID không được để trống")
    private UUID userId;

    @NotNull(message = "Số tiền không được để trống")
    @Positive(message = "Số tiền phân bổ phải lớn hơn 0")
    private BigDecimal amount;
}
