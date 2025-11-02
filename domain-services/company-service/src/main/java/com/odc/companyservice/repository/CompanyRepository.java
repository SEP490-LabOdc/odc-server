package com.odc.companyservice.repository;

import com.odc.companyservice.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID>, JpaSpecificationExecutor<Company> { // Sửa ID sang UUID
    Optional<Company> findByEmail(String email);

    Optional<Company> findByTaxCode(String taxCode);

    Optional<Company> findByIdAndUserId(UUID id, UUID userId);
}