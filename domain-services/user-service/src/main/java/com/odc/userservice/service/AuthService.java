package com.odc.userservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.userservice.dto.request.UserRegisterRequest;
import com.odc.userservice.dto.response.UserRegisterResponse;

public interface AuthService {
    ApiResponse<UserRegisterResponse> register(UserRegisterRequest request);
}
