//package com.odc.paymentservice.entity;
//
//import com.odc.common.entity.BaseEntity;
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.Table;
//import lombok.*;
//
//import java.math.BigDecimal;
//import java.util.UUID;
//
//@Builder
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Entity
//@Table(name = "payment_requests")
//public class PaymentRequest extends BaseEntity {
//    @Column(name = "company_id", nullable = false)
//    private UUID companyId;
//
//    @Column(name = "project_id", nullable = false)
//    private UUID projectId;
//
//    @Column(name = "milestone_id", nullable = false)
//    private UUID milestoneId;
//
//    @Column(name = "amount", precision = 19, scale = 2)
//    private BigDecimal amount;
//
//    @Column(name = "payment_content")
//    private String paymentContent; // Nội dung CK: "ODC <Mã_HD>"
//
//    @Column(name = "qr_code_url")
//    private String qrCodeUrl;
//
//    @Column(name = "status")
//    private String status; // PENDING, PAID, CANCELLED, FAILED
//
//    @Column(name = "transaction_ref_id")
//    private String transactionRefId; // Mã tham chiếu từ ngân hàng
//}