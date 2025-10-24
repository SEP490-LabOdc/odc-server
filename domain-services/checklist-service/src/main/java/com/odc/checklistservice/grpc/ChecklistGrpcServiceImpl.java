package com.odc.checklistservice.grpc;

import com.odc.checklist.v1.*;
import com.odc.checklistservice.repository.ChecklistItemRepository;
import com.odc.checklistservice.repository.ChecklistTemplateRepository;
import com.odc.checklistservice.repository.TemplateItemRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.UUID;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class ChecklistGrpcServiceImpl extends ChecklistServiceGrpc.ChecklistServiceImplBase {
    private final TemplateItemRepository templateItemRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final ChecklistTemplateRepository checklistTemplateRepository;

    @Override
    public void getDescriptions(GetDescriptionsRequest request, StreamObserver<GetDescriptionsResponse> responseObserver) {
        if (request.getIdsList().isEmpty()) {
            log.info("request.getIdsList() is empty");
        }

        List<String> descriptions = templateItemRepository.getDescriptionsByIds(request
                .getIdsList()
                .stream()
                .map(UUID::fromString)
                .toList());

        log.info("response data: {}", descriptions);

        responseObserver.onNext(GetDescriptionsResponse.newBuilder().addAllDescriptions(descriptions).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getChecklistItemsByTemplateTypeAndEntityId(GetChecklistItemsByTemplateTypeAndEntityIdRequest request, StreamObserver<GetChecklistItemsByTemplateTypeAndEntityIdResponse> responseObserver) {
        UUID templateId = checklistTemplateRepository.findIdByEntityTypeById(request.getTemplateType());
        log.info("templateId: {}", templateId);
        if (templateId == null) {
            log.info("templateId is null");
            responseObserver.onError(
                    new RuntimeException("templateId is null")
            );
            return;
        }

        List<TemplateItem> templateItems = checklistItemRepository
                .getChecklistItemsByTemplateTypeAndEntityId(templateId, request.getEntityId())
                .stream()
                .map(ci -> TemplateItem
                        .newBuilder()
                        .setId(ci.getTemplateItemId().toString())
                        .setIsChecked(ci.getIsChecked())
                        .build())
                .toList();

        responseObserver.onNext(GetChecklistItemsByTemplateTypeAndEntityIdResponse.newBuilder()
                .addAllTemplateItems(templateItems)
                .build());
        responseObserver.onCompleted();
    }
}
