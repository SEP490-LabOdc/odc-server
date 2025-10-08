package com.odc.checklistservice.entity;

import com.odc.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "template_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateGroup extends BaseEntity {
    @Column(nullable = false)
    private String title;

    @Column(name = "display_order")
    private Integer displayOrder;

    // Mối quan hệ Many-to-One: Nhiều group thuộc về một template
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ChecklistTemplate template;

    // Mối quan hệ One-to-Many: Một group có nhiều item
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TemplateItem> items;
}
