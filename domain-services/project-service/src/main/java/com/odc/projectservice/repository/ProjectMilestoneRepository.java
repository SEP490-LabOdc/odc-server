package com.odc.projectservice.repository;

import com.odc.projectservice.entity.ProjectMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface ProjectMilestoneRepository extends JpaRepository<ProjectMilestone, UUID>, JpaSpecificationExecutor<ProjectMilestone> {
    List<ProjectMilestone> findByProjectId(UUID projectId);
}
