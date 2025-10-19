package com.odc.userservice.entity;

import com.odc.common.entity.BaseOutbox;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "user_outbox")
@Getter
@Setter
@AllArgsConstructor
@Builder
public class UserOutbox extends BaseOutbox {
}