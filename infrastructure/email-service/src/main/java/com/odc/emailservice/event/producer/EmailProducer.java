package com.odc.emailservice.event.producer;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;



}
