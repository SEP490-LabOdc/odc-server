package com.odc.projectservice.repository;

import com.odc.projectservice.entity.MilestoneFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MilestoneFeedbackRepository extends JpaRepository<MilestoneFeedback, UUID> {
    Page<MilestoneFeedback> findByMilestoneId(
            UUID milestoneId,
            Pageable pageable
    );
}
