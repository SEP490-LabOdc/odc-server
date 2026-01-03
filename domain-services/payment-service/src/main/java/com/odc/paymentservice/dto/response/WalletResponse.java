package com.odc.paymentservice.dto.response;

import com.odc.paymentservice.entity.BankInfo;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class WalletResponse {
    private UUID id;
    private UUID ownerId;
    private String ownerType;
    private BigDecimal balance;
    private BigDecimal heldBalance;
    private String currency;
    private String status;
    private List<BankInfo> bankInfos;
}
