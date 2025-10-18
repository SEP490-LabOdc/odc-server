package com.odc.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UserLoginRequest {

    @NotBlank(message = "Yêu cầu nhập email")
    @Email(message = "Định dạng email không hợp lệ")
    private String email;

    @NotBlank(message = "Yêu cầu nhập mật khẩu")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    private String password;
}
