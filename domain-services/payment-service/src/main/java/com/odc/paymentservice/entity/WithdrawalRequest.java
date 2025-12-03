//package com.odc.paymentservice.entity;
//
//import com.odc.common.entity.BaseEntity;
//import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
//import jakarta.persistence.*;
//import lombok.*;
//import org.hibernate.annotations.Type;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.Map;
//import java.util.UUID;
//
//@Builder
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Entity
//@Table(name = "withdrawal_requests")
//public class WithdrawalRequest extends BaseEntity {
//    @Column(name = "user_id", nullable = false)
//    private UUID userId;
//
//    @ManyToOne
//    @JoinColumn(name = "wallet_id", nullable = false)
//    private Wallet wallet;
//
//    @Column(name = "amount", precision = 19, scale = 2)
//    private BigDecimal amount;
//
//    @Type(JsonBinaryType.class)
//    @Column(name = "bank_info", columnDefinition = "jsonb")
//    private Map<String, String> bankInfo; // { "bankName": "VCB", "accountNo": "...", "accountName": "..." }
//
//    @Column(name = "status")
//    private String status; // PENDING, APPROVED, COMPLETED, REJECTED
//
//    @Column(name = "admin_note")
//    private String adminNote;
//
//    @Column(name = "scheduled_at")
//    private LocalDate scheduledAt; // Ngày dự kiến chuyển (19 hàng tháng)
//
//    @Column(name = "processed_at")
//    private LocalDateTime processedAt;
//}
