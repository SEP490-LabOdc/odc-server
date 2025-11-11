package com.odc.userservice.service;

import com.odc.common.constant.Status;
import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.common.dto.SearchRequest;
import com.odc.userservice.dto.request.CreateUserRequest;
import com.odc.userservice.dto.request.UpdatePasswordRequest;
import com.odc.userservice.dto.request.UpdateRoleRequest;
import com.odc.userservice.dto.request.UpdateUserRequest;
import com.odc.userservice.dto.response.GetUserResponse;
import com.odc.userservice.dto.response.MentorResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {
    ApiResponse<GetUserResponse> getUserById(UUID id);

    ApiResponse<List<GetUserResponse>> getAllUsers();

    ApiResponse<GetUserResponse> createUser(CreateUserRequest request);

    ApiResponse<GetUserResponse> updateProfile(UUID userId, UpdateUserRequest request);

    ApiResponse<Void> updatePassword(UUID userId, UpdatePasswordRequest request);

    ApiResponse<GetUserResponse> updateRole(UUID userId, UpdateRoleRequest request);

    ApiResponse<GetUserResponse> updateStatus(UUID userId, Status status);

    ApiResponse<Void> deleteUser(UUID userId);

    ApiResponse<List<GetUserResponse>> searchUsers(SearchRequest request);

    ApiResponse<PaginatedResult<GetUserResponse>> searchUsersWithPagination(SearchRequest request);

    ApiResponse<List<MentorResponse>> getMentorsWithProjectCount();
}
