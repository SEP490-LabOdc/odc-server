package com.odc.companyservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.util.List;

@Getter
public class UpdateCompanyRegistrationRequest {

    @NotBlank(message = "Tên công ty không được để trống")
    private String name;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;

    @NotBlank(message = "Mã số thuế không được để trống")
    private String taxCode;

    @NotBlank(message = "Địa chỉ công ty không được để trống")
    private String address;

    // Contact person
    @NotBlank(message = "Tên của người liên lạc không được để trống")
    private String contactPersonName;

    @NotBlank(message = "Số điện thoại của người liên lạc không được để trống")
    private String contactPersonPhone;

    @NotBlank(message = "Email của người liên lạc không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String contactPersonEmail;

    private List<UpdateCompanyDocumentRequest> updateCompanyDocumentRequests;
}
