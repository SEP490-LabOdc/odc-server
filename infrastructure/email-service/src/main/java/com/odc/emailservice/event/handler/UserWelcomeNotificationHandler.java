package com.odc.emailservice.event.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.odc.commonlib.event.EventHandler;
import com.odc.emailservice.constants.EmailTemplateConstant;
import com.odc.emailservice.service.EmailService;
import com.odc.user.v1.UserWelcomeNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserWelcomeNotificationHandler implements EventHandler {

    private final EmailService emailService;

    @Value("${custom.client.address}")
    private String clientAddress;

    @Override
    public String getTopic() {
        return "user.welcome.notification.event";
    }

    @Override
    public void handle(byte[] payload) {
        try {
            UserWelcomeNotificationEvent event = UserWelcomeNotificationEvent.parseFrom(payload);
            emailService.sendEmailWithHtmlTemplate(
                    event.getEmail(),
                    "[LabOdc] Thông tin tài khoản đăng nhập",
                    EmailTemplateConstant.SEND_ACCOUNT_CREDENTIAL_TEMPLATE,
                    Map.of(
                            "fullName", event.getFullName(),
                            "email", event.getEmail(),
                            "password", event.getPassword(),
                            "loginLink", String.format("%s/sign-in", clientAddress)
                    )
            );
            log.info("Successfully sent account credential email to {}", event.getEmail());
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}