package com.odc.paymentservice.entity;

import com.odc.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment_requests")
public class PaymentRequest extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId; // Người nạp tiền

    // Cho phép null vì nạp tiền vào ví chưa gắn với dự án
    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "milestone_id")
    private UUID milestoneId;

    @Column(name = "amount", precision = 38, scale = 0)
    private BigDecimal amount;

    @Column(name = "order_code", nullable = false, unique = true)
    private Long orderCode;

    @Column(name = "checkout_url")
    private String checkoutUrl;

    @Column(name = "qr_code_url")
    private String qrCodeUrl;

    @Column(name = "status")
    private String status;

    @Column(name = "transaction_ref_id")
    private String transactionRefId;

    @Column(name = "type")
    private String type; // "DEPOSIT" hoặc "PAYMENT"
}