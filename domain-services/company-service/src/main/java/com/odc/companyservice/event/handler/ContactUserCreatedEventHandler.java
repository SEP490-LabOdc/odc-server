package com.odc.companyservice.event.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.odc.commonlib.event.EventHandler;
import com.odc.companyservice.entity.Company;
import com.odc.companyservice.repository.CompanyRepository;
import com.odc.user.v1.ContactUserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class ContactUserCreatedEventHandler implements EventHandler {
    private final CompanyRepository companyRepository;

    @Override
    public String getTopic() {
        return "contact.user.created.event";
    }

    @Override
    public void handle(byte[] eventPayload) {
        try {
            ContactUserCreatedEvent contactUserCreatedEvent = ContactUserCreatedEvent.parseFrom(eventPayload);

            Company company = companyRepository.findById(
                    UUID.fromString(contactUserCreatedEvent.getCompanyId())
            ).orElseThrow(() -> {
                // TODO: Publish failed event to rollback (saga pattern + outbox pattern)
                log.error("Company not found with id {}", contactUserCreatedEvent.getCompanyId());
                return new RuntimeException("Company not found");
            });

            company.setUserId(UUID.fromString(contactUserCreatedEvent.getUserId()));
            companyRepository.save(company);
        } catch (InvalidProtocolBufferException e) {
            log.error("ContactUserCreatedEvent parse error: {}", e.getMessage());
        }
    }
}
