package com.odc.projectservice.entity;

import com.odc.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
@Entity
@Table(name = "skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Skill extends BaseEntity {
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToMany(mappedBy = "skills")
    private Set<Project> projects = new HashSet<>();
}
