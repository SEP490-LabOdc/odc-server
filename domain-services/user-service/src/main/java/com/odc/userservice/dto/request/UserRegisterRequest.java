package com.odc.userservice.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UserRegisterRequest {

    @NotBlank(message = "Yêu cầu nhập họ và tên")
    private String fullName;

    @NotBlank(message = "Yêu cầu nhập Email")
    @Email(message = "Định dạng email không hợp lệ")
    private String email;

    @NotBlank(message = "Yêu cầu nhập mật khẩu")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    private String password;

    @NotBlank(message = "Yêu cầu xác nhận mật khẩu")
    private String confirmPassword;

    @AssertTrue(message = "Mật khẩu và xác nhận mật khẩu không khớp")
    public boolean isPasswordMatch() {
        return password != null && password.equals(confirmPassword);
    }
}
