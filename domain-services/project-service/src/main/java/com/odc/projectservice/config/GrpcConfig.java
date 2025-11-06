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

    @Value("${grpc.client.company-service.address:localhost}")
    private String companyServiceAddress;

    @Value("${grpc.client.company-service.port:9092}")
    private int companyServicePort;

    @Bean(name = "userServiceChannel")
    public ManagedChannel userServiceChannel() {
        return ManagedChannelBuilder
                .forAddress(userServiceAddress, userServicePort)
                .usePlaintext()
                .build();
    }

    @Bean
    public ManagedChannel companyServiceChannel() {
        return ManagedChannelBuilder
                .forAddress(companyServiceAddress, companyServicePort)
                .usePlaintext()
                .build();
    }
}