package com.odc.companyservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CompanyResponse {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String taxCode;
    private String address;
    private String description;
    private String website;
    private String status;
    private String domain;
    private String contactPersonName;
    private String contactPersonEmail;
    private String contactPersonPhone;
    private LocalDateTime createdAt;
}
