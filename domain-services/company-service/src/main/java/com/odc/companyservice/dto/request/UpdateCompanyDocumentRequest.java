package com.odc.companyservice.dto.request;

import lombok.Getter;

import java.util.UUID;

@Getter
public class UpdateCompanyDocumentRequest {
    private UUID id;
    private String fileName, fileUrl, type;
}
