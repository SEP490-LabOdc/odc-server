package com.odc.checklistservice.entity;

import com.odc.common.constant.ChecklistStatus;
import com.odc.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "checklists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Checklist extends BaseEntity {
    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    // QUAN TRỌNG: Lưu ID từ service khác, không tạo FK
    @Column(name = "entity_id", nullable = false)
    private String entityId;

    // QUAN TRỌNG: Lưu ID của assignee từ service khác
    @Column(name = "assignee_id")
    private String assigneeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChecklistStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "checklist", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ChecklistItem> items = new ArrayList<>();;
}
