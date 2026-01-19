package com.odc.projectservice.scheduler;

import com.odc.commonlib.event.EventPublisher;
import com.odc.projectservice.entity.ProjectEventLog;
import com.odc.projectservice.entity.ProjectOutBox;
import com.odc.projectservice.repository.ProjectEventLogRepository;
import com.odc.projectservice.repository.ProjectOutBoxRepository;
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
public class ProjectOutBoxScheduler {

    private final ProjectOutBoxRepository outboxRepository;
    private final ProjectEventLogRepository eventLogRepository;
    private final EventPublisher eventPublisher;

    /**
     * Publish events every 5 seconds
     */
    @Scheduled(fixedDelay = 5000)
//    @Transactional
    public void publishOutboxEvents() {

        List<ProjectOutBox> events =
                outboxRepository.findTop50ByProcessedFalseOrderByCreatedAtAsc();

        if (events.isEmpty()) {
            return;
        }

        log.info("Found {} outbox events to publish", events.size());

        for (ProjectOutBox outbox : events) {
            try {
                eventPublisher.publish(outbox.getEventType(), outbox.getPayload());

                outbox.setProcessed(true);

                ProjectEventLog eventLog = new ProjectEventLog();
                eventLog.setEventType(outbox.getEventType());
                eventLog.setEventId(UUID.fromString(outbox.getEventId()));
                eventLog.setSourceService("operation-service");
                eventLog.setPayload(outbox.getPayload());
                eventLog.setReceivedAt(Instant.now());
                eventLogRepository.save(
                        ProjectEventLog.builder()
                                .build()
                );

            } catch (Exception ex) {
                log.error("Failed to publish event id={}", outbox.getEventId(), ex);
            }
        }
    }
}
