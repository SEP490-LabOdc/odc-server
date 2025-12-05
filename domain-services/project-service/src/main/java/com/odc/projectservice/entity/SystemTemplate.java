package com.odc.projectservice.entity;

import com.odc.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "system_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemTemplate extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name; // VD: "Project Task Import Template", "Weekly Report Template"

    @Column(nullable = false)
    private String type; // Enum: REPORT, TASK_IMPORT, PROJECT_PLANNING

    @Column
    private String category; // Enum: PROJECT, REPORT

    @Column(nullable = false)
    private String fileUrl; // URL lấy từ File Service sau khi upload

    @Column(nullable = false)
    private String fileName; // Tên file gốc: "template_v1.xlsx"

    private String description;
}
