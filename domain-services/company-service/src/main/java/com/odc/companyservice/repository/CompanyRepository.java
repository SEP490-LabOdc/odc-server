package com.odc.companyservice.repository;

import com.odc.companyservice.entity.Company;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID>, JpaSpecificationExecutor<Company> { // Sửa ID sang UUID
    Optional<Company> findByEmail(String email);

    Optional<Company> findByTaxCode(String taxCode);

    Optional<Company> findByIdAndUserId(UUID id, UUID userId);

    Optional<Object> findByPhone(@NotBlank(message = "Số điện thoại không được để trống") String phone);

    Optional<Company> findByUserId(UUID userId);

    @Query(value = """
                WITH months AS (
                    SELECT TO_CHAR(
                        date_trunc('month', CURRENT_DATE) - INTERVAL '5 month' + (INTERVAL '1 month' * gs),
                        'YYYY-MM'
                    ) AS month
                    FROM generate_series(0, 5) gs
                )
                SELECT 
                    m.month,
                    COALESCE(COUNT(c.id), 0) AS total
                FROM months m
                LEFT JOIN companies c
                    ON TO_CHAR(c.created_at, 'YYYY-MM') = m.month
                    AND c.is_deleted = false
                GROUP BY m.month
                ORDER BY m.month
            """, nativeQuery = true)
    List<Object[]> countNewCompaniesLast6Months();
}