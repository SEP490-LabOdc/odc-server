package com.odc.notificationservice.entity;

import com.odc.common.entity.BaseEventLog;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "notification_event_log")
@Getter
@Setter
@AllArgsConstructor
@Builder
public class NotificationEventLog extends BaseEventLog {
}