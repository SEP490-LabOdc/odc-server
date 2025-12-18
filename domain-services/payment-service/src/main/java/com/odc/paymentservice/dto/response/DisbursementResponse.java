package com.odc.paymentservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class DisbursementResponse {

    private UUID disbursementId;

    private UUID milestoneId;
    private UUID projectId;

    private BigDecimal totalAmount;

    private BigDecimal systemFee;

    private BigDecimal mentorAmount;
    private UUID mentorLeaderId;

    private BigDecimal talentAmount;
    private UUID talentLeaderId;

    private String status;

    private LocalDateTime updatedAt;
}
