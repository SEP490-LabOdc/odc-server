package com.odc.paymentservice.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemWalletStatisticResponse {

    private BigDecimal currentBalance;

    private BigDecimal totalRevenue;
}
