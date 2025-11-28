package com.odc.projectservice.repository;

import com.odc.projectservice.entity.Project;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID>, JpaSpecificationExecutor<Project> {
    boolean existsByCompanyIdAndTitle(UUID companyId, String title);

    boolean existsByCompanyIdAndTitleAndIdNot(UUID companyId, String title, UUID id);

    List<Project> findByCompanyId(UUID companyId);

    Page<Project> findByStatus(String status, Pageable pageable);

    Page<Project> findByIsOpenForApplications(Boolean isOpenForApplications, Pageable pageable);

    List<Project> findByCompanyIdOrderByUpdatedAtDesc(UUID companyId);

    @Query("SELECT DISTINCT p FROM Project p " +
            "JOIN p.skills s " +
            "WHERE s.id IN :skillIds " +
            "AND p.id != :excludeProjectId " +
            "AND (p.status = 'ON_GOING' OR p.status = 'PLANNING' OR p.status = 'PENDING') " + // Lọc status phù hợp hiển thị
            "AND (p.isDeleted = false OR p.isDeleted IS NULL)")
    Page<Project> findRelatedProjectsBySkills(
            @Param("skillIds") Set<UUID> skillIds,
            @Param("excludeProjectId") UUID excludeProjectId,
            Pageable pageable
    );
}
