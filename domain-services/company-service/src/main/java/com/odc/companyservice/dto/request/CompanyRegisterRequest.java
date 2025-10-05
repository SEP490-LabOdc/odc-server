package com.odc.companyservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.UUID;

@Getter
public class CompanyRegisterRequest {
    @NotBlank(message = "Tên công ty không được để trống")
    private String name;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;

    @NotBlank(message = "Mã số thuế không được để trống")
    private String taxCode;

    private String address;
    private String description;
    private String website;
    private String domain;
    private String fileUrl;

    @NotNull(message = "User ID không được để trống")
    private UUID userId;

}
