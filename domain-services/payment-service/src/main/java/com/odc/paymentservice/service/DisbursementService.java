package com.odc.paymentservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.paymentservice.dto.request.CreateDisbursementRequest;
import com.odc.paymentservice.dto.request.MilestoneDisbursementRequest;
import com.odc.paymentservice.dto.response.DisbursementCalculationResponse;
import com.odc.paymentservice.dto.response.DisbursementResponse;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface DisbursementService {
    DisbursementCalculationResponse calculatePreview(UUID milestoneId, BigDecimal totalAmount);

    ApiResponse<Map<String, String>> calculateDisbursement(CreateDisbursementRequest request);

    DisbursementCalculationResponse executeDisbursement(UUID disbursementId);

    ApiResponse<Void> processMilestoneDisbursement(UUID milestoneId, UUID userId, MilestoneDisbursementRequest request);

    ApiResponse<DisbursementResponse> getByMilestoneId(UUID milestoneId);
}
