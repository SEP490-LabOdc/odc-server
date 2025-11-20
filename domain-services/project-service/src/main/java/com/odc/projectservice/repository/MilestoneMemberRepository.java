package com.odc.projectservice.repository;

import com.odc.projectservice.entity.MilestoneMember;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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
                                    @Param("projectMemberId") UUID projectMemberId);}
