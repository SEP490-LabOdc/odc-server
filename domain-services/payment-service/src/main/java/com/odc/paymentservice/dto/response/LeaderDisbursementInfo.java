package com.odc.paymentservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LeaderDisbursementInfo {
    private String userId;
    private String fullName;
    private String email;
    private String avatarUrl;
    private String roleInProject; // MENTOR / TALENT
    private boolean isLeader;
    private BigDecimal amount;    // số tiền leader được nhận
}
