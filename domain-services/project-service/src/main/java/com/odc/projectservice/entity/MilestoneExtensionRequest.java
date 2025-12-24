package com.odc.projectservice.entity;

import com.odc.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "milestone_extension_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilestoneExtensionRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id", nullable = false)
    private ProjectMilestone milestone;

    @Column(name = "requested_end_date", nullable = false)
    private LocalDate requestedEndDate;

    @Column(name = "current_end_date", nullable = false)
    private LocalDate currentEndDate;

    @Column(name = "request_reason", columnDefinition = "TEXT", nullable = false)
    private String requestReason;

    @Column(name = "status", length = 30, nullable = false)
    private String status;
    // PENDING, APPROVED, REJECTED

    @Column(name = "review_reason", columnDefinition = "TEXT")
    private String reviewReason;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy; // company admin id

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy; // mentor id
}

