package com.odc.companyservice.entity;

import com.odc.common.entity.BaseEventLog;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "company_event_log")
@Getter
@Setter
@AllArgsConstructor
@Builder
public class CompanyEventLog extends BaseEventLog {
}