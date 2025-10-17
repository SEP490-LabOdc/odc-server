package com.odc.userservice.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.odc.common.constant.Role;
import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.BusinessException;
import com.odc.common.exception.ResourceNotFoundException;
import com.odc.common.exception.UnauthenticatedException;
import com.odc.common.util.JwtUtil;
import com.odc.userservice.dto.request.GoogleLoginRequest;
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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
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
    @Value("${google.client-id}")
    private String googleClientId;

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
                                Map.of("role", user.getRole().getName(),
                                        "userId", user.getId())))
                        .refreshToken(refreshToken)
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
                Map.of("role", user.getRole().getName(),
                        "userId", user.getId()));

        return ApiResponse.success(RefreshTokenResponse.builder()
                .accessToken(accessToken)
                .build());
    }

    @Override
    public ApiResponse<UserLoginResponse> loginWithGoogle(GoogleLoginRequest request) {
        GoogleIdToken.Payload payload = verifyGoogleToken(request.getIdToken());
        if (payload == null) {
            throw new UnauthenticatedException("Invalid Google ID token!");
        }

        String email = payload.getEmail();
        User user = userRepository.findByEmail(email).orElseGet(() -> createUserFromGoogle(payload));

        // Nếu user đã tồn tại nhưng không phải từ Google login, bạn có thể xử lý thêm ở đây
        // Ví dụ: throw new BusinessException("Email already exists with password login!");

        return generateTokenResponse(user);
    }

    private GoogleIdToken.Payload verifyGoogleToken(String idTokenString) {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            return idToken != null ? idToken.getPayload() : null;
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Xác minh Google token thất bại.");
        }
    }

    private User createUserFromGoogle(GoogleIdToken.Payload payload) {
        if (userRepository.existsByEmail(payload.getEmail())) {
            throw new BusinessException("Email already exists!");
        }

        User newUser = User.builder()
                .fullName((String) payload.get("name"))
                .email(payload.getEmail())
                .emailVerified(payload.getEmailVerified())
                .avatarUrl((String) payload.get("picture"))
                .passwordHash(passwordEncoder.encode(generateRandomPassword())) // Tạo password ngẫu nhiên
                .role(roleRepository.findByName(Role.USER.toString())
                        .orElseThrow(() -> new ResourceNotFoundException("Default USER role not found.")))
                .build();

        return userRepository.save(newUser);
    }

    private String generateRandomPassword() {
        return UUID.randomUUID().toString();
    }

    private ApiResponse<UserLoginResponse> generateTokenResponse(User user) {
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
                                Map.of("role", user.getRole().getName(),
                                        "userId", user.getId())))
                        .refreshToken(refreshToken)
                        .build());
    }
}
