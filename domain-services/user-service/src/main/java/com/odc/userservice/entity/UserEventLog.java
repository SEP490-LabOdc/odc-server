package com.odc.userservice.entity;

import com.odc.common.entity.BaseEventLog;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "user_event_log")
@Getter
@Setter
@AllArgsConstructor
@Builder
public class UserEventLog extends BaseEventLog {
}