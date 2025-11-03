package com.odc.companyservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class GetCompanyEditResponse {
    private UUID id;
    private String companyName;
    private String companyEmail;
    private String companyPhone;
    private String taxCode;
    private String address;
    private String contactPersonName;
    private String contactPersonEmail;
    private String contactPersonPhone;
    private List<GetCompanyDocumentEditResponse> getCompanyDocumentEditResponses;

    @Data
    @Builder
    public static class GetCompanyDocumentEditResponse {
        private UUID id;
        private String fileName;
        private String fileUrl;
        private String type;
    }
}
