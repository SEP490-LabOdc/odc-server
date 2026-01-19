package com.odc.projectservice.repository;

import com.odc.projectservice.entity.ProjectOutBox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectOutBoxRepository extends JpaRepository<ProjectOutBox, UUID> {
    List<ProjectOutBox> findTop50ByProcessedFalseOrderByCreatedAtAsc();
}
