package com.odc.userservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Builder
@Getter
@Setter
public class UserRegisterResponse {
    private UUID id;
    private String email;
}
