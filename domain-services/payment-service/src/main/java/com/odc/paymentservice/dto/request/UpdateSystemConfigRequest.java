package com.odc.paymentservice.dto.request;

import lombok.Getter;

import java.util.Map;

@Getter
public class UpdateSystemConfigRequest {
    private String description;
    private Map<String, Object> properties;
}
