package com.odc.aiservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(
        basePackages = {
                "com.odc.common.exception",
                "com.odc.common.util",
                "com.odc.aiservice",
                "com.odc.commonlib"
        }
)
public class AiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }

}
