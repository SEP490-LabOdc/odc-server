package com.odc.projectservice.entity;

import com.odc.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "project_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMember extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "role_in_project", length = 100)
    private String roleInProject;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @OneToMany(mappedBy = "projectMember")
    private List<MilestoneMember> milestoneMembers;
}
