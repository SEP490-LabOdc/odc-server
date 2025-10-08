package com.odc.checklistservice.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class GetChecklistTemplateResponse {
    private UUID id;
    private String name;
    private String description;
    private String entityType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<GroupResponse> groups;

    @Data
    @NoArgsConstructor
    public static class GroupResponse {
        private UUID id;
        private String title;
        private int displayOrder;
        private List<ItemResponse> items;
    }

    @Data
    @NoArgsConstructor
    public static class ItemResponse {
        private UUID id;
        private String content;
        private int displayOrder;
        private boolean isRequired;
    }
}