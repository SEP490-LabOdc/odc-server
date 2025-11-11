package com.odc.userservice.config;

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

    @Value("${grpc.server.port:9091}")
    private int userServiceGrpcPort;

    @Bean(name = "projectServiceChannel")
    public ManagedChannel projectServiceChannel() {
        return ManagedChannelBuilder
                .forAddress(projectServiceAddress, projectServicePort)
                .usePlaintext()
                .build();
    }

    @Bean(name = "userServiceGrpcChannel")
    public ManagedChannel userServiceGrpcChannel() {
        return ManagedChannelBuilder
                .forAddress("localhost", userServiceGrpcPort)
                .usePlaintext()
                .build();
    }
}