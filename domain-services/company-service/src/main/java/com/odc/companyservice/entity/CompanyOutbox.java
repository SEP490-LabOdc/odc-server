package com.odc.companyservice.entity;

import com.odc.common.entity.BaseOutbox;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "company_outbox")
@Getter
@Setter
@AllArgsConstructor
@Builder
public class CompanyOutbox extends BaseOutbox {
}
