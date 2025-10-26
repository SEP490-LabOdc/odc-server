package com.odc.emailservice.event.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.odc.checklist.v1.ChecklistServiceGrpc;
import com.odc.checklist.v1.GetDescriptionsRequest;
import com.odc.common.constant.Constants;
import com.odc.common.util.StringUtil;
import com.odc.commonlib.event.EventHandler;
import com.odc.company.v1.CompanyUpdateRequestEmailEvent;
import com.odc.emailservice.constants.EmailTemplateConstant;
import com.odc.emailservice.service.EmailService;
import io.grpc.ManagedChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class CompanyUpdateEventHandler implements EventHandler {
    private final EmailService emailService;
    private final ManagedChannel checklistServiceChannel;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${custom.client.address}")
    private String clientAddress;

    @Override
    public String getTopic() {
        return "email.company_update_request";
    }

    @Override
    public void handle(byte[] eventPayload) {
        try {
            CompanyUpdateRequestEmailEvent event = CompanyUpdateRequestEmailEvent.parseFrom(eventPayload);
            log.info("Received CompanyUpdateRequestEmailEvent: {}", event);

            ChecklistServiceGrpc.ChecklistServiceBlockingStub blockingStub = ChecklistServiceGrpc.newBlockingStub(checklistServiceChannel);
            List<String> checklistDescriptions = blockingStub.getDescriptions(
                    GetDescriptionsRequest.newBuilder()
                            .addAllIds(event.getIncompleteChecklistsList())
                            .build()
            ).getDescriptionsList();
            log.info("Checklist descriptions to include in email: {}", checklistDescriptions);

            String token = StringUtil.generateRandomString(11);
            stringRedisTemplate.opsForValue().set(Constants.COMPANY_UPDATE_TOKEN_KEY_PREFIX + token, event.getCompanyId());

            emailService.sendEmailWithHtmlTemplate(
                    event.getEmail(),
                    String.format("[LabOdc] Yêu cầu cập nhật thông tin cho công ty %s", event.getCompanyName()),
                    EmailTemplateConstant.SEND_COMPANY_UPDATE_TEMPLATE,
                    Map.of(
                            "companyName", event.getCompanyName(),
                            "notes", event.getNotes(),
                            "incompleteChecklists", checklistDescriptions,
                            "updateLink", String.format("%s/company-register/update?token=%s", clientAddress, token)
                    )
            );
            log.info("Successfully sent company update request email to {}", event.getEmail());
        } catch (InvalidProtocolBufferException ex) {
            log.error("Error parsing CompanyUpdateRequestEmailEvent event", ex);
        } catch (Exception ex) {
            log.error("An unexpected error occurred while handling CompanyUpdateRequestEmailEvent", ex);
        }
    }
}
