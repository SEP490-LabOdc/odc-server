package com.odc.projectservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MilestoneAttachment {
    private UUID id;
    private String name;
    private String fileName;
    private String url;
}
