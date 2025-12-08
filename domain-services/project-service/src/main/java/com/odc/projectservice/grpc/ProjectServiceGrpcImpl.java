package com.odc.projectservice.grpc;

import com.odc.common.constant.Role;
import com.odc.common.exception.BusinessException;
import com.odc.projectservice.entity.MilestoneMember;
import com.odc.projectservice.entity.ProjectMember;
import com.odc.projectservice.entity.ProjectMilestone;
import com.odc.projectservice.repository.MilestoneMemberRepository;
import com.odc.projectservice.repository.ProjectMemberRepository;
import com.odc.projectservice.repository.ProjectMilestoneRepository;
import com.odc.projectservice.v1.*;
import com.odc.userservice.v1.GetUsersByIdsRequest;
import com.odc.userservice.v1.GetUsersByIdsResponse;
import com.odc.userservice.v1.UserInfo;
import com.odc.userservice.v1.UserServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class ProjectServiceGrpcImpl extends ProjectServiceGrpc.ProjectServiceImplBase {
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMilestoneRepository projectMilestoneRepository;
    private final ManagedChannel userServiceChannel1;
    private final MilestoneMemberRepository milestoneMemberRepository;

    @Override
    public void getMilestoneById(GetMilestoneByIdRequest request,
                                 StreamObserver<GetMilestoneByIdResponse> responseObserver) {
        try {
            // Parse UUID
            UUID milestoneId = UUID.fromString(request.getMilestoneId());
            log.info("[gRPC] getMilestoneById({}) called", milestoneId);

            // Fetch milestone
            ProjectMilestone milestone = projectMilestoneRepository.findById(milestoneId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Milestone với ID '" + milestoneId + "' không tồn tại"
                    ));

            // Build response
            GetMilestoneByIdResponse response = GetMilestoneByIdResponse.newBuilder()
                    .setId(milestone.getId().toString())
                    .setTitle(milestone.getTitle())
                    .setAmount(milestone.getBudget() == null ? 0.0 : milestone.getBudget().doubleValue())
                    .setProjectId(milestone.getProject().getId().toString())
                    .build();

            log.info("[gRPC] getMilestoneById response = {}", response);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("[gRPC] Error in getMilestoneById: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }


    @Override
    public void getProjectCountByMentorIds(
            GetProjectCountByMentorIdsRequest request,
            StreamObserver<GetProjectCountByMentorIdsResponse> responseObserver) {
        try {
            List<UUID> mentorIds = request.getMentorIdsList().stream()
                    .map(UUID::fromString)
                    .toList();

            // Lấy tất cả ProjectMember có role MENTOR và userId trong danh sách
            List<ProjectMember> mentorMembers = projectMemberRepository.findAll().stream()
                    .filter(pm -> Role.MENTOR.toString().equalsIgnoreCase(pm.getRoleInProject()))
                    .filter(pm -> mentorIds.contains(pm.getUserId()))
                    .toList();

            // Đếm số dự án unique cho mỗi mentor
            Map<String, Long> projectCounts = mentorMembers.stream()
                    .collect(Collectors.groupingBy(
                            pm -> pm.getUserId().toString(),
                            Collectors.mapping(
                                    pm -> pm.getProject().getId(),
                                    Collectors.toSet()
                            )
                    ))
                    .entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> (long) e.getValue().size()
                    ));

            // Đảm bảo tất cả mentorIds đều có trong response (nếu không có dự án thì = 0)
            Map<String, Integer> result = mentorIds.stream()
                    .collect(Collectors.toMap(
                            UUID::toString,
                            id -> projectCounts.getOrDefault(id.toString(), 0L).intValue()
                    ));

            GetProjectCountByMentorIdsResponse response = GetProjectCountByMentorIdsResponse.newBuilder()
                    .putAllMentorProjectCounts(result)
                    .build();

            log.info("GetProjectCountByMentorIds response: {}", response);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in getProjectCountByMentorIds: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getLeaderInfo(GetLeaderInfoRequest request, StreamObserver<GetLeaderInfoResponse> responseObserver) {
        try {
            UUID milestoneId = UUID.fromString(request.getMilestoneId());
            ProjectMilestone projectMilestone = projectMilestoneRepository.findById(milestoneId)
                    .orElseThrow(() -> new BusinessException("Project milestone not found"));

            List<MilestoneMember> milestoneMembers =
                    milestoneMemberRepository.findByProjectMilestone_Id(milestoneId);

            String mentorLeaderId = null;
            String talentLeaderId = null;

            for (MilestoneMember member : milestoneMembers) {
                if (member.isLeader()) {
                    String role = member.getProjectMember().getRoleInProject();
                    String userId = member.getProjectMember().getUserId().toString();

                    if (Role.MENTOR.toString().equals(role)) {
                        mentorLeaderId = userId;
                    } else if (Role.TALENT.toString().equals(role)) {
                        talentLeaderId = userId;
                    }
                }
            }

            // --- FIX: Chỉ add userId nào KHÔNG null ---
            List<String> leaderUserIds = new ArrayList<>();
            if (mentorLeaderId != null) leaderUserIds.add(mentorLeaderId);
            if (talentLeaderId != null) leaderUserIds.add(talentLeaderId);

            // Không có leader nào
            if (leaderUserIds.isEmpty()) {
                GetLeaderInfoResponse response = GetLeaderInfoResponse.newBuilder()
                        .setMilestoneStatus(projectMilestone.getStatus())
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Call user service
            UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub =
                    UserServiceGrpc.newBlockingStub(userServiceChannel1);

            GetUsersByIdsRequest userRequest = GetUsersByIdsRequest.newBuilder()
                    .addAllUserId(leaderUserIds)
                    .build();

            GetUsersByIdsResponse userResponse = userServiceBlockingStub.getUsersByIds(userRequest);

            List<LeaderInfo> leaders = new ArrayList<>();

            for (UserInfo user : userResponse.getUsersList()) {
                String roleInProject =
                        user.getUserId().equals(mentorLeaderId) ? "MENTOR" : "TALENT";

                LeaderInfo leaderInfo = LeaderInfo.newBuilder()
                        .setUserId(user.getUserId())
                        .setFullName(user.getFullName())
                        .setEmail(user.getEmail())
                        .setAvatarUrl(user.getAvatarUrl())
                        .setIsLeader(true)
                        .setRoleInProject(roleInProject)
                        .build();

                leaders.add(leaderInfo);
            }

            GetLeaderInfoResponse response = GetLeaderInfoResponse.newBuilder()
                    .addAllLeaders(leaders)
                    .setMilestoneStatus(projectMilestone.getStatus())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(
                    Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException()
            );
        }
    }

}