package com.odc.paymentservice.service;

import java.math.BigDecimal;
import java.util.UUID;

public interface DisbursementService {
    void executeDisbursement(UUID disbursementId);
    void distributeToMember(UUID leaderId, UUID memberId, UUID disbursementId, BigDecimal amount);
}
