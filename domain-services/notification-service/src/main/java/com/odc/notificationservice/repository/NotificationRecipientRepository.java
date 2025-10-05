package com.odc.notificationservice.repository;

import com.odc.notificationservice.entity.NotificationRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, UUID> {
    Optional<NotificationRecipient> findByIdAndUserIdAndReadStatus(UUID id, UUID userId, Boolean readStatus);

    List<NotificationRecipient> findAllByUserIdAndReadStatus(UUID userId, Boolean readStatus);
}
