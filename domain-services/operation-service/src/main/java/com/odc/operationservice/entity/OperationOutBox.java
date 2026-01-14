package com.odc.operationservice.entity;

import com.odc.common.entity.BaseOutbox;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "operation_outbox")
@Getter
@Setter
@AllArgsConstructor
@Builder
public class OperationOutBox extends BaseOutbox {
}
