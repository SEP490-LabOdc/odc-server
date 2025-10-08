package com.odc.companyservice.entity;

import com.odc.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "company_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyDocument extends BaseEntity {

    @Column(name = "type", nullable = false)
    private String type; // e.g., "BUSINESS_LICENSE", "TAX_CERTIFICATE"

    @Column(name = "file_url", nullable = false)
    private String fileUrl; // URL to the document file

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    // Quan hệ N-1 với Company
    // `fetch = FetchType.LAZY`: chỉ tải thông tin Company khi thực sự cần thiết.
    @jakarta.persistence.ManyToOne
    @jakarta.persistence.JoinColumn(name = "company_id") // Tên cột khóa ngoại trong bảng `company_documents`
    private Company company;
}
