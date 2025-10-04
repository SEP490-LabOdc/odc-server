package com.odc.notificationservice.repository;

import com.odc.notificationservice.entity.NotificationRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, UUID> {
}
