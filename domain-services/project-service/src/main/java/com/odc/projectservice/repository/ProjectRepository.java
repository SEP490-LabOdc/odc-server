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

    List<Project> findByCompanyIdAndIsOpenForApplicationsOrderByUpdatedAtDesc(UUID companyId, Boolean isOpenForApplications);

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

    @Query(value = """
                WITH months AS (
                    SELECT TO_CHAR(
                        date_trunc('month', CURRENT_DATE) - INTERVAL '5 month'
                        + (INTERVAL '1 month' * gs),
                        'YYYY-MM'
                    ) AS month
                    FROM generate_series(0, 5) gs
                )
                SELECT 
                    m.month,
                    COALESCE(COUNT(p.id), 0) AS total
                FROM months m
                LEFT JOIN projects p
                    ON TO_CHAR(p.created_at, 'YYYY-MM') = m.month
                    AND p.is_deleted = false
                GROUP BY m.month
                ORDER BY m.month
            """, nativeQuery = true)
    List<Object[]> countNewProjectsLast6Months();

    @Query("""
                SELECT COUNT(p)
                FROM Project p
                WHERE p.status = :status
                  AND p.isDeleted = false
            """)
    Long countByStatus(@Param("status") String status);

    @Query("""
                SELECT COUNT(p)
                FROM Project p
                WHERE p.isOpenForApplications = true
                  AND p.isDeleted = false
            """)
    Long countRecruitingProjects();

}
