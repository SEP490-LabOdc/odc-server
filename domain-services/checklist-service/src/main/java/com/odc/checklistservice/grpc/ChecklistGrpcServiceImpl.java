package com.odc.checklistservice.grpc;

import com.odc.checklist.v1.ChecklistServiceGrpc;
import com.odc.checklist.v1.GetDescriptionsRequest;
import com.odc.checklist.v1.GetDescriptionsResponse;
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
}
