package com.odc.paymentservice.entity;

import com.odc.common.entity.BaseEntity;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.Map;

@Entity
@Table(name = "system_configs")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SystemConfig extends BaseEntity {
    @Type(JsonBinaryType.class)
    @Column(name = "properties", columnDefinition = "jsonb")
    private Map<String, String> properties;
    private String description;
}