package com.odc.projectservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class SetMentorLeaderRequest {
    @NotNull(message = "projectId không được bỏ trống")
    private UUID projectId;

    @NotNull(message = "mentorId không được bỏ trống")
    private UUID mentorId;
}