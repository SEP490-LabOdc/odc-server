package com.odc.userservice.service;

import com.odc.common.constant.Status;
import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.SearchRequest;
import com.odc.common.dto.SortRequest;
import com.odc.common.exception.ResourceNotFoundException;
import com.odc.common.specification.GenericSpecification;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
                .message("Lấy thông tin user thành công!")
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
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user")))
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
                .message("Lấy danh sách user thành công !")
                .timestamp(LocalDateTime.now())
                .data(users)
                .build();
    }

    @Override
    public ApiResponse<GetUserResponse> createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ApiResponse.<GetUserResponse>builder()
                    .success(false)
                    .message("Email đã tồn tại ")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        Role role = roleRepository.findByName(com.odc.common.constant.Role.USER.toString())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Role mặc định USER. Vui lòng đảm bảo rằng Role USER đã tồn tại trong cơ sở dữ liệu."));

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

        return ApiResponse.<GetUserResponse>builder()
                .success(true)
                .message("Thêm người dùng mới thành công!")
                .timestamp(LocalDateTime.now())
                .data(toGetUserResponse(user))
                .build();
    }

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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không thấy User"));

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
                .message("Cập nhật hồ sơ thành công !")
                .timestamp(LocalDateTime.now())
                .data(toGetUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<Void> updatePassword(UUID userId, UpdatePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy User"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            return ApiResponse.<Void>builder()
                    .success(false)
                    .message("Mật khẩu hiện tại không đúng!")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ApiResponse.<Void>builder()
                .success(true)
                .message("Cập nhật mật khẩu thành công!")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<GetUserResponse> updateRole(UUID userId, UpdateRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy User"));

        Role role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Role"));

        user.setRole(role);
        userRepository.save(user);

        return ApiResponse.<GetUserResponse>builder()
                .success(true)
                .message("Cập nhật Role thành công!")
                .timestamp(LocalDateTime.now())
                .data(toGetUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<GetUserResponse> updateStatus(UUID userId, Status status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy User"));

        user.setStatus(status);
        userRepository.save(user);

        return ApiResponse.<GetUserResponse>builder()
                .success(true)
                .message("Cập nhật trạng thái thành công!")
                .timestamp(LocalDateTime.now())
                .data(toGetUserResponse(user))
                .build();
    }

    @Override
    public ApiResponse<Void> deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy User"));
        userRepository.delete(user);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Xóa User thành công!")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public ApiResponse<List<GetUserResponse>> searchUsers(SearchRequest request) {
        Specification<User> specification = new GenericSpecification<>(request.getFilters());

        List<Sort.Order> orders = new ArrayList<>();
        if (request.getSorts() != null && !request.getSorts().isEmpty()) {
            for (SortRequest sortRequest : request.getSorts()) {
                orders.add(new Sort.Order(sortRequest.getDirection(), sortRequest.getKey()));
            }
        }
        Sort sort = Sort.by(orders);
        List<GetUserResponse> users = userRepository.findAll(specification, sort)
                .stream()
                .map(this::toGetUserResponse)
                .collect(Collectors.toList());

        return ApiResponse.<List<GetUserResponse>>builder()
                .success(true)
                .message("Tìm kiếm người dùng thành công!")
                .timestamp(LocalDateTime.now())
                .data(users)
                .build();
    }
}