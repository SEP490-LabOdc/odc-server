package com.odc.userservice.grpc;

import com.odc.common.exception.ResourceNotFoundException;
import com.odc.userservice.entity.User;
import com.odc.userservice.v1.UserServiceGrpc;
import com.odc.userservice.v1.UserServiceProto;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

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
    @Override
    public void getUserById(
            com.odc.userservice.v1.GetUserByIdRequest request,
            io.grpc.stub.StreamObserver<com.odc.userservice.v1.GetUserByIdResponse> responseObserver) {

        UUID userId = UUID.fromString(request.getUserId());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        com.odc.userservice.v1.GetUserByIdResponse response = com.odc.userservice.v1.GetUserByIdResponse.newBuilder()
                .setId(user.getId().toString())
                .setFullName(user.getFullName())
                .setEmail(user.getEmail())
                .setPhone(user.getPhone() != null ? user.getPhone() : "")
                .setAvatarUrl(user.getAvatarUrl() != null ? user.getAvatarUrl() : "")
                .setRole(user.getRole().getName())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
