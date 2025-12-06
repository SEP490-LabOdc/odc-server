package com.odc.projectservice.repository;

import com.odc.projectservice.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {
    List<ProjectMember> findByProjectId(UUID projectId);

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.roleInProject = 'MENTOR' AND pm.userId IN :userIds")
    List<ProjectMember> findMentorMembersByUserIds(List<UUID> userIds);

    long countByUserId(UUID userId);

    boolean existsByUserIdAndProjectId(UUID userId, UUID projectId);

    @Query("SELECT COUNT(pm) FROM ProjectMember pm WHERE pm.project.id = :projectId AND pm.roleInProject = :role")
    long countMentorsInProject(UUID projectId, String role);

    List<ProjectMember> findByUserId(UUID userId);

    List<ProjectMember> findByIdIn(List<UUID> projectMemberIds);

    boolean existsByProject_IdAndUserId(UUID id, UUID userId);

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.project.id = :projectId AND pm.userId = :userId AND pm.roleInProject = :role")
    ProjectMember findByProjectIdAndUserIdAndRole(UUID projectId, UUID userId, String role);

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.project.id = :projectId AND pm.roleInProject = :role")
    List<ProjectMember> findByProjectIdAndRole(UUID projectId, String role);

    ProjectMember findByProject_IdAndUserId(UUID id, UUID userId);

    @Query("SELECT pm.userId FROM ProjectMember pm WHERE pm.project.id = :projectId AND pm.userId IN :userIds")
    List<UUID> findUserIdsByProjectIdAndUserIdIn(@Param("projectId") UUID projectId, @Param("userIds") List<UUID> userIds);
}
