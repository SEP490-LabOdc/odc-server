package com.odc.projectservice.repository;

import com.odc.projectservice.entity.MilestoneExtensionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MilestoneExtensionRequestRepository extends JpaRepository<MilestoneExtensionRequest, UUID> {
}
