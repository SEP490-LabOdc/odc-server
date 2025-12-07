package com.odc.paymentservice.dto.request;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class CreateDisbursementRequest {
    private UUID projectId, milestoneId, mentorLeaderId, talentLeaderId;
    private BigDecimal totalAmount;
}
