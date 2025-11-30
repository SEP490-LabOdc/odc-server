package com.odc.projectservice.service;

import com.odc.common.constant.Role;
import com.odc.common.constant.Status;
import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.BusinessException;
import com.odc.fileservice.v1.FileInfo;
import com.odc.fileservice.v1.FileServiceGrpc;
import com.odc.fileservice.v1.GetFilesByEntityIdRequest;
import com.odc.fileservice.v1.GetFilesByEntityIdResponse;
import com.odc.projectservice.dto.request.CreateProjectMilestoneRequest;
import com.odc.projectservice.dto.request.UpdateProjectMilestoneRequest;
import com.odc.projectservice.dto.response.MilestoneDocumentResponse;
import com.odc.projectservice.dto.response.ProjectMilestoneResponse;
import com.odc.projectservice.dto.response.TalentMentorInfoResponse;
import com.odc.projectservice.entity.Project;
import com.odc.projectservice.entity.ProjectMember;
import com.odc.projectservice.entity.ProjectMilestone;
import com.odc.projectservice.repository.ProjectMemberRepository;
import com.odc.projectservice.repository.ProjectMilestoneRepository;
import com.odc.projectservice.repository.ProjectRepository;
import com.odc.userservice.v1.GetUsersByIdsRequest;
import com.odc.userservice.v1.GetUsersByIdsResponse;
import com.odc.userservice.v1.UserInfo;
import com.odc.userservice.v1.UserServiceGrpc;
import io.grpc.ManagedChannel;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ProjectMilestoneServiceImpl implements ProjectMilestoneService {
    private final ProjectMilestoneRepository projectMilestoneRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final @Qualifier("userServiceChannel1") ManagedChannel userServiceChannel;
    private final @Qualifier("fileServiceChannel") ManagedChannel fileServiceChannel;

    @Override
    public ApiResponse<ProjectMilestoneResponse> createProjectMilestone(CreateProjectMilestoneRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new BusinessException("Dự án với ID '" + request.getProjectId() + "' không tồn tại"));

        Optional<ProjectMilestone> latestMilestone = projectMilestoneRepository.findLatestByProjectId(request.getProjectId());
        if (latestMilestone.isPresent()) {
            ProjectMilestone latest = latestMilestone.get();
            if (!Status.COMPLETED.toString().equalsIgnoreCase(latest.getStatus())) {
                throw new BusinessException("Không thể tạo milestone mới. Milestone mới nhất chưa hoàn thành (status: " + latest.getStatus() + ")");
            }
        }

        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new BusinessException("Ngày bắt đầu không được sau ngày kết thúc");
            }
        }

        ProjectMilestone projectMilestone = ProjectMilestone.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(Status.PENDING.toString())
                .project(project)
                .attachmentUrls(request.getAttachmentUrls() != null ? request.getAttachmentUrls() : List.of())
                .build();

        ProjectMilestone savedMilestone = projectMilestoneRepository.save(projectMilestone);

        ProjectMilestoneResponse responseData = ProjectMilestoneResponse.builder()
                .id(savedMilestone.getId())
                .projectId(savedMilestone.getProject().getId())
                .title(savedMilestone.getTitle())
                .description(savedMilestone.getDescription())
                .startDate(savedMilestone.getStartDate())
                .endDate(savedMilestone.getEndDate())
                .status(savedMilestone.getStatus())
                .attachmentUrls(savedMilestone.getAttachmentUrls() != null ? savedMilestone.getAttachmentUrls() : List.of())
                .build();

        return ApiResponse.success("Tạo milestone dự án thành công", responseData);
    }

    @Override
    public ApiResponse<ProjectMilestoneResponse> updateProjectMilestone(UUID milestoneId, UpdateProjectMilestoneRequest request) {
        ProjectMilestone existingMilestone = projectMilestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new BusinessException("Milestone với ID '" + milestoneId + "' không tồn tại"));

        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new BusinessException("Ngày bắt đầu không được sau ngày kết thúc");
            }
        }

        existingMilestone.setTitle(request.getTitle());
        existingMilestone.setDescription(request.getDescription());
        existingMilestone.setStartDate(request.getStartDate());
        existingMilestone.setEndDate(request.getEndDate());
        existingMilestone.setStatus(request.getStatus());

        if (request.getAttachmentUrls() != null) {
            existingMilestone.setAttachmentUrls(request.getAttachmentUrls());
        }

        ProjectMilestone updatedMilestone = projectMilestoneRepository.save(existingMilestone);

        ProjectMilestoneResponse responseData = ProjectMilestoneResponse.builder()
                .id(updatedMilestone.getId())
                .projectId(updatedMilestone.getProject().getId())
                .title(updatedMilestone.getTitle())
                .description(updatedMilestone.getDescription())
                .startDate(updatedMilestone.getStartDate())
                .endDate(updatedMilestone.getEndDate())
                .status(updatedMilestone.getStatus())
                .attachmentUrls(updatedMilestone.getAttachmentUrls() != null ? updatedMilestone.getAttachmentUrls() : List.of())
                .build();

        return ApiResponse.success("Cập nhật milestone dự án thành công", responseData);
    }

    @Override
    public ApiResponse<List<ProjectMilestoneResponse>> getAllProjectMilestones() {
        List<ProjectMilestone> milestones = projectMilestoneRepository.findAll();

        List<ProjectMilestoneResponse> milestoneResponses = milestones.stream()
                .map(this::convertToProjectMilestoneResponse)
                .collect(Collectors.toList());

        return ApiResponse.<List<ProjectMilestoneResponse>>builder()
                .success(true)
                .message("Lấy danh sách milestone dự án thành công")
                .timestamp(LocalDateTime.now())
                .data(milestoneResponses)
                .build();
    }

    @Override
    public ApiResponse<List<ProjectMilestoneResponse>> getAllProjectMilestonesByProjectId(UUID projectId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("Dự án với ID '" + projectId + "' không tồn tại"));

        List<ProjectMilestone> milestones = projectMilestoneRepository.findByProjectId(projectId);

        List<ProjectMilestoneResponse> milestoneResponses = milestones.stream()
                .map(this::convertToProjectMilestoneResponse)
                .collect(Collectors.toList());

        return ApiResponse.<List<ProjectMilestoneResponse>>builder()
                .success(true)
                .message("Lấy danh sách milestone dự án thành công")
                .timestamp(LocalDateTime.now())
                .data(milestoneResponses)
                .build();
    }

    @Override
    public ApiResponse<ProjectMilestoneResponse> getProjectMilestoneById(UUID milestoneId) {
        ProjectMilestone milestone = projectMilestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new BusinessException("Milestone với ID '" + milestoneId + "' không tồn tại"));

        Project project = milestone.getProject();

        String projectName = project.getTitle();

        List<ProjectMember> projectMembers = projectMemberRepository.findByProjectId(project.getId());

        List<ProjectMember> talentMembers = projectMembers.stream()
                .filter(pm -> Role.TALENT.toString().equalsIgnoreCase(pm.getRoleInProject()))
                .collect(Collectors.toList());

        List<ProjectMember> mentorMembers = projectMembers.stream()
                .filter(pm -> Role.MENTOR.toString().equalsIgnoreCase(pm.getRoleInProject()))
                .collect(Collectors.toList());

        List<String> allUserIds = projectMembers.stream()
                .map(pm -> pm.getUserId().toString())
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, UserInfo> userMap = new HashMap<>();
        if (!allUserIds.isEmpty()) {
            try {
                UserServiceGrpc.UserServiceBlockingStub userStub =
                        UserServiceGrpc.newBlockingStub(userServiceChannel);
                GetUsersByIdsResponse usersResponse = userStub.getUsersByIds(
                        GetUsersByIdsRequest.newBuilder()
                                .addAllUserId(allUserIds)
                                .build()
                );

                userMap = usersResponse.getUsersList().stream()
                        .collect(Collectors.toMap(
                                u -> UUID.fromString(u.getUserId()),
                                u -> u
                        ));
            } catch (Exception e) {
                System.err.println("Lỗi khi lấy thông tin user: " + e.getMessage());
            }
        }

        final Map<UUID, UserInfo> finalUserMap = userMap;
        List<TalentMentorInfoResponse> talents = talentMembers.stream()
                .map(pm -> {
                    UserInfo userInfo = finalUserMap.get(pm.getUserId());
                    if (userInfo != null) {
                        return TalentMentorInfoResponse.builder()
                                .userId(pm.getUserId())
                                .name(userInfo.getFullName())
                                .avatar(userInfo.getAvatarUrl())
                                .email(userInfo.getEmail())
                                .phone(userInfo.getPhone())
                                .build();
                    }
                    return TalentMentorInfoResponse.builder()
                            .userId(pm.getUserId())
                            .name("Unknown")
                            .avatar("")
                            .email("")
                            .phone("")
                            .build();
                })
                .collect(Collectors.toList());

        List<TalentMentorInfoResponse> mentors = mentorMembers.stream()
                .map(pm -> {
                    UserInfo userInfo = finalUserMap.get(pm.getUserId());
                    if (userInfo != null) {
                        return TalentMentorInfoResponse.builder()
                                .userId(pm.getUserId())
                                .name(userInfo.getFullName())
                                .avatar(userInfo.getAvatarUrl())
                                .email(userInfo.getEmail())
                                .phone(userInfo.getPhone())
                                .build();
                    }
                    return TalentMentorInfoResponse.builder()
                            .userId(pm.getUserId())
                            .name("Unknown")
                            .avatar("")
                            .email("")
                            .phone("")
                            .build();
                })
                .collect(Collectors.toList());

        ProjectMilestoneResponse responseData = ProjectMilestoneResponse.builder()
                .id(milestone.getId())
                .projectId(milestone.getProject().getId())
                .projectName(projectName)
                .title(milestone.getTitle())
                .description(milestone.getDescription())
                .startDate(milestone.getStartDate())
                .endDate(milestone.getEndDate())
                .status(milestone.getStatus())
                .attachmentUrls(milestone.getAttachmentUrls() != null ? milestone.getAttachmentUrls() : List.of())
                .talents(talents)
                .mentors(mentors)
                .build();

        return ApiResponse.<ProjectMilestoneResponse>builder()
                .success(true)
                .message("Lấy thông tin milestone dự án thành công")
                .timestamp(LocalDateTime.now())
                .data(responseData)
                .build();
    }

    @Override
    public ApiResponse<Void> deleteProjectMilestone(UUID milestoneId) {
        ProjectMilestone milestone = projectMilestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new BusinessException("Milestone với ID '" + milestoneId + "' không tồn tại"));

        projectMilestoneRepository.delete(milestone);

        return ApiResponse.<Void>builder()
                .success(true)
                .message("Xóa milestone dự án thành công")
                .timestamp(LocalDateTime.now())
                .data(null)
                .build();
    }

    @Override
    public ApiResponse<List<MilestoneDocumentResponse>> getDocumentsByMilestoneId(UUID milestoneId) {
        try {

            FileServiceGrpc.FileServiceBlockingStub stub =
                    FileServiceGrpc.newBlockingStub(fileServiceChannel);

            GetFilesByEntityIdRequest request = GetFilesByEntityIdRequest.newBuilder()
                    .setEntityId(milestoneId.toString())
                    .build();

            GetFilesByEntityIdResponse grpcResponse = stub.getFilesByEntityId(request);

            List<MilestoneDocumentResponse> documents = grpcResponse.getFilesList().stream()
                    .map(this::convertToMilestoneDocumentResponse)
                    .collect(Collectors.toList());

            return ApiResponse.<List<MilestoneDocumentResponse>>builder()
                    .success(true)
                    .message("Lấy danh sách documents của milestone thành công")
                    .timestamp(LocalDateTime.now())
                    .data(documents)
                    .build();

        } catch (Exception e) {
            log.error("Lỗi khi gọi file-service gRPC để lấy documents: {}", e.getMessage(), e);
            return ApiResponse.<List<MilestoneDocumentResponse>>builder()
                    .success(false)
                    .message("Lỗi khi lấy danh sách documents: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .data(List.of())
                    .build();
        }
    }

    private MilestoneDocumentResponse convertToMilestoneDocumentResponse(FileInfo fileInfo) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        return MilestoneDocumentResponse.builder()
                .id(UUID.fromString(fileInfo.getId()))
                .fileName(fileInfo.getFileName())
                .fileUrl(fileInfo.getFileUrl())
                .s3Key(fileInfo.getS3Key())
                .uploadedAt(LocalDateTime.parse(fileInfo.getUploadedAt(), formatter))
                .entityId(fileInfo.getEntityId())
                .build();
    }

    private ProjectMilestoneResponse convertToProjectMilestoneResponse(ProjectMilestone milestone) {
        return ProjectMilestoneResponse.builder()
                .id(milestone.getId())
                .projectId(milestone.getProject().getId())
                .title(milestone.getTitle())
                .description(milestone.getDescription())
                .startDate(milestone.getStartDate())
                .endDate(milestone.getEndDate())
                .status(milestone.getStatus())
                .attachmentUrls(milestone.getAttachmentUrls() != null ? milestone.getAttachmentUrls() : List.of())
                .build();
    }
}
