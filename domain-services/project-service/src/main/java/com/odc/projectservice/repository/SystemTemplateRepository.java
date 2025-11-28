package com.odc.projectservice.repository;

import com.odc.projectservice.entity.SystemTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemTemplateRepository extends JpaRepository<SystemTemplate, UUID>, JpaSpecificationExecutor<SystemTemplate> {
    boolean existsByName(String name);

    Optional<SystemTemplate> findFirstByTypeAndIsDeletedFalseOrderByUpdatedAtDesc(String type);
}
