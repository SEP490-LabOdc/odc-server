package com.odc.userservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.ResourceNotFoundException;
import com.odc.userservice.dto.response.GetUserResponse;
import com.odc.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

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
}
