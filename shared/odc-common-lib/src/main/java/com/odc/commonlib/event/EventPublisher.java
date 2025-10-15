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

    public <T extends GeneratedMessageV3> void publish(String topic, T event) {
        byte[] payload = ProtobufConverter.serialize(event);
        kafkaTemplate.send(topic, payload);
    }
}
