package com.odc.projectservice.service;

import com.odc.common.constant.Role;
import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.BusinessException;
import com.odc.projectservice.dto.request.AddBatchProjectMembersRequest;
import com.odc.projectservice.dto.response.AddBatchProjectMembersResponse;
import com.odc.projectservice.entity.Project;
import com.odc.projectservice.entity.ProjectMember;
import com.odc.projectservice.repository.ProjectMemberRepository;
import com.odc.projectservice.repository.ProjectRepository;
import com.odc.userservice.dto.response.MentorResponse;
import com.odc.userservice.v1.*;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ProjectMemberServiceImpl implements ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final ManagedChannel userServiceChannel;

    public ProjectMemberServiceImpl(
            ProjectMemberRepository projectMemberRepository,
            ProjectRepository projectRepository,
            @Qualifier("userServiceChannel1") ManagedChannel userServiceChannel) {
        this.projectMemberRepository = projectMemberRepository;
        this.projectRepository = projectRepository;
        this.userServiceChannel = userServiceChannel;
    }

    @Override
    public ApiResponse<AddBatchProjectMembersResponse> addBatchProjectMembers(AddBatchProjectMembersRequest request) {
        UUID projectId = request.getProjectId();
        List<UUID> userIds = request.getUserIds();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("Dự án với ID '" + projectId + "' không tồn tại"));


        if (userIds == null || userIds.size() != 2) {
            throw new BusinessException("Chỉ được phép thêm đúng 2 mentor vào dự án. Số lượng userIds hiện tại: " +
                    (userIds == null ? 0 : userIds.size()));
        }

        UserServiceGrpc.UserServiceBlockingStub userStub =
                UserServiceGrpc.newBlockingStub(userServiceChannel);


        List<UUID> invalidUserIds = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        for (UUID userId : userIds) {
            List<String> userErrors = new ArrayList<>();

            CheckRoleByUserIdRequest checkRoleRequest = CheckRoleByUserIdRequest.newBuilder()
                    .setUserId(userId.toString())
                    .setRoleName(Role.MENTOR.toString())
                    .build();

            CheckRoleByUserIdResponse checkRoleResponse = userStub.checkRoleByUserId(checkRoleRequest);
            if (!checkRoleResponse.getResult()) {
                userErrors.add("User " + userId + " không có role MENTOR");
            }

            long projectCount = projectMemberRepository.countByUserId(userId);
            if (projectCount >= 2) {
                userErrors.add("Mentor " + userId + " đã có " + projectCount + " dự án (tối đa 2)");
            }

            boolean alreadyMember = projectMemberRepository.existsByUserIdAndProjectId(userId, projectId);
            if (alreadyMember) {
                userErrors.add("User " + userId + " đã là thành viên của dự án này");
            }


            try {
                GetRoleIdByUserIdRequest getRoleIdRequest = GetRoleIdByUserIdRequest.newBuilder()
                        .setUserId(userId.toString())
                        .build();

                GetRoleIdByUserIdResponse roleIdResponse = userStub.getRoleIdByUserId(getRoleIdRequest);
                UUID roleId = UUID.fromString(roleIdResponse.getRoleId());

                CheckRoleIdExistsRequest checkRoleIdExistsRequest = CheckRoleIdExistsRequest.newBuilder()
                        .setRoleId(roleId.toString())
                        .build();

                CheckRoleIdExistsResponse checkRoleIdResponse = userStub.checkRoleIdExists(checkRoleIdExistsRequest);
                if (!checkRoleIdResponse.getExists()) {
                    userErrors.add("RoleId không tồn tại cho user " + userId);
                }
            } catch (Exception e) {
                userErrors.add("Không thể lấy roleId cho user " + userId + ": " + e.getMessage());
            }

            if (!userErrors.isEmpty()) {
                invalidUserIds.add(userId);
                errorMessages.addAll(userErrors);
            }
        }

        if (!invalidUserIds.isEmpty()) {
            String errorMessage = String.format(
                    "Không thể thêm mentor vào dự án. Các userId không hợp lệ: %s. Chi tiết lỗi: %s",
                    invalidUserIds,
                    String.join("; ", errorMessages)
            );
            throw new BusinessException(errorMessage);
        }

        List<UUID> addedUserIds = new ArrayList<>();
        for (UUID userId : userIds) {
            try {
                ProjectMember projectMember = ProjectMember.builder()
                        .userId(userId)
                        .project(project)
                        .roleInProject(Role.MENTOR.toString())
                        .isLeader(false)
                        .build();

                projectMemberRepository.save(projectMember);
                addedUserIds.add(userId);
                log.info("Đã thêm mentor {} vào dự án {}", userId, projectId);
            } catch (Exception e) {
                log.error("Lỗi khi thêm mentor {} vào dự án: {}", userId, e.getMessage());
                throw new BusinessException("Lỗi khi thêm mentor " + userId + " vào dự án: " + e.getMessage());
            }
        }

        AddBatchProjectMembersResponse response = AddBatchProjectMembersResponse.builder()
                .addedUserIds(addedUserIds)
                .skippedUserIds(new ArrayList<>())
                .totalAdded(addedUserIds.size())
                .totalSkipped(0)
                .build();

        String message = String.format("Đã thêm %d mentor vào dự án thành công.", addedUserIds.size());

        return ApiResponse.success(message, response);
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
                        .projectCount(mentorInfo.getProjectCount())
                        .build())
                .toList();

        return ApiResponse.success("Lấy danh sách mentor khả dụng thành công", availableMentors);
    }
}