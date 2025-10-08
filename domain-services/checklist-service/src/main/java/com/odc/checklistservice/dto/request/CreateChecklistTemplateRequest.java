package com.odc.checklistservice.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class CreateChecklistTemplateRequest {
    private String name;
    private String description;
    private String entityType;
    private List<GroupRequest> groups;

    @Data
    @NoArgsConstructor
    public static class GroupRequest {
        private String title;
        private int displayOrder;
        private List<ItemRequest> items;
    }

    @Data
    @NoArgsConstructor
    public static class ItemRequest {
        private String content;
        private int displayOrder;
        private boolean isRequired;
    }
}
