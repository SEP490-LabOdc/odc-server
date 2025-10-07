package com.odc.notificationservice.entity;

import com.odc.common.entity.BaseEntity;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Column(name = "type", length = 50)
    private String type;

    @Column(name = "title")
    private String title;

    @Column(name = "content")
    private String content;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> data;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "prority", length = 50)
    private String priority;

    @Column(name = "deep_link")
    private String deepLink;

    @Column(name = "sent_at")
    private Instant sentAt;

    @OneToMany(mappedBy = "notification")
    private Set<NotificationRecipient> recipients;
}
