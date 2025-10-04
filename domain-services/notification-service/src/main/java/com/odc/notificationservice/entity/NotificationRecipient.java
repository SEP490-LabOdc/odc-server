package com.odc.notificationservice.entity;

import com.odc.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_recipient")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRecipient extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "read_status", nullable = false)
    private Boolean readStatus = false;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "delivered_via")
    private String deliveredVia; // EMAIL, FCM, WEBSOCKET

    @Column(name = "device_token")
    private String deviceToken; // optional, for push FCM
}

