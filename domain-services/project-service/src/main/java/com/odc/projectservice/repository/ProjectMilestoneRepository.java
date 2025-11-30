package com.odc.projectservice.repository;

import com.odc.projectservice.entity.ProjectMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectMilestoneRepository extends JpaRepository<ProjectMilestone, UUID>, JpaSpecificationExecutor<ProjectMilestone> {
    List<ProjectMilestone> findByProjectId(UUID projectId);

    @Query("SELECT pm FROM ProjectMilestone pm WHERE pm.project.id = :projectId ORDER BY pm.createdAt DESC LIMIT 1")
    Optional<ProjectMilestone> findLatestByProjectId(UUID projectId);

    @Query("""
                SELECT m FROM ProjectMilestone m
                WHERE m.status = 'PENDING_START'
                AND m.startDate = :today
            """)
    List<ProjectMilestone> findMilestonesToStart(@Param("today") LocalDate today);
}
