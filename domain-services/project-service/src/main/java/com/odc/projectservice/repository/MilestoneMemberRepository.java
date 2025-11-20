package com.odc.projectservice.repository;

import com.odc.projectservice.entity.MilestoneMember;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MilestoneMemberRepository extends JpaRepository<MilestoneMember, UUID> {
    @Query("""
                SELECT CASE WHEN COUNT(mm) > 0 THEN TRUE ELSE FALSE END
                FROM MilestoneMember mm
                WHERE mm.projectMilestone.id = :milestoneId
                  AND mm.projectMember.id = :projectMemberId
            """)
    boolean existsMemberInMilestone(@Param("milestoneId") UUID milestoneId,
                                    @Param("projectMemberId") UUID projectMemberId);

    @Query("""
                SELECT mm FROM MilestoneMember mm
                WHERE mm.projectMilestone.id = :milestoneId
                  AND mm.projectMember.id IN :memberIds
            """)
    List<MilestoneMember> findByProjectMilestoneIdAndProjectMemberIds(
            @Param("milestoneId") UUID milestoneId,
            @Param("memberIds") List<UUID> memberIds
    );

    @Query("""
                SELECT mm FROM MilestoneMember mm
                JOIN FETCH mm.projectMember pm
                WHERE mm.projectMilestone.project.id = :projectId
                  AND mm.projectMilestone.status = 'IN_PROGRESS'
                  AND pm.roleInProject = 'TALENT'
                  AND mm.leftAt IS NULL
            """)
    List<MilestoneMember> findActiveTalentsByProjectId(@Param("projectId") UUID projectId);

    List<MilestoneMember> findByProjectMilestone_IdAndProjectMember_RoleInProjectAndLeftAtIsNull(UUID milestoneId, String talent);

    List<MilestoneMember> findByProjectMilestone_IdAndLeftAtIsNull(UUID milestoneId);

    List<MilestoneMember> findByProjectMilestone_Id(UUID milestoneId);
}
