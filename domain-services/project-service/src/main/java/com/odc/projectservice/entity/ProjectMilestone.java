package com.odc.projectservice.entity;

import com.odc.common.entity.BaseEntity;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "project_milestones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMilestone extends BaseEntity {
    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "budget", precision = 38, scale = 0)
    private BigDecimal budget;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @OneToMany(mappedBy = "projectMilestone")
    private List<MilestoneMember> members;

    @Type(JsonBinaryType.class)
    @Column(name = "attachment_urls", columnDefinition = "jsonb")
    private List<MilestoneAttachment> attachmentUrls;

    @OneToMany(mappedBy = "milestone", cascade = CascadeType.ALL)
    private List<MilestoneExtensionRequest> extensionRequests;
}
