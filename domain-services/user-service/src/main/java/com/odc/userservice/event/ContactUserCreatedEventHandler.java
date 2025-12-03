package com.odc.userservice.event;

import com.google.protobuf.InvalidProtocolBufferException;
import com.odc.common.constant.Role;
import com.odc.common.constant.Status;
import com.odc.common.util.StringUtil;
import com.odc.commonlib.event.EventHandler;
import com.odc.commonlib.event.EventPublisher;
import com.odc.company.v1.CompanyApprovedEvent;
import com.odc.company.v1.ContactUser;
import com.odc.user.v1.ContactUserCreatedEvent;
import com.odc.user.v1.UserCreatedEvent;
import com.odc.user.v1.UserWelcomeNotificationEvent;
import com.odc.userservice.entity.User;
import com.odc.userservice.repository.RoleRepository;
import com.odc.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ContactUserCreatedEventHandler implements EventHandler {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final EventPublisher eventPublisher;

    @Override
    public String getTopic() {
        return "company.approved.event";
    }

    @Override
    public void handle(byte[] eventPayload) {
        try {
            CompanyApprovedEvent event = CompanyApprovedEvent.parseFrom(eventPayload);
            log.info("consume CompanyApprovedEvent received: {}", event);

            com.odc.userservice.entity.Role companyRole = roleRepository.findByName(Role.COMPANY.toString())
                    .orElseThrow(() -> new RuntimeException("Role not found"));

            ContactUser contactUser = event.getContactUser();

            String rawPassword = StringUtil.generateRandomString(12);
            User user = User.builder()
                    .fullName(contactUser.getName())
                    .email(contactUser.getEmail())
                    .phone(contactUser.getPhone())
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .role(companyRole)
                    .status(Status.ACTIVE)
                    .build();

            userRepository.save(user);
            log.info("user has been saved: {}", user);

            UserCreatedEvent userCreatedEvent = UserCreatedEvent.newBuilder()
                    .setUserId(user.getId().toString())
                    .setEmail(user.getEmail())
                    .setFullName(user.getFullName())
                    .setRole(user.getRole().getName())
                    .build();
            eventPublisher.publish("user.created", userCreatedEvent);

            ContactUserCreatedEvent contactUserCreatedEvent = ContactUserCreatedEvent
                    .newBuilder()
                    .setUserId(user.getId().toString())
                    .setCompanyId(event.getCompanyId())
                    .build();
            eventPublisher.publish("contact.user.created.event", contactUserCreatedEvent);
            log.info("user has been created: {}", user);

            UserWelcomeNotificationEvent welcomeEvent = UserWelcomeNotificationEvent.newBuilder()
                    .setUserId(user.getId().toString())
                    .setEmail(user.getEmail())
                    .setFullName(user.getFullName())
                    .setPassword(rawPassword)
                    .build();
            eventPublisher.publish("user.welcome.notification.event", welcomeEvent);
            log.info("Published welcome notification event for {}", user.getEmail());
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}
