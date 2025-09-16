package com.odc.userservice.service;

import com.odc.common.constant.Role;
import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.BusinessException;
import com.odc.common.exception.ResourceNotFoundException;
import com.odc.common.exception.UnauthenticatedException;
import com.odc.common.util.JwtUtil;
import com.odc.userservice.dto.request.RefreshTokenRequest;
import com.odc.userservice.dto.request.UserLoginRequest;
import com.odc.userservice.dto.request.UserRegisterRequest;
import com.odc.userservice.dto.response.RefreshTokenResponse;
import com.odc.userservice.dto.response.UserLoginResponse;
import com.odc.userservice.dto.response.UserRegisterResponse;
import com.odc.userservice.entity.User;
import com.odc.userservice.repository.RoleRepository;
import com.odc.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private RoleRepository roleRepository;
    private JwtUtil jwtUtil;
    private StringRedisTemplate stringRedisTemplate;

    @Value("${refresh-expiration:7}")
    private int refreshExpiration;

    @Autowired
    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, RoleRepository roleRepository, JwtUtil jwtUtil, StringRedisTemplate stringRedisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.jwtUtil = jwtUtil;
        this.stringRedisTemplate = stringRedisTemplate;
    }

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
                .role(roleRepository.findByName(Role.USER.toString()).orElse(null))
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

    @Override
    public ApiResponse<UserLoginResponse> login(UserLoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .map(u -> {
                    if (!passwordEncoder.matches(request.getPassword(), u.getPasswordHash())) {
                        throw new UnauthenticatedException("Invalid email or password!");
                    }
                    return u;
                })
                .orElseThrow(() -> new UnauthenticatedException("Email does not exist!"));

        String redisKey = String.format("user:%s:refreshToken", user.getId().toString());

        String refreshToken = stringRedisTemplate.opsForValue().get(redisKey);
        if (refreshToken == null || refreshToken.isEmpty()) {
            refreshToken = jwtUtil.generateRefreshToken();
            stringRedisTemplate.opsForValue()
                    .set(redisKey, refreshToken, refreshExpiration, TimeUnit.DAYS);
        }

        return ApiResponse.success("Login successfully!",
                UserLoginResponse
                        .builder()
                        .accessToken(jwtUtil.generateToken(user.getEmail(),
                                Map.of("role", user.getRole().getName())))
                        .refreshToken(refreshToken)
                        .userId(user.getId())
                        .build());
    }

    @Override
    public ApiResponse<RefreshTokenResponse> refreshToken(RefreshTokenRequest request) {
        String redisKey = String.format("user:%s:refreshToken", request.getUserId().toString());
        String refreshToken = stringRedisTemplate.opsForValue().get(redisKey);

        if (refreshToken == null || refreshToken.isEmpty())
            throw new UnauthenticatedException("Invalid refresh token!");


        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));

        String accessToken = jwtUtil.generateToken(user.getEmail(),
                Map.of("role", user.getRole().getName()));

        return ApiResponse.success(RefreshTokenResponse.builder()
                .accessToken(accessToken)
                .build());
    }
}
