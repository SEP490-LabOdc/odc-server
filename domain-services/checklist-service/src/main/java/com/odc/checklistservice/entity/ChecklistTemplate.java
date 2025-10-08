package com.odc.checklistservice.entity;

import com.odc.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "checklist_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistTemplate extends BaseEntity {
    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "entity_type", nullable = false)
    private String entityType; // Ví dụ: "COMPANY", "TASK"

    // Mối quan hệ One-to-Many với TemplateGroup
    // Cascade.ALL: Khi xóa template, các item con cũng bị xóa.
    // orphanRemoval = true: Khi xóa một item khỏi list, nó cũng bị xóa khỏi DB.
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TemplateGroup> groups;
}
