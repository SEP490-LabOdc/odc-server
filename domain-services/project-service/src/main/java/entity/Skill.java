package entity;

import com.odc.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToMany;

import java.util.HashSet;
import java.util.Set;

public class Skill extends BaseEntity {
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToMany(mappedBy = "skills")
    private Set<Project> projects = new HashSet<>();
}
