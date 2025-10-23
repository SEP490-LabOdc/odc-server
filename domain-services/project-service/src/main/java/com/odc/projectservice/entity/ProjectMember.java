package com.odc.projectservice.entity;

import com.odc.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "is_leader", nullable = false)
    private boolean isLeader = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
}
