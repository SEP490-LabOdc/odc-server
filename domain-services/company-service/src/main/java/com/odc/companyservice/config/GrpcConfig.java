package com.odc.companyservice.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfig {
    @Value("${grpc.client.checklist-service.address:localhost}")
    private String checklistServiceAddress;

    @Value("${grpc.client.checklist-service.port:9093}")
    private int checklistServicePort;

    @Bean
    public ManagedChannel checklistServiceChannel() {
        return ManagedChannelBuilder
                .forAddress(checklistServiceAddress, checklistServicePort)
                .usePlaintext()
                .build();
    }
}
