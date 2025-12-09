package com.odc.projectservice.repository;

import com.odc.projectservice.entity.MilestoneMember;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MilestoneMemberRepository extends JpaRepository<MilestoneMember, UUID> {

    boolean existsByProjectMilestone_IdAndProjectMember_IdAndIsActiveTrue(UUID milestoneId, UUID projectMemberId);

    List<MilestoneMember> findByProjectMilestone_IdAndIsActive(UUID milestoneId, boolean isActive);

    @Query("""
                SELECT mm FROM MilestoneMember mm
                WHERE mm.projectMilestone.id = :milestoneId
                  AND mm.projectMember.id IN :memberIds
            """)
    List<MilestoneMember> findByProjectMilestoneIdAndProjectMemberIds(
            @Param("milestoneId") UUID milestoneId,
            @Param("memberIds") List<UUID> memberIds
    );

    List<MilestoneMember> findByProjectMilestone_Id(UUID milestoneId);

    Optional<MilestoneMember> findByProjectMilestone_IdAndProjectMember_Id(UUID milestoneId, UUID projectMemberId);

    List<MilestoneMember> findByProjectMilestone_IdInAndIsActive(List<UUID> milestoneIds, boolean isActive);

    Optional<MilestoneMember> findByProjectMilestone_IdAndIdAndIsActive(UUID milestoneId, UUID milestoneMemberId, boolean isActive);

    boolean existsByProjectMilestone_IdAndIsLeaderTrue(UUID milestoneId);

    @Query("SELECT COUNT(mm) FROM MilestoneMember mm " +
            "WHERE mm.projectMember.id = :projectMemberId " +
            "AND mm.isActive = true " +
            "AND mm.projectMilestone.status IN :activeStatuses")
    long countActiveMilestonesForMember(@Param("projectMemberId") UUID projectMemberId,
                                        @Param("activeStatuses") List<String> activeStatuses);

    @Query("""
                SELECT mm FROM MilestoneMember mm
                JOIN FETCH mm.projectMember
                WHERE mm.projectMilestone.id = :milestoneId
            """)
    List<MilestoneMember> findByProjectMilestoneIdWithProjectMember(@Param("milestoneId") UUID milestoneId);

}
