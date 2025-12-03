package com.odc.projectservice.repository;

import com.odc.projectservice.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {
    // Hộp thư đến: Tìm report gửi cho tôi
    Page<Report> findByRecipientId(UUID recipientId, Pageable pageable);

    // Hộp thư đi: Tìm report do tôi tạo
    Page<Report> findByReporterId(UUID reporterId, Pageable pageable);

    // Admin/Audit: Tìm report theo dự án
    Page<Report> findByProjectId(UUID projectId, Pageable pageable);

    // Admin: Tìm report gửi lên hệ thống (recipientId = null)
    Page<Report> findByRecipientIdIsNull(Pageable pageable);

    Page<Report> findByMilestone_Id(UUID milestoneId, Pageable pageable);
}