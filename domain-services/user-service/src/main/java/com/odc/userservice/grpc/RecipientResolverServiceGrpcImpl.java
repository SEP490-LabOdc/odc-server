package com.odc.userservice.grpc;

import com.odc.common.constant.Status;
import com.odc.company.v1.RecipientResolverServiceGrpc;
import com.odc.company.v1.ResolveRecipientsRequest;
import com.odc.company.v1.ResolveRecipientsResponse;
import com.odc.notification.v1.Target;
import com.odc.userservice.repository.UserRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class RecipientResolverServiceGrpcImpl extends RecipientResolverServiceGrpc.RecipientResolverServiceImplBase {

    private final UserRepository userRepository;

    @Override
    public void resolveRecipients(ResolveRecipientsRequest request, StreamObserver<ResolveRecipientsResponse> responseObserver) {
        Target target = request.getTarget();

        ResolveRecipientsResponse.Builder responseBuilder = ResolveRecipientsResponse.newBuilder();

        switch (target.getTargetTypeCase()) {
            case ALL:
                boolean includeInactive = target.getAll().getIncludeInactive();
                if (includeInactive)
                    responseBuilder.addAllUserIds(userRepository.findAllUserIds().stream().map(String::valueOf).toList());
                else
                    responseBuilder.addAllUserIds(userRepository.findAllActiveUserIds(Status.ACTIVE.toString()).stream().map(String::valueOf).toList());
                break;

            case ROLE:
                List<String> roles = target.getRole().getRolesList();
                responseBuilder.addAllUserIds(userRepository.findUserIdsByRoles(roles).stream().map(String::valueOf).toList());
                break;

            case USER:
                List<String> userIds = target.getUser().getUserIdsList();
                responseBuilder.addAllUserIds(userIds);
                break;

            case TARGETTYPE_NOT_SET:
            default:
                break;
        }

        log.info("response data: {}", responseBuilder.build());
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }
}
