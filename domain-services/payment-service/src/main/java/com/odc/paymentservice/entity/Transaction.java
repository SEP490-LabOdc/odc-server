package com.odc.paymentservice.entity;

import com.odc.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transaction_wallet", columnList = "wallet_id"),
        @Index(name = "idx_transaction_project", columnList = "project_id"),
        @Index(name = "idx_transaction_related_user", columnList = "related_user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet; // Ví của người xem lịch sử này

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "type", nullable = false)
    private String type;
    // DEPOSIT, MILESTONE_PAYMENT, DISBURSEMENT_IN, ALLOCATION_OUT, ALLOCATION_IN, WITHDRAWAL

    @Column(name = "direction", nullable = false)
    private String direction; // CREDIT (+), DEBIT (-)

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // --- TRACEABILITY ---
    @Column(name = "ref_id")
    private UUID refId; // ID của PaymentRequest, Disbursement, hoặc WithdrawalRequest

    @Column(name = "ref_type")
    private String refType; // "PAYMENT_REQUEST", "DISBURSEMENT", "WITHDRAWAL"

    // --- CONTEXT (Để query lịch sử nhanh) ---
    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "milestone_id")
    private UUID milestoneId;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "related_user_id")
    private UUID relatedUserId; // Người gửi/nhận tiền liên quan (để hiển thị "Nhận từ ai/Chuyển cho ai")

    @Column(name = "status")
    private String status; // SUCCESS

    @Column(name = "balance_after", precision = 19, scale = 2)
    private BigDecimal balanceAfter;
}