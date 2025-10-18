package com.odc.emailservice.service;

import com.odc.common.exception.BusinessException;
import com.odc.commonlib.event.EventPublisher;
import com.odc.emailservice.constants.EmailTemplateConstant;
import com.odc.emailservice.dto.request.ConfirmOtpRequest;
import com.odc.emailservice.dto.request.SendOtpRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {
    private final EmailService emailService;
    private final StringRedisTemplate stringRedisTemplate;
    private final EventPublisher eventPublisher;

    @Override
    public void sendOtpRequest(SendOtpRequest request) {
        String otp = generateAndCacheOtp(request.getEmail());
        emailService.sendEmailWithHtmlTemplate(
                request.getEmail(),
                "[LabOdc] Mã OTP xác nhận email",
                EmailTemplateConstant.SEND_OTP_TEMPLATE,
                Map.of("otpCode", otp)
        );
    }

    @Override
    public void sendOtpRequest(String request) {
        String otp = generateAndCacheOtp(request);
        emailService.sendEmailWithHtmlTemplate(
                request,
                "[LabOdc] Mã OTP xác nhận email",
                EmailTemplateConstant.SEND_OTP_TEMPLATE,
                Map.of("otpCode", otp)
        );
    }

    @Override
    public void confirmOtpRequest(ConfirmOtpRequest request) {
        clearCache(request.getEmail(), request.getOtp());

        eventPublisher.publish("email.otp.confirmed", com.odc.notification.v1.SendOtpRequest
                .newBuilder()
                .setEmail(request.getEmail())
                .build());

        log.info("otp code confirmed and publish event to notification service: {}", request);
    }

    private String generateAndCacheOtp(String key) {
        Random random = new Random();
        String otp = String.format("%06d", random.nextInt(999999));

        stringRedisTemplate.opsForValue().set(
                String.format("user-%s-otp", key),
                otp,
                5,
                TimeUnit.MINUTES
        );

        return otp;
    }

    private void clearCache(String key, String otpCode) {
        String formattedKey = String.format("user-%s-otp", key);
        String value = stringRedisTemplate.opsForValue().get(formattedKey);

        if (!otpCode.equals(value)) {
            throw new BusinessException("Có lỗi khi xác nhận OTP Code.");
        }

        stringRedisTemplate.delete(formattedKey);
    }
}
