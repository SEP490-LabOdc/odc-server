package com.odc.paymentservice.repository;

import com.odc.paymentservice.entity.WithdrawalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, UUID> {
    Page<WithdrawalRequest> findByStatus(String status, Pageable pageable);

    @Query("""
                select wr from WithdrawalRequest wr
                where (:status is null or wr.status = :status)
                  and (cast(:from as timestamp) is null or wr.createdAt >= :from)
                  and (cast(:to as timestamp) is null or wr.createdAt <= :to)
                order by
                    case wr.status
                        when 'PENDING' then 1
                        when 'APPROVED' then 2
                        when 'COMPLETED' then 3
                        when 'REJECTED' then 4
                        else 5
                    end,
                    wr.createdAt desc
            """)
    Page<WithdrawalRequest> search(
            @Param("status") String status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    @Query("""
                select wr from WithdrawalRequest wr
                where wr.status = 'APPROVED'
                  and wr.scheduledAt <= :now
                order by wr.createdAt asc
            """)
    List<WithdrawalRequest> findBatchToProcess(
            @Param("now") LocalDate now,
            Pageable pageable
    );

}