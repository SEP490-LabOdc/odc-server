package entity;

import com.odc.common.entity.BaseEntity;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report extends BaseEntity {
    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;

    @Column(name = "recipient_id")
    private UUID recipientId;

    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType;

    @Column(name = "reporting_date", nullable = false)
    private LocalDate reportingDate;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Type(JsonBinaryType.class)
    @Column(name = "attachments_url", columnDefinition = "jsonb")
    private List<String> attachmentsUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_report_id")
    private Report parentReport;
}
