package com.odc.projectservice.repository;

import com.odc.projectservice.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID>, JpaSpecificationExecutor<Project> {
    boolean existsByCompanyIdAndTitle(UUID companyId, String title);

    boolean existsByCompanyIdAndTitleAndIdNot(UUID companyId, String title, UUID id);
    
    List<Project> findByCompanyId(UUID companyId);
}
