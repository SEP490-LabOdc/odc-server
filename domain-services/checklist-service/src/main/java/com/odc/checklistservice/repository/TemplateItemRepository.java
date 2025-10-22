package com.odc.checklistservice.repository;

import com.odc.checklistservice.entity.TemplateItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TemplateItemRepository extends JpaRepository<TemplateItem, UUID> {

    @Query("SELECT ti.content FROM TemplateItem ti WHERE ti.id in :ids")
    List<String> getDescriptionsByIds(@Param("ids") List<UUID> ids);
}
