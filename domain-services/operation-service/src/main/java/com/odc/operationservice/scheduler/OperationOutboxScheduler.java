package com.odc.operationservice.scheduler;

import com.odc.commonlib.event.EventPublisher;
import com.odc.operationservice.entity.OperationEventLog;
import com.odc.operationservice.entity.OperationOutBox;
import com.odc.operationservice.repository.OperationEventLogRepository;
import com.odc.operationservice.repository.OperationOutBoxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OperationOutboxScheduler {

    private final OperationOutBoxRepository outboxRepository;
    private final OperationEventLogRepository eventLogRepository;
    private final EventPublisher eventPublisher;

    /**
     * Publish events every 5 seconds
     */
    @Scheduled(fixedDelay = 5000)
//    @Transactional
    public void publishOutboxEvents() {

        List<OperationOutBox> events =
                outboxRepository.findTop50ByProcessedFalseOrderByCreatedAtAsc();

        if (events.isEmpty()) {
            return;
        }

        log.info("Found {} outbox events to publish", events.size());

        for (OperationOutBox outbox : events) {
            try {
                eventPublisher.publish(outbox.getEventType(), outbox.getPayload());

                outbox.setProcessed(true);

                OperationEventLog eventLog = new OperationEventLog();
                eventLog.setEventType(outbox.getEventType());
                eventLog.setEventId(UUID.fromString(outbox.getEventId()));
                eventLog.setSourceService("operation-service");
                eventLog.setPayload(outbox.getPayload());
                eventLog.setReceivedAt(Instant.now());
                eventLogRepository.save(
                        OperationEventLog.builder()
                                .build()
                );

            } catch (Exception ex) {
                log.error("Failed to publish event id={}", outbox.getEventId(), ex);
            }
        }
    }
}

