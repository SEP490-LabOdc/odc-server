package com.odc.userservice.entity;

import com.odc.common.entity.BaseEventLog;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_event_log")
@Getter
@Setter
@AllArgsConstructor
@Builder
public class UserEventLog extends BaseEventLog {
}