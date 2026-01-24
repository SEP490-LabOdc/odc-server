package com.odc.projectservice.entity;

import com.odc.common.constant.ProjectClosureStatus;
import com.odc.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_closure_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectClosureRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // Mentor gửi request
    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ProjectClosureStatus status;

    // Lab admin review
    @Column(name = "lab_admin_id")
    private UUID labAdminId;

    @Column(name = "lab_admin_comment", columnDefinition = "TEXT")
    private String labAdminComment;

    @Column(name = "lab_admin_reviewed_at")
    private LocalDateTime labAdminReviewedAt;

    // Company review
    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "company_comment", columnDefinition = "TEXT")
    private String companyComment;

    @Column(name = "company_reviewed_at")
    private LocalDateTime companyReviewedAt;
}
