package com.odc.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "event_id")
    private String eventId;

    @Lob
    @Column(name = "payload", columnDefinition = "BYTEA")
    private byte[] payload;

    private Boolean processed;

    @CreatedDate
    @Column(name = "created_at")
    private Instant createdAt;
}
