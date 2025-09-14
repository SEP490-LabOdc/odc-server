package com.odc.userservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.userservice.dto.response.GetUserResponse;

import java.util.UUID;

public interface UserService {
    ApiResponse<GetUserResponse> getUserById(UUID id);
}
