package com.odc.notificationservice.repository;

import com.odc.notificationservice.entity.NotificationEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationEventLogRepository extends JpaRepository<NotificationEventLog, UUID> {
}
