package com.odc.companyservice.event.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.odc.common.constant.Status;
import com.odc.commonlib.event.EventHandler;
import com.odc.commonlib.event.EventPublisher;
import com.odc.companyservice.entity.Company;
import com.odc.companyservice.repository.CompanyRepository;
import com.odc.notification.v1.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConfirmOtpEventHandler implements EventHandler {
    private final CompanyRepository companyRepository;
    private final EventPublisher eventPublisher;

    @Override
    public String getTopic() {
        return "email.otp.confirmed";
    }

    @Override
    public void handle(byte[] eventPayload) {
        try {
            SendOtpRequest event = SendOtpRequest.parseFrom(eventPayload);
            log.info("consume email received : {}", event.getEmail());

            Optional<Company> optionalCompany = companyRepository.findByEmail(event.getEmail());
            if (optionalCompany.isEmpty()) {
                log.error("company not found: {}", event.getEmail());
                return;
            }

            Company company = optionalCompany.get();
            company.setStatus(Status.PENDING.toString());
            companyRepository.save(company);
            log.info("update successfully with company status is {}", company.getStatus());

            Map<String, String> dataMap = Map.of(
                    "companyId", company.getId().toString(),
                    "companyName", company.getName()
            );

            RoleTarget roleTarget = RoleTarget.newBuilder()
                    .addRoles("LAB_ADMIN")
                    .build();

            Target target = Target.newBuilder()
                    .setRole(roleTarget)
                    .build();

            NotificationEvent notificationEvent = NotificationEvent.newBuilder()
                    .setId(UUID.randomUUID().toString())
                    .setType("COMPANY_REGISTRATION")
                    .setTitle("New Company Registration Request")
                    .setContent("A new company named \"" + company.getName() + "\" has just registered and is awaiting verification.")
                    .putAllData(dataMap)
                    .setDeepLink("/approve?id=" + company.getId())
                    .setPriority("HIGH")
                    .setTarget(target)
                    .addAllChannels(List.of(
                            Channel.WEB
                    ))
                    .setCreatedAt(System.currentTimeMillis())
                    .setCategory("COMPANY_MANAGEMENT")
                    .build();

            log.info("built notification event for LAB_ADMIN: {}", notificationEvent);

            eventPublisher.publish("notifications", notificationEvent);
            log.info("notification event published successfully.");
        } catch (InvalidProtocolBufferException ex) {
            log.error("error in parse event: {}", ex.getMessage());
        }
    }
}
