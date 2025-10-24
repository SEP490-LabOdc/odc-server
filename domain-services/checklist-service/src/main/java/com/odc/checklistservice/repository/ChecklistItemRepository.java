package com.odc.checklistservice.repository;

import com.odc.checklistservice.entity.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, UUID> {

    @Query("select ci from ChecklistItem ci where ci.checklist.templateId = :templateId and ci.checklist.entityId = :entityId")
    List<ChecklistItem> getChecklistItemsByTemplateTypeAndEntityId(UUID templateId, String entityId);
}
