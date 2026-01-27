package com.odc.paymentservice.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCronRequest {
    private String cronExpression;
}
