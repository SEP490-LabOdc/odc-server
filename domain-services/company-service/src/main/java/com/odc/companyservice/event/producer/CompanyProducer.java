package com.odc.companyservice.event.producer;

import com.odc.commonlib.event.EventPublisher;
import com.odc.company.v1.CompanyApprovedEvent;
import com.odc.company.v1.CompanyUpdateRequestEmailEvent;
import com.odc.company.v1.ReviewCompanyInfoEvent;
import com.odc.notification.v1.NotificationEvent;
import com.odc.notification.v1.SendOtpRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CompanyProducer {
    private final EventPublisher eventPublisher;

    public void sendOtpEmailEvent(SendOtpRequest request) {
        log.info("send email event : {}", request.getEmail());
        eventPublisher.publish("email.otp.company_verification", request);
    }

    public void sendReviewCompanyInfoEvent(ReviewCompanyInfoEvent event) {
        log.info("send review company info event : {}", event);
        eventPublisher.publish("review.company_verification", event);
    }

    public void notifyCompanyRegistrationEvent(NotificationEvent event) {
        log.info("notify company registration event: {}", event);
        eventPublisher.publish("notifications", event);
    }

    public void publishCompanyApprovedEmail(CompanyApprovedEvent event) {
        log.info("publish company approved email event : {}", event);
        eventPublisher.publish("company.approved.event", event);
    }

    public void publishCompanyUpdateRequestEmail(CompanyUpdateRequestEmailEvent event) {
        log.info("publish company update request email event : {}", event);
        eventPublisher.publish("email.company_update_request", event);
    }
}
