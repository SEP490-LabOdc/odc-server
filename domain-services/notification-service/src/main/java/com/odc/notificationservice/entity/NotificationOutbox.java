package com.odc.notificationservice.entity;

import com.odc.common.entity.BaseOutbox;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "notification_outbox")
@Getter
@Setter
@AllArgsConstructor
@Builder
public class NotificationOutbox extends BaseOutbox {
}
