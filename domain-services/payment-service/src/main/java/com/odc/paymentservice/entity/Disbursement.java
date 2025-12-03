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
//@Table(name = "disbursements")
//public class Disbursement extends BaseEntity { // Ghi nhận việc phân bổ tiền từ System về Leader.
//    @Column(name = "milestone_id", nullable = false)
//    private UUID milestoneId;
//
//    @Column(name = "payment_request_id", nullable = false)
//    private UUID paymentRequestId;
//
//    @Column(name = "leader_wallet_id", nullable = false)
//    private UUID leaderWalletId;
//
//    @Column(name = "amount", precision = 19, scale = 2)
//    private BigDecimal amount;
//
//    @Column(name = "status")
//    private String status; // COMPLETED
//}
