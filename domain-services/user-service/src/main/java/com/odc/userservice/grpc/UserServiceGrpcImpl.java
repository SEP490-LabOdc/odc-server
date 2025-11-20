package com.odc.userservice.grpc;

import com.odc.common.constant.Role;
import com.odc.common.exception.ResourceNotFoundException;
import com.odc.projectservice.v1.GetProjectCountByMentorIdsRequest;
import com.odc.projectservice.v1.GetProjectCountByMentorIdsResponse;
import com.odc.projectservice.v1.ProjectServiceGrpc;
import com.odc.userservice.entity.User;
import com.odc.userservice.v1.*;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@GrpcService
@Slf4j
@RequiredArgsConstructor

public class UserServiceGrpcImpl extends UserServiceGrpc.UserServiceImplBase {
    private final com.odc.userservice.repository.UserRepository userRepository;
    private final @Qualifier("projectServiceChannel") ManagedChannel projectServiceChannel;
    private final com.odc.userservice.repository.RoleRepository roleRepository;


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

    @Override
    public void getMentorsWithProjectCount(
            GetMentorsWithProjectCountRequest request,
            StreamObserver<GetMentorsWithProjectCountResponse> responseObserver) {
        try {

            List<User> mentors = userRepository.findAll().stream()
                    .filter(user -> user.getRole() != null &&
                            Role.MENTOR.toString().equalsIgnoreCase(user.getRole().getName()))
                    .toList();

            if (mentors.isEmpty()) {
                GetMentorsWithProjectCountResponse response = GetMentorsWithProjectCountResponse.newBuilder()
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            List<String> mentorIds = mentors.stream()
                    .map(u -> u.getId().toString())
                    .toList();


            ProjectServiceGrpc.ProjectServiceBlockingStub projectStub =
                    ProjectServiceGrpc.newBlockingStub(projectServiceChannel);

            GetProjectCountByMentorIdsRequest projectRequest = GetProjectCountByMentorIdsRequest.newBuilder()
                    .addAllMentorIds(mentorIds)
                    .build();

            GetProjectCountByMentorIdsResponse projectResponse = projectStub.getProjectCountByMentorIds(projectRequest);
            Map<String, Integer> projectCounts = projectResponse.getMentorProjectCountsMap();

            // Build response
            GetMentorsWithProjectCountResponse.Builder responseBuilder =
                    GetMentorsWithProjectCountResponse.newBuilder();

            for (User mentor : mentors) {
                String mentorId = mentor.getId().toString();
                int projectCount = projectCounts.getOrDefault(mentorId, 0);

                MentorInfo mentorInfo = MentorInfo.newBuilder()
                        .setId(mentorId)
                        .setName(mentor.getFullName())
                        .setEmail(mentor.getEmail())
                        .setProjectCount(projectCount)
                        .build();

                responseBuilder.addMentors(mentorInfo);
            }

            log.info("GetMentorsWithProjectCount response: {} mentors", responseBuilder.getMentorsCount());
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in getMentorsWithProjectCount: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getRoleIdByUserId(
            GetRoleIdByUserIdRequest request,
            StreamObserver<GetRoleIdByUserIdResponse> responseObserver) {
        try {
            UUID userId = UUID.fromString(request.getUserId());
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

            if (user.getRole() == null) {
                responseObserver.onError(new RuntimeException("User does not have a role"));
                return;
            }

            GetRoleIdByUserIdResponse response = GetRoleIdByUserIdResponse.newBuilder()
                    .setRoleId(user.getRole().getId().toString())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting roleId for userId {}: {}", request.getUserId(), e.getMessage());
            responseObserver.onError(e);
        }
    }

    @Override
    public void checkUsersInRole(CheckUsersInRoleRequest request, StreamObserver<CheckUsersInRoleResponse> responseObserver) {
        try {
            List<UUID> userIds = request.getUserIdsList().stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toList());
            String roleName = request.getRoleName();

            List<User> users = userRepository.findAllById(userIds);

            Map<String, Boolean> result = users.stream()
                    .collect(Collectors.toMap(
                            u -> u.getId().toString(),
                            u -> u.getRole() != null && u.getRole().getName().equalsIgnoreCase(roleName)
                    ));

            CheckUsersInRoleResponse response = CheckUsersInRoleResponse.newBuilder()
                    .putAllResults(result)
                    .build();

            log.info("checkUsersInRole response: {}", result);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in checkUsersInRole: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getUsersByIds(
            GetUsersByIdsRequest request,
            StreamObserver<GetUsersByIdsResponse> responseObserver) {

        try {
            List<UUID> userIds = request.getUserIdList().stream()
                    .map(UUID::fromString)
                    .toList();

            List<User> users = userRepository.findByIdIn(userIds);

            List<com.odc.userservice.v1.UserInfo> userInfoList = users.stream()
                    .map(user -> com.odc.userservice.v1.UserInfo.newBuilder()
                            .setUserId(user.getId().toString())
                            .setFullName(user.getFullName())
                            .setEmail(user.getEmail())
                            .setPhone(user.getPhone() != null ? user.getPhone() : "")
                            .setAvatarUrl(user.getAvatarUrl() != null ? user.getAvatarUrl() : "")
                            .build())
                    .toList();

            GetUsersByIdsResponse response = GetUsersByIdsResponse.newBuilder()
                    .addAllUsers(userInfoList)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

}
