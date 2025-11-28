package com.odc.projectservice.service;

import com.odc.common.constant.ProjectStatus;
import com.odc.common.constant.Role;
import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.BusinessException;
import com.odc.projectservice.dto.request.AddBatchProjectMembersRequest;
import com.odc.projectservice.dto.request.ToggleMentorLeaderRequest;
import com.odc.projectservice.dto.response.GetProjectMemberByProjectIdResponse;
import com.odc.projectservice.dto.response.MentorResponse;
import com.odc.projectservice.entity.Project;
import com.odc.projectservice.entity.ProjectMember;
import com.odc.projectservice.repository.ProjectMemberRepository;
import com.odc.projectservice.repository.ProjectRepository;
import com.odc.userservice.v1.*;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
                    .isLeader(false)
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
    public ApiResponse<List<GetProjectMemberByProjectIdResponse>> getProjectMembersByProjectId(UUID projectId) {
        List<ProjectMember> projectMemberList = projectMemberRepository.findByProjectId(projectId);

        if (projectMemberList.isEmpty()) {
            return ApiResponse.success(List.of());
        }

        List<String> userIds = projectMemberList.stream()
                .map(pm -> pm.getUserId().toString())
                .toList();

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
                        u -> u
                ));

        List<GetProjectMemberByProjectIdResponse> result = projectMemberList.stream().map(pm -> {

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
            dto.setIsLeader(pm.isLeader());
            dto.setJoinedAt(pm.getJoinedAt());
            dto.setLeftAt(pm.getLeftAt());
            dto.setIsActive(pm.getLeftAt() == null);

            return dto;
        }).sorted(
                Comparator.comparing(GetProjectMemberByProjectIdResponse::getIsLeader).reversed()
                        .thenComparing(dto -> {
                            ProjectMember pm = projectMemberList.stream()
                                    .filter(p -> p.getId().equals(dto.getProjectMemberId()))
                                    .findFirst()
                                    .orElse(null);
                            return pm != null ? pm.getRoleInProject() : "";
                        }, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(GetProjectMemberByProjectIdResponse::getJoinedAt,
                                Comparator.nullsLast(LocalDateTime::compareTo))
        ).toList();
        return ApiResponse.success(result);
    }

    @Override
    public ApiResponse<UUID> toggleMentorLeader(UUID projectId, UUID mentorId, ToggleMentorLeaderRequest request) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("Dự án với ID: '" + projectId + "' không tồn tại"));

        ProjectMember mentorMember = projectMemberRepository
                .findByProjectIdAndUserIdAndRole(projectId, mentorId, Role.MENTOR.toString());

        if (mentorMember == null) {
            throw new BusinessException("Mentor not in project");
        }

        mentorMember.setLeader(request.getIsLeader());
        projectMemberRepository.save(mentorMember);

        log.info("Đã cập nhật isLeader={} cho mentor {} trong dự án {}",
                request.getIsLeader(), mentorId, projectId);

        return ApiResponse.success("Đã cập nhật trạng thái leader thành công", mentorMember.getId());
    }

    @Override
    public ApiResponse<UUID> toggleTalentLeader(UUID projectId, UUID talentId, ToggleMentorLeaderRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("Dự án với ID: '" + projectId + "' không tồn tại"));

        ProjectMember talentMember = projectMemberRepository
                .findByProjectIdAndUserIdAndRole(projectId, talentId, Role.TALENT.toString());

        if (talentMember == null) {
            throw new BusinessException("Talent với ID: '" + talentId + "' không thuộc dự án này");
        }

        UUID requesterId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ProjectMember requesterMember = projectMemberRepository
                .findByProjectIdAndUserIdAndRole(projectId, requesterId, Role.MENTOR.toString());

        if (requesterMember == null) {
            throw new BusinessException("Bạn không phải là mentor của dự án này");
        }

        if (Boolean.TRUE.equals(request.getIsLeader())) {
            List<ProjectMember> existingLeaders = projectMemberRepository
                    .findByProjectIdAndRoleAndIsLeaderTrue(projectId, Role.TALENT.toString());

            for (ProjectMember leader : existingLeaders) {
                if (!leader.getUserId().equals(talentId)) {
                    leader.setLeader(false);
                    projectMemberRepository.save(leader);
                    log.info("Đã bỏ leader role từ talent {} trong dự án {}", leader.getUserId(), projectId);
                }
            }

            talentMember.setLeader(true);
            projectMemberRepository.save(talentMember);
            log.info("Đã đặt talent {} làm leader trong dự án {}", talentId, projectId);
        } else {
            talentMember.setLeader(false);
            projectMemberRepository.save(talentMember);
            log.info("Đã bỏ leader role từ talent {} trong dự án {}", talentId, projectId);
        }

        return ApiResponse.success("Đã cập nhật trạng thái leader thành công", talentMember.getId());
    }
}