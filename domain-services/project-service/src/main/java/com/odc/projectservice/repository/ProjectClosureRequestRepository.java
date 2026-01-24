package com.odc.projectservice.repository;

import com.odc.projectservice.entity.ProjectClosureRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectClosureRequestRepository extends JpaRepository<ProjectClosureRequest, UUID> {
    Optional<ProjectClosureRequest> findByIdAndProject_Id(UUID requestId, UUID projectId);

    List<ProjectClosureRequest> findByProject_IdOrderByCreatedAtDesc(UUID projectId);
}
