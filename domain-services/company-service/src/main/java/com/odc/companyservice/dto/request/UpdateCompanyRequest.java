package com.odc.companyservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateCompanyRequest {

    @NotBlank(message = "Tên công ty không được để trống")
    private String name;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;
    private String address;
    private String description;
    private String website;
    private String logo;
    private String banner;
    private String domain;
}
