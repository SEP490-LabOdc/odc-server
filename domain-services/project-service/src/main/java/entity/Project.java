package entity;


import com.odc.common.entity.BaseEntity;
import jakarta.persistence.Column;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class Project  extends BaseEntity {
    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "mentor_id")
    private UUID mentorId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "budget")
    private BigDecimal budget;
}
