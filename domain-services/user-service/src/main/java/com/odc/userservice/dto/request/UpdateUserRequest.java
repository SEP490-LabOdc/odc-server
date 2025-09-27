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
    @NotBlank(message = "Full name is required")
    private String fullName;

    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Invalid phone number format")
    private String phone;

    private Gender gender;

    private LocalDate birthDate;

    private String address;

    private String avatarUrl;

}
