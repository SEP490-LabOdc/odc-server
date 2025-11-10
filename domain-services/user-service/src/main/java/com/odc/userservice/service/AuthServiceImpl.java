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
import com.odc.common.util.StringUtil;
import com.odc.commonlib.event.EventPublisher;
import com.odc.user.v1.PasswordResetRequestEvent;
import com.odc.userservice.dto.request.*;
import com.odc.userservice.dto.response.RefreshTokenResponse;
import com.odc.userservice.dto.response.UserLoginResponse;
import com.odc.userservice.dto.response.UserRegisterResponse;
import com.odc.userservice.entity.User;
import com.odc.userservice.repository.RoleRepository;
import com.odc.userservice.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private RoleRepository roleRepository;
    private JwtUtil jwtUtil;
    private StringRedisTemplate stringRedisTemplate;
    private EventPublisher eventPublisher;

    @Value("${refresh-expiration:7}")
    private int refreshExpiration;
    @Value("${google.client-id}")
    private String googleClientId;


    @Autowired
    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, RoleRepository roleRepository, JwtUtil jwtUtil, StringRedisTemplate stringRedisTemplate, EventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.jwtUtil = jwtUtil;
        this.stringRedisTemplate = stringRedisTemplate;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public ApiResponse<UserRegisterResponse> register(UserRegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException("Email đã tồn tại");
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
        response.setMessage("Đăng ký thành công!");
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
                        throw new UnauthenticatedException("Không tồn tại email hoặc mật khẩu không đúng!");
                    }
                    return u;
                })
                .orElseThrow(() -> new UnauthenticatedException("Email Không tồn tại !"));

        String redisKey = String.format("user:%s:refreshToken", user.getId().toString());

        String refreshToken = stringRedisTemplate.opsForValue().get(redisKey);
        if (refreshToken == null || refreshToken.isEmpty()) {
            refreshToken = jwtUtil.generateRefreshToken();
            stringRedisTemplate.opsForValue()
                    .set(redisKey, refreshToken, refreshExpiration, TimeUnit.DAYS);
        }

        return ApiResponse.success("Đăng nhập thành công !",
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

        if (refreshToken == null ||
                refreshToken.isEmpty() ||
                !refreshToken.equals(request.getRefreshToken()))
            throw new UnauthenticatedException("Refresh token không hợp lệ!");

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("không tìm thấy User!"));

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
            throw new UnauthenticatedException("Mã Google ID token không hợp lệ !");
        }

        String email = payload.getEmail();
        validateFptEmail(email);
        User user = userRepository.findByEmail(email).orElseGet(() -> createUserFromGoogle(payload));

        // Nếu user đã tồn tại nhưng không phải từ Google login, bạn có thể xử lý thêm ở đây
        // Ví dụ: throw new BusinessException("Email already exists with password login!");

        return generateTokenResponse(user);
    }

    @Override
    public ApiResponse<Void> resetPassword(ResetPasswordRequest request) {
        // Tìm user theo email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Email không tồn tại trong hệ thống"));

        // Verify OTP từ Redis
        String redisKey = String.format("user-%s-otp", request.getEmail());
        String cachedOtp = stringRedisTemplate.opsForValue().get(redisKey);

        if (cachedOtp == null || cachedOtp.isEmpty()) {
            throw new BusinessException("OTP đã hết hạn hoặc không tồn tại. Vui lòng yêu cầu mã OTP mới.");
        }

        if (!cachedOtp.equals(request.getOtp())) {
            throw new BusinessException("OTP không đúng. Vui lòng kiểm tra lại.");
        }

        // Generate password mới (server-side)
        String newPassword = StringUtil.generateRandomString(12);

        // Update password trong database
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Xóa OTP khỏi Redis sau khi verify thành công
        stringRedisTemplate.delete(redisKey);

        // Publish event với password đã generate
        PasswordResetRequestEvent event = PasswordResetRequestEvent.newBuilder()
                .setEmail(user.getEmail())
                .setOtp(request.getOtp())
                .setNewPassword(newPassword) // Password được generate bởi server
                .setFullName(user.getFullName())
                .build();

        eventPublisher.publish("user.password.reset.event", event);

        log.info("Password reset successful for user: {}", user.getEmail());

        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đặt lại mật khẩu thành công! Vui lòng kiểm tra email để nhận mật khẩu mới.")
                .timestamp(LocalDateTime.now())
                .build();
    }

    private void validateFptEmail(String email) {
        if (email == null || email.isEmpty()) {
            throw new UnauthenticatedException("Email không được để trống!");
        }
        boolean isFptEmail = email.toLowerCase().endsWith("@fpt.edu.vn");

        if (!isFptEmail) {
            throw new UnauthenticatedException("Chỉ cho phép đăng nhập bằng email FPT !");
        }
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
            throw new BusinessException("Email đã tồn tại !");
        }

        User newUser = User.builder()
                .fullName((String) payload.get("name"))
                .email(payload.getEmail())
                .emailVerified(payload.getEmailVerified())
                .avatarUrl((String) payload.get("picture"))
                .passwordHash(passwordEncoder.encode(generateRandomPassword())) // Tạo password ngẫu nhiên
                .role(roleRepository.findByName(Role.USER.toString())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Role user mặc định (USER).")))
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

        return ApiResponse.success("Đăng nhập thành công !",
                UserLoginResponse
                        .builder()
                        .accessToken(jwtUtil.generateToken(user.getEmail(),
                                Map.of("role", user.getRole().getName(),
                                        "userId", user.getId())))
                        .refreshToken(refreshToken)
                        .build());
    }
}
