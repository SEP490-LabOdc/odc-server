package com.odc.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
                "spring.main.web-application-type=reactive",
                "eureka.client.enabled=false",
                "spring.cloud.config.enabled=false"
        }
)
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }

}
