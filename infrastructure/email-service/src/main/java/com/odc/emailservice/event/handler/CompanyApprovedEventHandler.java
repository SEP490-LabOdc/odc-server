package com.odc.emailservice.event.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.odc.commonlib.event.EventHandler;
import com.odc.company.v1.CompanyApprovedEvent;
import com.odc.emailservice.constants.EmailTemplateConstant;
import com.odc.emailservice.service.EmailService;
import com.odc.userservice.v1.GetNameRequest;
import com.odc.userservice.v1.UserServiceGrpc;
import io.grpc.ManagedChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class CompanyApprovedEventHandler implements EventHandler {
    private final EmailService emailService;
    private final ManagedChannel userServiceChannel;

    @Override
    public String getTopic() {
        return "company.approved.event";
    }

    @Override
    public void handle(byte[] eventPayload) {
        try {
            CompanyApprovedEvent event = CompanyApprovedEvent.parseFrom(eventPayload);
            log.info("consume CompanyApprovedEvent received: {}", event);

            emailService.sendEmailWithHtmlTemplate(
                    event.getEmail(),
                    String.format("[LabOdc] Công ty [%s] đã được duyệt thành công trên LabOdc", event.getCompanyName()),
                    EmailTemplateConstant.SEND_COMPANY_APPROVED_TEMPLATE,
                    Map.of(
                            "contactUserName", event.getContactUser().getName(),
                            "companyName", event.getCompanyName(),
                            "companyEmail", event.getEmail(),
                            "approvedBy", UserServiceGrpc.newBlockingStub(userServiceChannel).getName(
                                            GetNameRequest
                                                    .newBuilder()
                                                    .addAllIds(List.of(event.getApprovedBy()))
                                                    .build()
                                    ).getMapMap()
                                    .getOrDefault(event.getApprovedBy(), "Unknown"),
                            "contactUserEmail", event.getContactUser().getEmail(),
                            "contactUserPhone", event.getContactUser().getPhone()
                    ));
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}
