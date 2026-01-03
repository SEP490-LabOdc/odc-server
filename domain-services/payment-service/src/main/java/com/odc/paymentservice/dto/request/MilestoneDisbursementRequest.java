package com.odc.paymentservice.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class MilestoneDisbursementRequest {
    @NotNull(message = "Wallet ID không được để trống")
    private UUID walletId;

    @NotEmpty(message = "Danh sách phân bổ không được trống.")
    private List<MilestoneDisbursement> disbursements;
}
