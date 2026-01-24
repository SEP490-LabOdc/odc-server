package com.odc.projectservice.dto.request;

import lombok.Getter;

@Getter
public class ReviewClosureRequest {
    private boolean approved;
    private String comment;
}
