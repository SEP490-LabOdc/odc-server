package com.odc.userservice.grpc;

import com.odc.common.exception.ResourceNotFoundException;
import com.odc.userservice.entity.User;
import com.odc.userservice.v1.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Override
    public void getName(GetNameRequest request, StreamObserver<GetNameResponse> responseObserver) {
        List<UUID> userIds = request.getIdsList().stream()
                .map(UUID::fromString)
                .toList();

        List<User> users = userRepository.findAllById(userIds);

        Map<String, String> dataMap = users.stream()
                .collect(Collectors.toMap(
                        u -> u.getId().toString(),
                        User::getFullName
                ));

        GetNameResponse response = GetNameResponse.newBuilder()
                .putAllMap(dataMap)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void checkRoleByUserId(CheckRoleByUserIdRequest request, StreamObserver<CheckRoleByUserIdResponse> responseObserver) {
        try {
            UUID userId = UUID.fromString(request.getUserId());
            String roleName = request.getRoleName();

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

            boolean result = user.getRole() != null && user.getRole().getName().equalsIgnoreCase(roleName);

            CheckRoleByUserIdResponse response = CheckRoleByUserIdResponse.newBuilder()
                    .setResult(result)
                    .build();

            log.info("CheckRoleByUserId -> userId: {}, roleName: {}, result: {}", userId, roleName, result);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error checking role for userId {}: {}", request.getUserId(), e.getMessage());
            responseObserver.onError(e);
        }
    }
}
