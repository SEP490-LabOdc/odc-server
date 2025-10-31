package com.odc.userservice.dto.response;

import com.odc.common.constant.Gender;
import com.odc.common.constant.Status;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Builder
@Getter
@Setter
public class GetUserResponse {
    private UUID id;
    private String email, phone;
    private String fullName;
    private LocalDate birthDate;
    private String avatarUrl;
    private String role;
    private Gender gender;
    private String address;
    private Status status;
}
