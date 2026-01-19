package com.odc.commonlib.event;

import com.google.protobuf.GeneratedMessageV3;
import com.odc.commonlib.util.ProtobufConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {
    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    public EventPublisher(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publish protobuf event
     */
    public <T extends GeneratedMessageV3> void publish(String topic, T event) {
        byte[] payload = ProtobufConverter.serialize(event);
        kafkaTemplate.send(topic, payload);
    }

    /**
     * Publish raw payload (for Outbox Scheduler)
     */
    public void publish(String topic, byte[] payload) {
        kafkaTemplate.send(topic, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error(
                                "[KAFKA_PUBLISH_FAILED] topic={}, error={}",
                                topic,
                                ex.getMessage(),
                                ex
                        );
                    } else {
                        log.info(
                                "[KAFKA_PUBLISHED] topic={}, partition={}, offset={}",
                                topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset()
                        );
                    }
                });
    }
}
