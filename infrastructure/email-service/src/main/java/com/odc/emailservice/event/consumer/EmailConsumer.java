package com.odc.emailservice.event.consumer;

import com.odc.emailservice.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailConsumer {
    private final OtpService otpService;

    @KafkaListener(topics = "email.otp.company_verification", groupId = "email-service-group")
    public void consumeEmail(String email) {
        log.info("consume email received : {}", email);
        otpService.sendOtpRequest(email);
    }
}
