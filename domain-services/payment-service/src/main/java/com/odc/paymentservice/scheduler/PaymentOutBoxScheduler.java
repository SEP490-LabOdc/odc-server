package com.odc.paymentservice.scheduler;

import com.odc.commonlib.event.EventPublisher;
import com.odc.paymentservice.entity.PaymentEventLog;
import com.odc.paymentservice.entity.PaymentOutBox;
import com.odc.paymentservice.repository.PaymentEventLogRepository;
import com.odc.paymentservice.repository.PaymentOutBoxRepository;
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
public class PaymentOutBoxScheduler {

    private final PaymentOutBoxRepository outboxRepository;
    private final PaymentEventLogRepository eventLogRepository;
    private final EventPublisher eventPublisher;

    /**
     * Publish events every 5 seconds
     */
    @Scheduled(fixedDelay = 5000)
//    @Transactional
    public void publishOutboxEvents() {

        List<PaymentOutBox> events =
                outboxRepository.findTop50ByProcessedFalseOrderByCreatedAtAsc();

        if (events.isEmpty()) {
            return;
        }

        log.info("Found {} outbox events to publish", events.size());

        for (PaymentOutBox outbox : events) {
            try {
                eventPublisher.publish(outbox.getEventType(), outbox.getPayload());

                outbox.setProcessed(true);

                PaymentEventLog eventLog = new PaymentEventLog();
                eventLog.setEventType(outbox.getEventType());
                eventLog.setEventId(UUID.fromString(outbox.getEventId()));
                eventLog.setPayload(outbox.getPayload());
                eventLog.setReceivedAt(Instant.now());
                eventLogRepository.save(eventLog);

            } catch (Exception ex) {
                log.error("Failed to publish event id={}", outbox.getEventId(), ex);
            }
        }
    }
}
