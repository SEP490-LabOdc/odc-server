package com.odc.paymentservice.entity;

import com.odc.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "type", nullable = false)
    private String type; // DEPOSIT, WITHDRAWAL, TRANSFER, FEE

    @Column(name = "direction", nullable = false)
    private String direction; // CREDIT (+), DEBIT (-)

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "ref_id")
    private UUID refId; // ID tham chiếu (PaymentRequest, WithdrawalRequest)

    @Column(name = "ref_type")
    private String refType; // PAYMENT_REQUEST, WITHDRAWAL_REQUEST

    @Column(name = "status")
    private String status; // SUCCESS, FAILED, PENDING

    @Column(name = "balance_after", precision = 19, scale = 2)
    private BigDecimal balanceAfter; // Số dư sau giao dịch (để đối soát)
}