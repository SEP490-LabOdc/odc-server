package com.odc.projectservice.repository;

import com.odc.projectservice.entity.ProjectApplication;
import io.lettuce.core.dynamic.annotation.Param;
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

    @Query("SELECT pa FROM ProjectApplication pa " +
            "JOIN pa.project p " +
            "WHERE pa.userId = :userId " +
            "AND (pa.isDeleted = false OR pa.isDeleted IS NULL) " +
            "AND (p.isDeleted = false OR p.isDeleted IS NULL)")
    List<ProjectApplication> findByUserIdAndNotDeleted(@Param("userId") UUID userId);
}
