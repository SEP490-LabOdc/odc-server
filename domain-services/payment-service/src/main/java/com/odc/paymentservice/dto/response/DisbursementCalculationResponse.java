package com.odc.paymentservice.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Data
@Builder
public class DisbursementCalculationResponse {
    private String milestoneId;
    private BigDecimal totalAmount;
    private BigDecimal systemFee;
    private String status;

    private LeaderDisbursementInfo mentorLeader;  // Leader team Mentor
    private LeaderDisbursementInfo talentLeader;  // Leader team Talent
}
