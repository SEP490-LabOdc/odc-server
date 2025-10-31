package com.odc.checklistservice.event.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.odc.checklistservice.entity.Checklist;
import com.odc.checklistservice.entity.ChecklistItem;
import com.odc.checklistservice.repository.ChecklistRepository;
import com.odc.common.constant.ChecklistStatus;
import com.odc.commonlib.event.EventHandler;
import com.odc.commonlib.util.ProtobufConverter;
import com.odc.company.v1.CreateChecklistRequest;
import com.odc.company.v1.ReviewCompanyInfoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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
            log.info("Received review company info event : {}", event);

            CreateChecklistRequest createChecklistRequest = event.getCreateChecklistRequest();

            UUID templateId = UUID.fromString(createChecklistRequest.getTemplateId());
            String entityId = createChecklistRequest.getCompanyId();

            Optional<Checklist> existingOpt = checklistRepository.findByTemplateIdAndEntityId(templateId, entityId);

            Checklist checklist;
            if (existingOpt.isPresent()) {
                checklist = existingOpt.get();
                log.info("Checklist existed, updating items for templateId={}, entityId={}", templateId, entityId);

                checklist.setStatus(ChecklistStatus.valueOf(createChecklistRequest.getStatus()));
                checklist.setNotes(createChecklistRequest.getNotes());

                Map<UUID, ChecklistItem> existingItemsMap = checklist.getItems()
                        .stream()
                        .collect(Collectors.toMap(ChecklistItem::getTemplateItemId, i -> i));

                List<ChecklistItem> updatedItems = createChecklistRequest.getItemsList().stream()
                        .map(req -> {
                            UUID templateItemId = UUID.fromString(req.getTemplateItemId());
                            ChecklistItem item = existingItemsMap.getOrDefault(templateItemId,
                                    ChecklistItem.builder()
                                            .templateItemId(templateItemId)
                                            .checklist(checklist)
                                            .build());
                            item.setCompletedById(req.getCompletedById());
                            item.setIsChecked(req.getIsChecked());
                            return item;
                        })
                        .toList();

                checklist.getItems().clear();
                checklist.getItems().addAll(updatedItems);
            } else {
                checklist = Checklist.builder()
                        .templateId(templateId)
                        .entityId(entityId)
                        .assigneeId(createChecklistRequest.getAssigneeId())
                        .status(ChecklistStatus.valueOf(createChecklistRequest.getStatus()))
                        .notes(createChecklistRequest.getNotes())
                        .build();

                List<ChecklistItem> items = createChecklistRequest.getItemsList().stream()
                        .map(req -> ChecklistItem.builder()
                                .templateItemId(UUID.fromString(req.getTemplateItemId()))
                                .completedById(req.getCompletedById())
                                .checklist(checklist)
                                .isChecked(req.getIsChecked())
                                .build())
                        .toList();

                checklist.setItems(items);
                log.info("Creating new checklist for templateId={}, entityId={}", templateId, entityId);
            }

            checklistRepository.save(checklist);
        } catch (InvalidProtocolBufferException e) {
            log.error("Received review company info event error", e);
            throw new RuntimeException(e);
        }
    }
}
