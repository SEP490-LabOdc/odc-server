package com.odc.companyservice.repository;

import com.odc.companyservice.entity.CompanyEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CompanyEventLogRepository extends JpaRepository<CompanyEventLog, UUID> {
}
