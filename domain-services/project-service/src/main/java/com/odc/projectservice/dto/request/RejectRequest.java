package com.odc.projectservice.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RejectRequest {
    private String reviewNotes;
}
