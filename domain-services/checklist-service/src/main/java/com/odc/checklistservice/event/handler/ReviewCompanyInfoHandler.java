package com.odc.checklistservice.event.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.odc.checklistservice.entity.Checklist;
import com.odc.checklistservice.entity.ChecklistItem;
import com.odc.checklistservice.repository.ChecklistRepository;
import com.odc.common.constant.ChecklistItemStatus;
import com.odc.common.constant.ChecklistStatus;
import com.odc.commonlib.event.EventHandler;
import com.odc.commonlib.util.ProtobufConverter;
import com.odc.company.v1.CreateChecklistRequest;
import com.odc.company.v1.ReviewCompanyInfoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewCompanyInfoHandler implements EventHandler {
    private final ChecklistRepository checklistRepository;

    @Override
    public String getTopic() {
        return "review.company_verification";
    }

    @Override
    @Transactional
    public void handle(byte[] eventPayload) {
        try {
            ReviewCompanyInfoEvent event = ProtobufConverter.deserialize(eventPayload, ReviewCompanyInfoEvent.parser());
            log.info("received review company info event : {}", event);

            CreateChecklistRequest createChecklistRequest = event.getCreateChecklistRequest();

            Checklist checklist = Checklist
                    .builder()
                    .templateId(UUID.fromString(createChecklistRequest.getTemplateId()))
                    .entityId(createChecklistRequest.getCompanyId())
                    .assigneeId(createChecklistRequest.getAssigneeId())
                    .status(ChecklistStatus.valueOf(createChecklistRequest.getStatus()))
                    .notes(createChecklistRequest.getNotes())
                    .build();

            List<ChecklistItem> items = createChecklistRequest.getItemsList()
                    .stream()
                    .map(req -> ChecklistItem.builder()
                            .templateItemId(UUID.fromString(req.getTemplateItemId()))
                            .completedById(req.getCompletedById())
                            .checklist(checklist)
                            .isChecked(req.getIsChecked())
                            .build())
                    .toList();

            checklist.setItems(items);
            checklistRepository.save(checklist);
        } catch (InvalidProtocolBufferException e) {
            log.error("received review company info event error", e);
            throw new RuntimeException(e);
        }
    }
}
