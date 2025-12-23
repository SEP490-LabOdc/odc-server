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

    //                 WHERE p.status = 'ON_GOING' <=> add this condition if need filter by project status
    @Query("""
                SELECT COUNT(DISTINCT pm.userId)
                FROM ProjectMember pm
                JOIN pm.project p
                  WHERE  p.status = 'ON_GOING'
                  AND pm.leftAt IS NULL
                  AND pm.roleInProject = 'TALENT'
                  AND pm.isDeleted = false
            """)
    Long countJoinedStudents();

    @Query(value = """
                SELECT COUNT(*)
                FROM (
                    SELECT pm.user_id, COUNT(p.id) AS active_projects
                    FROM project_members pm
                    JOIN projects p ON pm.project_id = p.id
                    WHERE pm.role_in_project = 'MENTOR'
                      AND pm.left_at IS NULL
                      AND pm.is_deleted = false
                      AND p.status = 'ON_GOING'
                      AND p.is_deleted = false
                    GROUP BY pm.user_id
                    HAVING COUNT(p.id) < 2
                ) t
            """, nativeQuery = true)
    Long countAvailableMentors();

}
