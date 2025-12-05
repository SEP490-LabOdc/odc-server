package com.odc.projectservice.entity;

import com.odc.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "milestone_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilestoneMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id", nullable = false)
    private ProjectMilestone projectMilestone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_member_id", nullable = false)
    private ProjectMember projectMember;

    @Column(name = "is_leader", nullable = false)
    private boolean isLeader = false;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
