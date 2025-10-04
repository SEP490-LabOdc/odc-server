package com.odc.notificationservice.entity;

import com.odc.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "notification_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Column(name = "recipient_id")
    private UUID recipientId; // optional, trỏ tới NotificationRecipient.id

    @Column(name = "status")
    private String status; // SENT, FAILED, PENDING

    @Column(name = "sent_via")
    private String sentVia; // EMAIL, FCM, WEBSOCKET

    @Column(name = "response_message", columnDefinition = "TEXT")
    private String responseMessage; // lưu log phản hồi từ FCM hoặc SMTP
}

