package com.odc.emailservice.event.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.odc.commonlib.event.EventHandler;
import com.odc.emailservice.service.EmailService;
import com.odc.projectservice.v1.ProjectUpdateRequiredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectUpdateRequiredEventHandler implements EventHandler {

    private final EmailService emailService;

    public static final String SEND_PROJECT_UPDATE_TEMPLATE = "emails/project-update-required-email";

    @Override
    public String getTopic() {
        return "project.update-required";
    }

    @Override
    public void handle(byte[] eventPayload) {
        try {
            ProjectUpdateRequiredEvent event = ProjectUpdateRequiredEvent.parseFrom(eventPayload);
            log.info("Received ProjectUpdateRequiredEvent: {}", event);

            String subject = String.format("[LabOdc] Yêu cầu cập nhật dự án: %s", event.getProjectTitle());

            Map<String, Object> model = new HashMap<>();
            model.put("projectId", event.getProjectId());
            model.put("projectTitle", event.getProjectTitle());
            model.put("companyId", event.getCompanyId());
            model.put("companyName", event.getCompanyName());
            model.put("notes", event.getNotes());
            model.put("contactPersonEmail", event.getContactPersonEmail());

            emailService.sendEmailWithHtmlTemplate(
                    event.getContactPersonEmail(),
                    subject,
                    SEND_PROJECT_UPDATE_TEMPLATE,
                    model
            );

            log.info("Email sent to {}", event.getContactPersonEmail());

        } catch (InvalidProtocolBufferException ex) {
            log.error("Failed to parse ProjectUpdateRequiredEvent: {}", ex.getMessage(), ex);
        } catch (Exception e) {
            log.error("Error handling ProjectUpdateRequiredEvent: {}", e.getMessage(), e);
        }
    }
}
