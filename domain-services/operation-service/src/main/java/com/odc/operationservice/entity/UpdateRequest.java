package com.odc.operationservice.entity;

import com.odc.common.constant.UpdateRequestStatus;
import com.odc.common.entity.BaseEntity;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "update_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRequest extends BaseEntity {

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "code", length = 8, nullable = false, unique = true)
    private String code;

    @Column(name = "request_type", length = 100, nullable = false)
    private String requestType; // Define enum

    @Type(JsonBinaryType.class)
    @Column(name = "change_data", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> changeData;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private UpdateRequestStatus status;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reject_reason")
    private String rejectReason;
}
