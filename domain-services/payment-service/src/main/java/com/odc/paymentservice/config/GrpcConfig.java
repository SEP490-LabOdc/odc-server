package com.odc.paymentservice.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfig {

    @Value("${grpc.client.project-service.address:localhost}")
    private String projectServiceAddress;

    @Value("${grpc.client.project-service.port:9094}")
    private int projectServicePort;

    @Bean
    public ManagedChannel projectServiceChannel() {
        return ManagedChannelBuilder
                .forAddress(projectServiceAddress, projectServicePort)
                .usePlaintext()
                .build();
    }
}