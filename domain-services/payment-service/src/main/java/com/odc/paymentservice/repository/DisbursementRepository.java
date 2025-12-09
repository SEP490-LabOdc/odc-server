package com.odc.paymentservice.repository;

import com.odc.paymentservice.entity.Disbursement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DisbursementRepository extends JpaRepository<Disbursement, UUID> {
    Optional<Disbursement> findByMilestoneId(UUID milestoneId);
}
