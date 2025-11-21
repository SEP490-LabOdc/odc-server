package com.odc.projectservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
public class UserSubmittedCvResponse {
    private String projectName;
    private LocalDateTime submittedAt;
    private String fileLink;
    private String fileName;
}
