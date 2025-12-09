package com.odc.paymentservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.paymentservice.dto.request.CreateDisbursementRequest;
import com.odc.paymentservice.dto.request.MilestoneDisbursementRequest;
import com.odc.paymentservice.dto.response.DisbursementCalculationResponse;
import com.odc.paymentservice.service.DisbursementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/disbursement")
@RequiredArgsConstructor
public class DisbursementController {

    private final DisbursementService disbursementService;

    @GetMapping("/preview")
    public ResponseEntity<DisbursementCalculationResponse> previewDisbursement(
            @RequestParam UUID milestoneId,
            @RequestParam BigDecimal totalAmount
    ) {
        DisbursementCalculationResponse response = disbursementService.calculatePreview(milestoneId, totalAmount);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/calculate")
    public ResponseEntity<Void> createDisbursement(
            @RequestBody CreateDisbursementRequest request
    ) {
        disbursementService.calculateDisbursement(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/execute/{disbursementId}")
    public ResponseEntity<DisbursementCalculationResponse> executeDisbursement(
            @PathVariable UUID disbursementId
    ) {
        DisbursementCalculationResponse response = disbursementService.executeDisbursement(disbursementId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("milestones/{milestoneId}/disburse")
    ResponseEntity<ApiResponse<Void>> disburseMilestoneFunds(
            @PathVariable UUID milestoneId,
            @RequestBody @Valid MilestoneDisbursementRequest request
    ) {
        return ResponseEntity.ok(disbursementService.processMilestoneDisbursement(milestoneId, request));
    }
}
