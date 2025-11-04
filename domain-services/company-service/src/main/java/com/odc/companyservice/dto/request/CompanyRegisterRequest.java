package com.odc.companyservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.DefaultValue;
import lombok.Getter;

@Getter
public class CompanyRegisterRequest {

    // Company information
    @NotBlank(message = "Tên công ty không được để trống")
    private String name;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;

    @NotBlank(message = "Mã số thuế không được để trống")
    private String taxCode;

    @NotBlank(message = "Địa chỉ công ty không được để trống")
    private String address;

    @DefaultValue("")
    private String businessLicenseLink;
    private String businessLicenseFileName;

    // Contact person
    @NotBlank(message = "Tên của người liên lạc không được để trống")
    private String contactPersonName;

    @NotBlank(message = "Số điện thoại của người liên lạc không được để trống")
    private String contactPersonPhone;

    @NotBlank(message = "Email của người liên lạc không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String contactPersonEmail;
}
