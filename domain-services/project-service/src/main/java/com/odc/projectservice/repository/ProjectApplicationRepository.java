package com.odc.projectservice.repository;

import com.odc.projectservice.entity.ProjectApplication;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectApplicationRepository extends JpaRepository<ProjectApplication, UUID> {

    @Query("SELECT pa FROM ProjectApplication pa WHERE pa.project.id = :projectId")
    List<ProjectApplication> findByProjectId(UUID projectId);

    @Query("SELECT pa FROM ProjectApplication  pa WHERE pa.project.id = :projectId AND pa.userId = :userId")
    boolean existsByProjectIdAndUserId(UUID projectId, @NotNull(message = "userId không được bỏ trống") UUID userId);

    int countByProjectId(UUID projectId);
}
