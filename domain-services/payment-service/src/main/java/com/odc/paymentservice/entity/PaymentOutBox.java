package com.odc.paymentservice.entity;

import com.odc.common.entity.BaseOutbox;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "payment_outbox")
@Getter
@Setter
@AllArgsConstructor
@Builder
public class PaymentOutBox extends BaseOutbox {
}
