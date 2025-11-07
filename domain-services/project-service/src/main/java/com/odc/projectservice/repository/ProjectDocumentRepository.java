package com.odc.projectservice.repository;

import com.odc.projectservice.entity.ProjectDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, UUID>, JpaSpecificationExecutor<ProjectDocument> {
    List<ProjectDocument> findByProjectId(UUID projectId);
}
