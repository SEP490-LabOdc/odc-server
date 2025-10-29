package com.odc.companyservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class GetCompanyChecklistResponse {
    private List<GetChecklistResponse> checklists;

    @Data
    @Builder
    public static class GetChecklistResponse {
        private UUID id;
        private boolean isChecked;
    }
}


