package com.odc.projectservice.config;

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

    @Bean(name = "userServiceChannel")
    public ManagedChannel userServiceChannel() {
        return ManagedChannelBuilder
                .forAddress(userServiceAddress, userServicePort)
                .usePlaintext()
                .build();
    }
}