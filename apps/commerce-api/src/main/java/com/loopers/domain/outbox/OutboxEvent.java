package com.loopers.domain.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.loopers.domain.BaseEntity;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "outbox_event")
public class OutboxEvent extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 100)
    private EventType eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "partition_key", length = 100)
    private String partitionKey;

    public OutboxEvent(EventType eventType, String payload) {
        this(eventType, payload, null);
    }

    public OutboxEvent(EventType eventType, String payload, String partitionKey) {
        this.eventType = eventType;
        this.payload = payload;
        this.partitionKey = partitionKey;
        this.status = OutboxStatus.INIT;
    }

    public void markSent() {
        this.status = OutboxStatus.SENT;
    }
}
