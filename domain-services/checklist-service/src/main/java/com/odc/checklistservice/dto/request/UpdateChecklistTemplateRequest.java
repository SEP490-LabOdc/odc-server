package com.odc.checklistservice.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class UpdateChecklistTemplateRequest {
    private String name;
    private String description;
    private List<GroupRequest> groups;

    @Data
    @NoArgsConstructor
    public static class GroupRequest {
        private UUID id;
        private String title;
        private int displayOrder;
        private List<ItemRequest> items;
    }

    @Data
    @NoArgsConstructor
    public static class ItemRequest {
        private UUID id;
        private String content;
        private int displayOrder;
        private boolean isRequired;
        private Boolean isDeleted;
    }
}
