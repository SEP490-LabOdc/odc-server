package com.odc.userservice.dto.request;

import com.odc.common.constant.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateUserRequest {
    @NotBlank(message = "Yêu cầu nhập họ và tên")
    private String fullName;

    @NotBlank(message = "Yêu cầu nhập Email")
    @Email(message = "Định dạng email không hợp lệ")
    private String email;

    @NotBlank(message = "Yêu cầu nhập mật khẩu")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    private String password;

    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    private Gender gender;

    private LocalDate birthDate;

    private String address;

    private String avatarUrl;

    // Role sẽ được mặc định là USER khi tạo user mới
    // Không cần roleId field nữa
}
