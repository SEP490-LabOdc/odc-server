package com.odc.checklistservice.repository;

import com.odc.checklistservice.entity.ChecklistTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ChecklistTemplateRepository extends JpaRepository<ChecklistTemplate, UUID>, JpaSpecificationExecutor<ChecklistTemplate> {

    @Query("SELECT clt.id FROM ChecklistTemplate clt WHERE clt.entityType = :entityType")
    UUID findIdByEntityTypeById(String entityType);
}
