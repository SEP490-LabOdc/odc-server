package com.odc.paymentservice.repository;

import com.odc.paymentservice.entity.Transaction;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Page<Transaction> findByWalletId(UUID walletId, Pageable pageable);

    @Query("""
        SELECT t FROM Transaction t
        JOIN PaymentRequest pr ON t.refId = pr.id
        WHERE pr.projectId = :projectId
        AND t.refType = 'PAYMENT_REQUEST'
        AND (t.isDeleted = false OR t.isDeleted IS NULL)
        ORDER BY t.createdAt DESC
        """)
    Page<Transaction> findByProjectId(
            @Param("projectId") UUID projectId,
            Pageable pageable
    );

    // Lấy tất cả transactions (global)
    @Query("""
        SELECT t FROM Transaction t
        WHERE (t.isDeleted = false OR t.isDeleted IS NULL)
        ORDER BY t.createdAt DESC
        """)
    Page<Transaction> findAllNotDeleted(Pageable pageable);

    // Lấy transaction detail với thông tin PaymentRequest
    @Query("""
        SELECT t FROM Transaction t
        LEFT JOIN FETCH PaymentRequest pr ON t.refId = pr.id AND t.refType = 'PAYMENT_REQUEST'
        WHERE t.id = :transactionId
        AND (t.isDeleted = false OR t.isDeleted IS NULL)
        """)
    Optional<Transaction> findByIdWithPaymentRequest(@Param("transactionId") UUID transactionId);
}