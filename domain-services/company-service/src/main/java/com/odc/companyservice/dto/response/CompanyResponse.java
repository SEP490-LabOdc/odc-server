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
    private String website;
    private String status;
    private String domain;
    private String fileUrl;
    private LocalDateTime createdAt;

    private UserInfor user;

    @Data
    @Builder
    public static class UserInfor {
        private UUID id;
        private String fullName;
        private String email;
        private String phone;
    }
}
