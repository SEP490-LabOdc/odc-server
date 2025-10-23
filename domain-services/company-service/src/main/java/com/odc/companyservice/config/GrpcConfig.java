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

    @Value("${grpc.client.user-service.address:localhost}")
    private String userServiceAddress;

    @Value("${grpc.client.user-service.port:9091}")
    private int userServicePort;

    @Bean
    public ManagedChannel userServiceChannel() {
        return ManagedChannelBuilder
                .forAddress(userServiceAddress, userServicePort)
                .usePlaintext()
                .build();
    }

    @Bean
    public ManagedChannel checklistServiceChannel() {
        return ManagedChannelBuilder
                .forAddress(checklistServiceAddress, checklistServicePort)
                .usePlaintext()
                .build();
    }
}
