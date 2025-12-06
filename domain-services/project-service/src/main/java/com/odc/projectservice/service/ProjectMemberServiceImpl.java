package com.odc.projectservice.service;

import com.odc.common.constant.ProjectStatus;
import com.odc.common.constant.Role;
import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.BusinessException;
import com.odc.projectservice.dto.request.AddBatchProjectMembersRequest;
import com.odc.projectservice.dto.response.GetProjectMemberByProjectIdResponse;
import com.odc.projectservice.dto.response.MentorResponse;
import com.odc.projectservice.entity.MilestoneMember;
import com.odc.projectservice.entity.Project;
import com.odc.projectservice.entity.ProjectMember;
import com.odc.projectservice.repository.MilestoneMemberRepository;
import com.odc.projectservice.repository.ProjectMemberRepository;
import com.odc.projectservice.repository.ProjectRepository;
import com.odc.userservice.v1.*;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProjectMemberServiceImpl implements ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final MilestoneMemberRepository milestoneMemberRepository;
    private final ManagedChannel userServiceChannel;

    public ProjectMemberServiceImpl(
            ProjectMemberRepository projectMemberRepository,
            ProjectRepository projectRepository,
            MilestoneMemberRepository milestoneMemberRepository,
            @Qualifier("userServiceChannel1") ManagedChannel userServiceChannel) {
        this.projectMemberRepository = projectMemberRepository;
        this.projectRepository = projectRepository;
        this.milestoneMemberRepository = milestoneMemberRepository;
        this.userServiceChannel = userServiceChannel;
    }

    @Override
    public ApiResponse<Void> addBatchProjectMembers(AddBatchProjectMembersRequest request) {
        UUID projectId = request.getProjectId();
        List<UUID> userIds = request.getUserIds();

        if (userIds == null || userIds.isEmpty() || userIds.size() > 2) {
            throw new BusinessException("Chỉ được phép thêm từ 1 đến 2 mentor vào dự án. Số lượng userIds hiện tại: " +
                    (userIds == null ? 0 : userIds.size()));
        }

        long currentMentorCount = projectMemberRepository.countMentorsInProject(request.getProjectId(), Role.MENTOR.toString());
        long newMentorCount = userIds.size();
        if (currentMentorCount + newMentorCount > 2) {
            throw new BusinessException("Dự án chỉ được phép có tối đa 2 mentor. Hiện tại có " + currentMentorCount +
                    " mentor, không thể thêm " + newMentorCount + " mentor mới.");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("Dự án với ID: '" + projectId + "' không tồn tại"));

        UserServiceGrpc.UserServiceBlockingStub userStub =
                UserServiceGrpc.newBlockingStub(userServiceChannel);

        CheckUsersInRoleRequest checkRoleByUserIdRequest = CheckUsersInRoleRequest
                .newBuilder()
                .addAllUserIds(userIds
                        .stream()
                        .map(UUID::toString)
                        .toList())
                .setRoleName(Role.MENTOR.toString())
                .build();

        userStub.checkUsersInRole(checkRoleByUserIdRequest)
                .getResultsMap()
                .forEach((id, isMentor) -> {
                    if (!isMentor) {
                        throw new BusinessException("User với ID: " + id + " không phải mentor");
                    }
                });

        List<ProjectMember> projectMemberList = new ArrayList<>();
        for (UUID userId : userIds) {
            long projectCount = projectMemberRepository.countByUserId(userId);
            if (projectCount >= 2) {
                throw new BusinessException("Mentor với ID: " + userId + " đã có " + projectCount + " dự án (tối đa 2)");
            }

            boolean alreadyMember = projectMemberRepository.existsByUserIdAndProjectId(userId, projectId);
            if (alreadyMember) {
                throw new BusinessException("Mentor với ID:  " + userId + " đã là thành viên của dự án này");
            }

            ProjectMember projectMember = ProjectMember.builder()
                    .userId(userId)
                    .project(project)
                    .roleInProject(Role.MENTOR.toString())
                    .build();

            projectMemberList.add(projectMember);
            log.info("Đã thêm mentor {} vào dự án {}", userId, projectId);
        }

        projectMemberRepository.saveAll(projectMemberList);

        long totalMentorCount = projectMemberRepository.countMentorsInProject(projectId, Role.MENTOR.toString());
        if (totalMentorCount >= 1 && totalMentorCount <= 2) {
            project.setStatus(ProjectStatus.PLANNING.toString());
            projectRepository.save(project);
            log.info("Đã cập nhật trạng thái dự án {} sang PLANNING vì có {} mentor", projectId, totalMentorCount);
        }
        return ApiResponse.success("Đã thêm thành công mentor vào dự án.", null);
    }

    @Override
    public ApiResponse<List<MentorResponse>> getAvailableMentors(UUID projectId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("Dự án với ID '" + projectId + "' không tồn tại"));

        // Lấy tất cả mentors với project count
        UserServiceGrpc.UserServiceBlockingStub userStub =
                UserServiceGrpc.newBlockingStub(userServiceChannel);

        GetMentorsWithProjectCountRequest request = GetMentorsWithProjectCountRequest.newBuilder()
                .build();

        GetMentorsWithProjectCountResponse response = userStub.getMentorsWithProjectCount(request);

        // Lọc mentors có < 2 dự án và chưa là member của project này
        List<MentorResponse> availableMentors = response.getMentorsList().stream()
                .filter(mentorInfo -> {
                    UUID mentorId = UUID.fromString(mentorInfo.getId());
                    int projectCount = mentorInfo.getProjectCount();

                    if (projectCount >= 2) {
                        return false;
                    }

                    boolean alreadyMember = projectMemberRepository.existsByUserIdAndProjectId(mentorId, projectId);
                    return !alreadyMember;
                })
                .map(mentorInfo -> MentorResponse.builder()
                        .id(UUID.fromString(mentorInfo.getId()))
                        .name(mentorInfo.getName())
                        .email(mentorInfo.getEmail())
                        .avatarUrl(Optional.of(mentorInfo.getAvatarUrl()).orElse(""))
                        .projectCount(mentorInfo.getProjectCount())
                        .build())
                .sorted(Comparator
                        .comparingInt(MentorResponse::getProjectCount)
                        .thenComparing(MentorResponse::getName))
                .toList();

        return ApiResponse.success("Lấy danh sách mentor khả dụng thành công", availableMentors);
    }

    @Override
    public ApiResponse<List<GetProjectMemberByProjectIdResponse>> getProjectMembersByProjectId(UUID projectId, UUID milestoneId) {
        // 1. Lấy danh sách và khử trùng ngay lập tức dựa trên ProjectMember ID
        List<ProjectMember> rawMembers = projectMemberRepository.findByProjectId(projectId);

        // Sử dụng Map để loại bỏ các bản ghi trùng lặp (nếu có) dựa trên ID
        List<ProjectMember> projectMemberList = new ArrayList<>(
                rawMembers.stream()
                        .collect(Collectors.toMap(
                                ProjectMember::getId,
                                pm -> pm,
                                (existing, replacement) -> existing
                        ))
                        .values()
        );

        // 2. Logic lọc thành viên chưa tham gia Milestone (giữ nguyên logic của bạn)
        if (milestoneId != null) {
            List<MilestoneMember> milestoneMembers = milestoneMemberRepository.findByProjectMilestone_Id(milestoneId);

            Set<UUID> talentProjectMemberIdsInMilestone = milestoneMembers.stream()
                    .map(MilestoneMember::getProjectMember)
                    .filter(pm -> Role.TALENT.toString().equalsIgnoreCase(pm.getRoleInProject()))
                    .map(ProjectMember::getId)
                    .collect(Collectors.toSet());

            projectMemberList = projectMemberList.stream()
                    .filter(pm -> {
                        if (Role.TALENT.toString().equalsIgnoreCase(pm.getRoleInProject())) {
                            return !talentProjectMemberIdsInMilestone.contains(pm.getId());
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        if (projectMemberList.isEmpty()) {
            return ApiResponse.success(List.of());
        }

        // 3. Lấy danh sách User ID và khử trùng (distinct) để tối ưu gRPC call
        List<String> userIds = projectMemberList.stream()
                .map(pm -> pm.getUserId().toString())
                .distinct() // Thêm distinct để tránh gửi trùng ID sang User Service
                .toList();

        // 4. Gọi gRPC lấy thông tin User
        UserServiceGrpc.UserServiceBlockingStub stub =
                UserServiceGrpc.newBlockingStub(userServiceChannel);

        GetUsersByIdsResponse usersResponse = stub.getUsersByIds(
                GetUsersByIdsRequest.newBuilder()
                        .addAllUserId(userIds)
                        .build()
        );

        Map<UUID, UserInfo> userMap = usersResponse.getUsersList().stream()
                .collect(Collectors.toMap(
                        u -> UUID.fromString(u.getUserId()),
                        u -> u,
                        (existing, replacement) -> existing // Handle trường hợp user map bị trùng key
                ));

        // 5. Map sang DTO
        List<GetProjectMemberByProjectIdResponse> result = projectMemberList.stream()
                .map(pm -> {
                    UserInfo userInfo = userMap.get(pm.getUserId());

                    GetProjectMemberByProjectIdResponse dto = new GetProjectMemberByProjectIdResponse();
                    dto.setProjectMemberId(pm.getId());
                    dto.setUserId(pm.getUserId());

                    if (userInfo != null) {
                        dto.setFullName(userInfo.getFullName());
                        dto.setEmail(userInfo.getEmail());
                        dto.setPhone(userInfo.getPhone());
                        dto.setAvatarUrl(userInfo.getAvatarUrl());
                    }

                    dto.setRoleName(pm.getRoleInProject());
                    dto.setJoinedAt(pm.getJoinedAt());
                    dto.setLeftAt(pm.getLeftAt());
                    dto.setIsActive(pm.getLeftAt() == null);

                    return dto;
                })
                .sorted(Comparator.comparing(GetProjectMemberByProjectIdResponse::getJoinedAt,
                        Comparator.nullsLast(LocalDateTime::compareTo)))
                .toList();

        return ApiResponse.success(result);
    }
}