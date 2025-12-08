package com.odc.paymentservice.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WithdrawalFilterRequest {
    private String status;    // PENDING/APPROVED/REJECTED/COMPLETED
    private String fromDate;  // yyyy-MM-dd
    private String toDate;    // yyyy-MM-dd
    private Integer page = 0;
    private Integer size = 20;
}