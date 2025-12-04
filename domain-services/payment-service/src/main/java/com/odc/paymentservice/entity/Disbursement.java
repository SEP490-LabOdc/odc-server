package com.odc.paymentservice.entity;

import com.odc.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "disbursements", indexes = {
        @Index(name = "idx_disbursement_milestone", columnList = "milestone_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Disbursement extends BaseEntity { // Lưu thông tin Admin đã chia tiền Milestone như thế nào (10/20/70).

    @Column(name = "milestone_id", nullable = false)
    private UUID milestoneId; // Milestone nào được giải ngân

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    // Tổng tiền nhận được từ Milestone (100%)
    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount;

    // --- Phần chia 10% cho System ---
    @Column(name = "system_fee", precision = 19, scale = 2)
    private BigDecimal systemFee;

    // --- Phần chia 20% cho Mentor Team ---
    @Column(name = "mentor_amount", precision = 19, scale = 2)
    private BigDecimal mentorAmount;

    @Column(name = "mentor_leader_id")
    private UUID mentorLeaderId; // Người nhận đại diện (Leader Mentor)

    // --- Phần chia 70% cho Talent Team ---
    @Column(name = "talent_amount", precision = 19, scale = 2)
    private BigDecimal talentAmount;

    @Column(name = "talent_leader_id")
    private UUID talentLeaderId; // Người nhận đại diện (Leader Talent)

    @Column(name = "status")
    private String status; // COMPLETED, PENDING
}