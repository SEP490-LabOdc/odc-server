package com.odc.userservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.userservice.dto.request.GoogleLoginRequest;
import com.odc.userservice.dto.request.RefreshTokenRequest;
import com.odc.userservice.dto.request.UserLoginRequest;
import com.odc.userservice.dto.request.UserRegisterRequest;
import com.odc.userservice.dto.response.RefreshTokenResponse;
import com.odc.userservice.dto.response.UserLoginResponse;
import com.odc.userservice.dto.response.UserRegisterResponse;

public interface AuthService {
    ApiResponse<UserRegisterResponse> register(UserRegisterRequest request);

    ApiResponse<UserLoginResponse> login(UserLoginRequest request);

    ApiResponse<RefreshTokenResponse> refreshToken(RefreshTokenRequest request);

    ApiResponse<UserLoginResponse> loginWithGoogle(GoogleLoginRequest request);
}
