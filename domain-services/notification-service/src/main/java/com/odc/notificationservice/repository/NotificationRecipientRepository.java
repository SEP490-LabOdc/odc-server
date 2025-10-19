package com.odc.notificationservice.repository;

import com.odc.notificationservice.entity.NotificationRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, UUID> {
    @Query("SELECT nr FROM NotificationRecipient nr WHERE nr.userId = :userId AND (:readStatus IS NULL OR nr.readStatus = :readStatus)")
    List<NotificationRecipient> findAllNotificationsByUserIdAndReadStatus(@Param("userId") UUID userId, @Param("readStatus") Boolean readStatus);

    Optional<NotificationRecipient> findByIdAndReadStatus(UUID id, Boolean readStatus);

    List<NotificationRecipient> findAllByUserIdAndReadStatus(UUID userId, Boolean readStatus);
}
