package com.odc.checklistservice.entity;

import com.odc.common.constant.ChecklistItemStatus;
import com.odc.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "checklist_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistItem extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checklist_id", nullable = false)
    private Checklist checklist;

    @Column(name = "template_item_id", nullable = false)
    private UUID templateItemId;

    @Column(name = "is_checked", nullable = false)
    private Boolean isChecked;

    @Column(name = "completed_by_id")
    private String completedById;
}
