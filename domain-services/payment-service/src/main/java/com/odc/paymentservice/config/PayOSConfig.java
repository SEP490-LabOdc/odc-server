package com.odc.paymentservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Configuration
public class PayOSConfig {

    @Value("${payos.client-id}")
    private String clientId;

    @Value("${payos.api-key}")
    private String apiKey;

    @Value("${payos.checksum-key}")
    private String checksumKey;

    @Bean
    public PayOS payOS() {
        return new PayOS(clientId, apiKey, checksumKey);
    }

    @Bean
    public PayOS payOSPayout(
            @Value("${payos-payout.client-id}") String clientId,
            @Value("${payos-payout.api-key}") String apiKey,
            @Value("${payos-payout.checksum-key}") String checksumKey
    ) {
        return new PayOS(clientId, apiKey, checksumKey);
    }
}