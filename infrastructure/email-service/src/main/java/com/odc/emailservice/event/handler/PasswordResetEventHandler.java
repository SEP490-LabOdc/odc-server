package com.odc.emailservice.event.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.odc.commonlib.event.EventHandler;
import com.odc.emailservice.constants.EmailTemplateConstant;
import com.odc.emailservice.service.EmailService;
import com.odc.user.v1.PasswordResetRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class PasswordResetEventHandler implements EventHandler {

    private final EmailService emailService;

    @Value("${custom.client.address}")
    private String clientAddress;

    @Override
    public String getTopic() {
        return "user.password.reset.event";
    }

    @Override
    public void handle(byte[] payload) {
        try {
            PasswordResetRequestEvent event = PasswordResetRequestEvent.parseFrom(payload);

            // Lấy fullName từ email (có thể cần query từ user-service nếu cần)
            // Tạm thời dùng email làm fullName, hoặc có thể lấy từ event nếu thêm vào
            String fullName = event.getEmail().split("@")[0]; // Tạm thời

            emailService.sendEmailWithHtmlTemplate(
                    event.getEmail(),
                    "[LabOdc] Đặt lại mật khẩu thành công",
                    EmailTemplateConstant.SEND_PASSWORD_RESET_TEMPLATE,
                    Map.of(
                            "fullName", event.getFullName(),
                            "newPassword", event.getNewPassword(),
                            "loginLink", String.format("%s/sign-in", clientAddress)
                    )
            );
            log.info("Successfully sent password reset confirmation email to {}", event.getEmail());
        } catch (InvalidProtocolBufferException e) {
            log.error("Error parsing PasswordResetRequestEvent", e);
            throw new RuntimeException("Error processing password reset event", e);
        }
    }
}