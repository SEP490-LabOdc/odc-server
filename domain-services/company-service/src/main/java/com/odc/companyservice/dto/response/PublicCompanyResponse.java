package com.odc.companyservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PublicCompanyResponse {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String description;
    private String logo;
    private String banner;
    private String website;
    private String domain;
    private LocalDateTime createdAt;
}