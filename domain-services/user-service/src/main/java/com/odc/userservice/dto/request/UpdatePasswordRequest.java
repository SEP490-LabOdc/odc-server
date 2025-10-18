package com.odc.userservice.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePasswordRequest {
    @NotBlank(message = "Yêu cầu nhập mật khẩu hiện tại")
    private String currentPassword;

    @NotBlank(message = "Yêu cầu nhập mật khẩu mới")
    @Size(min = 8, message = "Mật khẩu mới phải có ít nhất 8 ký tự")
    private String newPassword;

    @NotBlank(message = "Yêu cầu xác nhận mật khẩu mới")
    private String confirmPassword;

    @AssertTrue(message = "Mật khẩu mới và xác nhận mật khẩu không khớp")
    public boolean isPasswordMatch() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}