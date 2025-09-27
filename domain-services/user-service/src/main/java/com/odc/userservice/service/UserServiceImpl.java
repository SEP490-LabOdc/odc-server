package com.odc.userservice.service;

import com.odc.common.constant.Status;
import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.ResourceNotFoundException;
import com.odc.userservice.dto.request.CreateUserRequest;
import com.odc.userservice.dto.request.UpdatePasswordRequest;
import com.odc.userservice.dto.request.UpdateRoleRequest;
import com.odc.userservice.dto.request.UpdateUserRequest;
import com.odc.userservice.dto.response.GetUserResponse;
import com.odc.userservice.entity.Role;
import com.odc.userservice.entity.User;
import com.odc.userservice.repository.RoleRepository;
import com.odc.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public ApiResponse<GetUserResponse> getUserById(UUID id) {
        return ApiResponse.<GetUserResponse>builder()
                .success(true)
                .message("Get user successfully!")
                .timestamp(LocalDateTime.now())
                .data(userRepository.findById(id)
                        .map(u -> GetUserResponse.builder()
                                .id(u.getId())
                                .phone(u.getPhone())
                                .email(u.getEmail())
                                .avatarUrl(u.getAvatarUrl())
                                .birthDate(u.getBirthDate())
                                .fullName(u.getFullName())
                                .role(u.getRole().getName())
                                .gender(u.getGender())
                                .build())
                        .orElseThrow(() -> new ResourceNotFoundException("Not found user")))
                .build();
    }
    
    @Override
    public ApiResponse<List<GetUserResponse>> getAllUsers() {
        List<GetUserResponse> users = userRepository.findAll()
                .stream()
                .map(user -> GetUserResponse.builder()
                        .id(user.getId())
                        .phone(user.getPhone())
                        .email(user.getEmail())
                        .avatarUrl(user.getAvatarUrl())
                        .birthDate(user.getBirthDate())
                        .fullName(user.getFullName())
                        .role(user.getRole().getName())
                        .gender(user.getGender())
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.<List<GetUserResponse>>builder()
                .success(true)
                .message("Get all users successfully!")
                .timestamp(LocalDateTime.now())
                .data(users)
                .build();
    }
    
    @Override
    public ApiResponse<GetUserResponse> createUser(CreateUserRequest request) {
        try {
            // 1️⃣ Kiểm tra email đã tồn tại
            if (userRepository.existsByEmail(request.getEmail())) {
                return ApiResponse.<GetUserResponse>builder()
                        .success(false)
                        .message("Email already exists")
                        .timestamp(LocalDateTime.now())
                        .build();
            }

            // 2️⃣ Tìm Role mặc định là USER
            Role role = roleRepository.findByName("User")
                    .orElseThrow(() -> new ResourceNotFoundException("Default USER role not found. Please ensure USER role exists in database."));

            // 3️⃣ Map sang Entity
            User user = User.builder()
                    .email(request.getEmail())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .fullName(request.getFullName())
                    .phone(request.getPhone())
                    .gender(request.getGender())
                    .birthDate(request.getBirthDate())
                    .address(request.getAddress())
                    .avatarUrl(request.getAvatarUrl())
                    .status(Status.ACTIVE)
                    .role(role)
                    .emailVerified(false)
                    .phoneVerified(false)
                    .failedLoginAttempts(0)
                    .build();

            userRepository.save(user);

            // 4️⃣ Trả về DTO
            return ApiResponse.<GetUserResponse>builder()
                    .success(true)
                    .message("User created successfully!")
                    .timestamp(LocalDateTime.now())
                    .data(toGetUserResponse(user))
                    .build();
        } catch (Exception e) {
            // Log error for debugging
            System.err.println("Error creating user: " + e.getMessage());
            e.printStackTrace();
            
            return ApiResponse.<GetUserResponse>builder()
                    .success(false)
                    .message("Failed to create user: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    // ✅ Helper mapper
    private GetUserResponse toGetUserResponse(User user) {
        return GetUserResponse.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .birthDate(user.getBirthDate())
                .fullName(user.getFullName())
                .role(user.getRole().getName())
                .gender(user.getGender())
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<GetUserResponse> updateProfile(UUID userId, UpdateUserRequest request) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            
            // Update only allowed fields
            user.setFullName(request.getFullName());
            user.setPhone(request.getPhone());
            user.setGender(request.getGender());
            user.setBirthDate(request.getBirthDate());
            user.setAddress(request.getAddress());
            user.setAvatarUrl(request.getAvatarUrl());
            
            userRepository.save(user);
            
            return ApiResponse.<GetUserResponse>builder()
                    .success(true)
                    .message("Profile updated successfully!")
                    .timestamp(LocalDateTime.now())
                    .data(toGetUserResponse(user))
                    .build();
        } catch (Exception e) {
            return ApiResponse.<GetUserResponse>builder()
                    .success(false)
                    .message("Failed to update profile: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    @Transactional
    public ApiResponse<Void> updatePassword(UUID userId, UpdatePasswordRequest request) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            
            // Verify current password
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                return ApiResponse.<Void>builder()
                        .success(false)
                        .message("Current password is incorrect")
                        .timestamp(LocalDateTime.now())
                        .build();
            }
            
            // Update password
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);
            
            return ApiResponse.<Void>builder()
                    .success(true)
                    .message("Password updated successfully!")
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            return ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to update password: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    @Transactional
    public ApiResponse<GetUserResponse> updateRole(UUID userId, UpdateRoleRequest request) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
            
            user.setRole(role);
            userRepository.save(user);
            
            return ApiResponse.<GetUserResponse>builder()
                    .success(true)
                    .message("Role updated successfully!")
                    .timestamp(LocalDateTime.now())
                    .data(toGetUserResponse(user))
                    .build();
        } catch (Exception e) {
            return ApiResponse.<GetUserResponse>builder()
                    .success(false)
                    .message("Failed to update role: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    @Transactional
    public ApiResponse<GetUserResponse> updateStatus(UUID userId, Status status) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            
            user.setStatus(status);
            userRepository.save(user);
            
            return ApiResponse.<GetUserResponse>builder()
                    .success(true)
                    .message("Status updated successfully!")
                    .timestamp(LocalDateTime.now())
                    .data(toGetUserResponse(user))
                    .build();
        } catch (Exception e) {
            return ApiResponse.<GetUserResponse>builder()
                    .success(false)
                    .message("Failed to update status: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    public ApiResponse<Void> deleteUser(UUID id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            userRepository.delete(user);
            return ApiResponse.<Void>builder()
                    .success(true)
                    .message("User deleted successfully!")
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            return ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete user: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }
}