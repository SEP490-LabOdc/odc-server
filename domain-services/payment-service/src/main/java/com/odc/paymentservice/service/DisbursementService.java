package com.odc.paymentservice.service;

import com.odc.paymentservice.dto.request.CreateDisbursementRequest;
import com.odc.paymentservice.dto.response.DisbursementCalculationResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface DisbursementService {
    DisbursementCalculationResponse calculatePreview(UUID milestoneId, BigDecimal totalAmount);

    void calculateDisbursement(CreateDisbursementRequest request);

    DisbursementCalculationResponse executeDisbursement(UUID disbursementId);
}
