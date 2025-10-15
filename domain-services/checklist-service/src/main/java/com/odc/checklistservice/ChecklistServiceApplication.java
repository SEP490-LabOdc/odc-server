package com.odc.checklistservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.odc.common",
        "com.odc.checklistservice",
        "com.odc.commonlib"
})
public class ChecklistServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChecklistServiceApplication.class, args);
    }

}
