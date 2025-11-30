package com.odc.projectservice.repository;

import com.odc.projectservice.entity.ProjectMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectMilestoneRepository extends JpaRepository<ProjectMilestone, UUID>, JpaSpecificationExecutor<ProjectMilestone> {
    List<ProjectMilestone> findByProjectId(UUID projectId);

    @Query("SELECT pm FROM ProjectMilestone pm WHERE pm.project.id = :projectId ORDER BY pm.createdAt DESC LIMIT 1")
    Optional<ProjectMilestone> findLatestByProjectId(UUID projectId);
}
