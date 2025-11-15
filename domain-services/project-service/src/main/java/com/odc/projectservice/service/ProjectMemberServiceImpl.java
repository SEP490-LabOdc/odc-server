package com.odc.projectservice.service;

import com.odc.common.constant.Role;
import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.BusinessException;
import com.odc.projectservice.dto.request.AddBatchProjectMembersRequest;
import com.odc.projectservice.dto.response.MentorResponse;
import com.odc.projectservice.entity.Project;
import com.odc.projectservice.entity.ProjectMember;
import com.odc.projectservice.repository.ProjectMemberRepository;
import com.odc.projectservice.repository.ProjectRepository;
import com.odc.userservice.v1.CheckUsersInRoleRequest;
import com.odc.userservice.v1.GetMentorsWithProjectCountRequest;
import com.odc.userservice.v1.GetMentorsWithProjectCountResponse;
import com.odc.userservice.v1.UserServiceGrpc;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
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
    public ApiResponse<Void> addBatchProjectMembers(AddBatchProjectMembersRequest request) {
        UUID projectId = request.getProjectId();
        List<UUID> userIds = request.getUserIds();

        if (userIds == null || userIds.size() != 2) {
            throw new BusinessException("Chỉ được phép thêm đúng 2 mentor vào dự án. Số lượng userIds hiện tại: " +
                    (userIds == null ? 0 : userIds.size()));
        }

        if(projectMemberRepository.countMentorsInProject(request.getProjectId(), Role.MENTOR.toString()) > 2) {
            throw new BusinessException("Dự án chỉ được phép có tối đa 2 mentor.");
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
                    .isLeader(false)
                    .build();

            projectMemberList.add(projectMember);
            log.info("Đã thêm mentor {} vào dự án {}", userId, projectId);
        }

        projectMemberRepository.saveAll(projectMemberList);
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
                        .projectCount(mentorInfo.getProjectCount())
                        .build())
                .sorted(Comparator
                        .comparingInt(MentorResponse::getProjectCount)
                        .thenComparing(MentorResponse::getName))
                .toList();

        return ApiResponse.success("Lấy danh sách mentor khả dụng thành công", availableMentors);
    }
}