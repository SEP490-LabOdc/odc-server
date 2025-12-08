package com.odc.paymentservice.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfig {

    @Value("${grpc.client.user-service.address:localhost}")
    private String userServiceAddress;

    @Value("${grpc.client.user-service.port:9091}")
    private int userServicePort;

    @Value("${grpc.client.project-service.address:localhost}")
    private String projectServiceAddress;

    @Value("${grpc.client.project-service.port:9094}")
    private int projectServicePort;

    @Bean(name = "userServiceChannel1")
    public ManagedChannel userServiceChannel() {
        return ManagedChannelBuilder
                .forAddress(userServiceAddress, userServicePort)
                .usePlaintext()
                .build();
    }

    @Bean(name = "projectServiceChannel")
    public ManagedChannel projectServiceChannel() {
        return ManagedChannelBuilder
                .forAddress(projectServiceAddress, projectServicePort)
                .usePlaintext()
                .build();
    }
}