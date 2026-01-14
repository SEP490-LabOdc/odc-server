package com.odc.operationservice.repository;

import com.odc.operationservice.entity.OperationEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OperationEventLogRepository extends JpaRepository<OperationEventLog, UUID> {
}
