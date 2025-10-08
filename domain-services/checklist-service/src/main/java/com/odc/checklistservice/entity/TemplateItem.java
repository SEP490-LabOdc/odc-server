package com.odc.checklistservice.entity;

import com.odc.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "template_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateItem extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private TemplateGroup group;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "is_required")
    private Boolean isRequired;
}
