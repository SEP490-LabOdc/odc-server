package com.odc.projectservice.repository;

import com.odc.projectservice.entity.ProjectApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectApplicationRepository extends JpaRepository<ProjectApplication, UUID> {

    @Query("SELECT pa FROM ProjectApplication pa WHERE pa.project.id = :projectId")
    List<ProjectApplication> findByProjectId(UUID projectId);

    boolean existsByProject_IdAndUserId(UUID projectId, UUID userId);

    Integer countByProjectId(UUID projectId);
}
