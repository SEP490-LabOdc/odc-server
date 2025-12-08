package com.odc.paymentservice.repository;

import com.odc.paymentservice.entity.WithdrawalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, UUID> {
    Page<WithdrawalRequest> findByStatus(String status, Pageable pageable);

    @Query("""
        select wr from WithdrawalRequest wr
        where (:status is null or wr.status = :status)
          and (:from is null or wr.createdAt >= :from)
          and (:to is null or wr.createdAt <= :to)
        """)
    Page<WithdrawalRequest> search(String status, LocalDateTime from, LocalDateTime to, Pageable pageable);
}