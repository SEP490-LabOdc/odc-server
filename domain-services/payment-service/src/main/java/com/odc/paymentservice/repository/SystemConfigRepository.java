package com.odc.paymentservice.repository;

import com.odc.paymentservice.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, UUID>, JpaSpecificationExecutor<SystemConfig> {
    Optional<SystemConfig> findByName(String name);
}
