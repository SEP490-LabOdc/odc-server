package com.odc.userservice.dto.request;

import com.odc.common.constant.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateUserRequest {
    // ✅ Có thể update
    @NotBlank(message = "Yêu cầu nhập họ và tên")
    private String fullName;

    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    private Gender gender;

    private LocalDate birthDate;

    private String address;

    private String avatarUrl;

}
