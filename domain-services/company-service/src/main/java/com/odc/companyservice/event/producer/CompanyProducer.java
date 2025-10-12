package com.odc.companyservice.event.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CompanyProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendEmailEvent(String email) {
        log.info("send email event : {}", email);
        kafkaTemplate.send("email.otp.company_verification", email);
    }
}
