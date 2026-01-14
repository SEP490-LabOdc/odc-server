package com.odc.commonlib.event;

import com.google.protobuf.GeneratedMessageV3;
import com.odc.commonlib.util.ProtobufConverter;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

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
//                        log.error("Failed to publish event to topic={}", topic, ex);
                    } else {
//                        log.info(
//                                "Published event to topic={}, offset={}",
//                                topic,
//                                result.getRecordMetadata().offset()
//                        );
                    }
                });
    }
}
