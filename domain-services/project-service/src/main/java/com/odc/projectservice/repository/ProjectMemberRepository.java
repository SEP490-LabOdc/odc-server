package com.odc.projectservice.repository;

import com.odc.projectservice.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {
    List<ProjectMember> findByProjectId(UUID projectId);

    @Query("SELECT COUNT(DISTINCT pm.project.id) FROM ProjectMember pm " +
            "WHERE pm.userId = :userId AND UPPER(pm.roleInProject) = 'MENTOR'")
    Long countProjectsByMentorId(@Param("userId") UUID userId);

    @Query("SELECT DISTINCT pm.userId FROM ProjectMember pm WHERE UPPER(pm.roleInProject) = 'MENTOR'")
    List<UUID> findAllMentorUserIds();
}
