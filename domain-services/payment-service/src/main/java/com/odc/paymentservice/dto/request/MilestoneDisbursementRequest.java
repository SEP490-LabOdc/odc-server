package com.odc.paymentservice.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

@Getter
public class MilestoneDisbursementRequest {
    @NotEmpty(message = "Danh sách phân bổ không được trống.")
    private List<MilestoneDisbursement> disbursements;
}
