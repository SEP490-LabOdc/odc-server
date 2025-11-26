package com.odc.projectservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ToggleMentorLeaderRequest {
    @NotNull(message = "isLeader không được bỏ trống")
    private Boolean isLeader;
}