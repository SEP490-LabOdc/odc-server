package com.odc.userservice.grpc;

import com.odc.userservice.v1.UserServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class UserServiceGrpcImpl extends UserServiceGrpc.UserServiceImplBase {
    private final com.odc.userservice.repository.UserRepository userRepository;

    @Override
    public void checkEmailExists(com.odc.userservice.v1.CheckEmailRequest request, io.grpc.stub.StreamObserver<com.odc.userservice.v1.CheckEmailResponse> responseObserver) {
        boolean exists = userRepository.findByEmail(request.getEmail()).isPresent();
        com.odc.userservice.v1.CheckEmailResponse response = com.odc.userservice.v1.CheckEmailResponse
                .newBuilder()
                .setResult(exists)
                .build();
        log.info("response data: {}", response);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
