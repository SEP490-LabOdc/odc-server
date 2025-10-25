package com.odc.projectservice.repository;

import com.odc.projectservice.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    boolean existsByCompanyIdAndTitle(UUID companyId, String title);

    boolean existsByCompanyIdAndTitleAndIdNot(UUID companyId, String title, UUID id);

}
