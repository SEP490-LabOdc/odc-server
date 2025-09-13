package com.odc.userservice.service;

import com.odc.common.constant.Role;
import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.BusinessException;
import com.odc.userservice.dto.request.UserRegisterRequest;
import com.odc.userservice.dto.response.UserRegisterResponse;
import com.odc.userservice.entity.User;
import com.odc.userservice.repository.RoleRepository;
import com.odc.userservice.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private RoleRepository roleRepository;

    @Override
    public ApiResponse<UserRegisterResponse> register(UserRegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException("Email already exists!");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .emailVerified(false)
                .phoneVerified(false)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(roleRepository.findByName(Role.ADMIN.toString()).orElse(null))
                .build();

        userRepository.save(user);

        ApiResponse<UserRegisterResponse> response = new ApiResponse<>();

        response.setSuccess(true);
        response.setMessage("User registered successfully!");
        response.setData(
                UserRegisterResponse.builder().id(user.getId())
                        .email(user.getEmail())
                        .build()
        );

        return response;
    }
}
