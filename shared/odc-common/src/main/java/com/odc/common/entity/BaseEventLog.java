package com.odc.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "source_service")
    private String sourceService;

    @Lob
    @Column(name = "payload", columnDefinition = "BYTEA")
    private byte[] payload;

    @CreatedDate
    @Column(name = "received_at")
    private Instant receivedAt;

    @LastModifiedDate
    @Column(name = "processed_at")
    private Instant processedAt;

}