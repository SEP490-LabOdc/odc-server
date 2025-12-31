package com.odc.paymentservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankInfo {
    private String bankName;
    private String accountNumber;
    private String accountHolderName;
}
