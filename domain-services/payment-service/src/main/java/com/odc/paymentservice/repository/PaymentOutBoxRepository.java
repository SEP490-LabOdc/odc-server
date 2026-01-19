package com.odc.paymentservice.repository;

import com.odc.paymentservice.entity.PaymentOutBox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentOutBoxRepository extends JpaRepository<PaymentOutBox, UUID> {
    List<PaymentOutBox> findTop50ByProcessedFalseOrderByCreatedAtAsc();
}
