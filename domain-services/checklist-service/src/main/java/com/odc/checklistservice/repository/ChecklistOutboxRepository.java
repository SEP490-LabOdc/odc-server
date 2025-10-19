package com.odc.checklistservice.repository;

import com.odc.checklistservice.entity.ChecklistOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ChecklistOutboxRepository extends JpaRepository<ChecklistOutbox, UUID> {
}
