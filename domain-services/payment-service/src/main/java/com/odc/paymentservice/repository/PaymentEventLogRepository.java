package com.odc.paymentservice.repository;

import com.odc.paymentservice.entity.PaymentEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PaymentEventLogRepository extends JpaRepository<PaymentEventLog, UUID> {
}
