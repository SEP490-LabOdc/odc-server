package com.odc.paymentservice.entity;

import com.odc.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transaction_wallet", columnList = "wallet_id"),
        @Index(name = "idx_transaction_ref", columnList = "ref_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet; // Ví bị tác động

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "type", nullable = false)
    private String type;
    // Enum gợi ý:
    // DEPOSIT (Nạp tiền từ PayOS),
    // WITHDRAWAL (Rút tiền ra bank),
    // TRANSFER (Chuyển tiền nội bộ),
    // FEE (Thu phí)

    @Column(name = "direction", nullable = false)
    private String direction; // CREDIT (Cộng tiền +), DEBIT (Trừ tiền -)

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // Ví dụ: "Nhận tiền từ Milestone 1", "Chuyển tiền cho user A"

    // Các trường tham chiếu để biết transaction này sinh ra từ đâu
    @Column(name = "ref_id")
    private UUID refId; // ID của PaymentRequest, Disbursement, hoặc WithdrawalRequest

    @Column(name = "ref_type")
    private String refType; // PAYMENT_REQUEST, DISBURSEMENT, WITHDRAWAL_REQUEST, INTERNAL_DISTRIBUTION

    @Column(name = "status")
    private String status; // SUCCESS, FAILED, PENDING

    @Column(name = "balance_after", precision = 19, scale = 2)
    private BigDecimal balanceAfter; // Số dư ví sau khi thực hiện giao dịch (để đối soát)
}