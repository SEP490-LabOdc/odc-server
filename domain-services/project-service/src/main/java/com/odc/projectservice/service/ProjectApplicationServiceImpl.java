package com.odc.projectservice.service;

import com.odc.common.constant.ProjectApplicationStatus;
import com.odc.common.constant.Role;
import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.BusinessException;
import com.odc.commonlib.event.EventPublisher;
import com.odc.fileservice.v1.FileServiceGrpc;
import com.odc.fileservice.v1.GetFileNamesByLinksRequest;
import com.odc.notification.v1.Channel;
import com.odc.notification.v1.NotificationEvent;
import com.odc.notification.v1.Target;
import com.odc.notification.v1.UserTarget;
import com.odc.projectservice.dto.request.ApplyProjectRequest;
import com.odc.projectservice.dto.request.RejectRequest;
import com.odc.projectservice.dto.response.ApplyProjectResponse;
import com.odc.projectservice.dto.response.ProjectApplicationStatusResponse;
import com.odc.projectservice.dto.response.UserSubmittedCvResponse;
import com.odc.projectservice.entity.Project;
import com.odc.projectservice.entity.ProjectApplication;
import com.odc.projectservice.entity.ProjectMember;
import com.odc.projectservice.repository.ProjectApplicationRepository;
import com.odc.projectservice.repository.ProjectMemberRepository;
import com.odc.projectservice.repository.ProjectRepository;
import com.odc.projectservice.v1.CvAnalysisRequiredEvent;
import com.odc.userservice.v1.CheckRoleByUserIdRequest;
import com.odc.userservice.v1.UserServiceGrpc;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProjectApplicationServiceImpl implements ProjectApplicationService {
    private final ProjectRepository projectRepository;
    private final ProjectApplicationRepository projectApplicationRepository;
    private final ManagedChannel userServiceChannel;
    private final EventPublisher eventPublisher;
    private final ManagedChannel fileServiceChannel;
    private final ProjectMemberRepository projectMemberRepository;

    public ProjectApplicationServiceImpl(ProjectRepository projectRepository,
                                         ProjectApplicationRepository projectApplicationRepository,
                                         @Qualifier("userServiceChannel1") ManagedChannel userServiceChannel1,
                                         @Qualifier("fileServiceChannel") ManagedChannel fileServiceChannel,
                                         EventPublisher eventPublisher,
                                         ProjectMemberRepository projectMemberRepository) {
        this.projectRepository = projectRepository;
        this.projectApplicationRepository = projectApplicationRepository;
        this.userServiceChannel = userServiceChannel1;
        this.fileServiceChannel = fileServiceChannel;
        this.eventPublisher = eventPublisher;
        this.projectMemberRepository = projectMemberRepository;
    }

    @Override
    public ApiResponse<ApplyProjectResponse> applyProject(ApplyProjectRequest request) {
        Optional<ProjectApplication> existingApplicationOpt =
                projectApplicationRepository.findByProject_IdAndUserId(
                        request.getProjectId(),
                        request.getUserId()
                );

        if (existingApplicationOpt.isPresent()) {
            ProjectApplication existingApplication = existingApplicationOpt.get();

            if (!existingApplication.getStatus().equals(ProjectApplicationStatus.REJECTED.toString())) {
                throw new BusinessException(
                        "Bạn đã ứng tuyển vào dự án này."
                );
            }

            if (existingApplication.getStatus().equals(ProjectApplicationStatus.CANCELED.toString())) {
                throw new BusinessException("Ứng viên đã vào một dự án khác, không thể ứng tuyển.");
            }
        }

        if (!UserServiceGrpc
                .newBlockingStub(userServiceChannel)
                .checkRoleByUserId(CheckRoleByUserIdRequest
                        .newBuilder()
                        .setUserId(request.getUserId().toString())
                        .setRoleName(Role.USER.toString())
                        .build())
                .getResult()) {
            throw new BusinessException("Bạn không có quyền thực hiện hành động này. Chỉ có tài khoản sinh viên (TALENT) mới được phép ứng tuyển theo cách này.");
        }

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new BusinessException("Dự án với ID '" + request.getProjectId() + "' không tồn tại"));

        boolean alreadyApplied = projectApplicationRepository
                .existsByProject_IdAndUserId(project.getId(), request.getUserId());
        if (alreadyApplied) {
            throw new BusinessException("Sinh viên đã đăng ký tham gia dự án này");
        }

        List<ProjectApplication> otherApplications = projectApplicationRepository
                .findOtherApplicationsByUserId(request.getUserId(), request.getProjectId()
                );
        for (ProjectApplication pa : otherApplications) {
            if (pa.getStatus().equals(ProjectApplicationStatus.PENDING.toString())) {
                pa.setStatus(ProjectApplicationStatus.CANCELED.toString());
            }
        }
        projectApplicationRepository.saveAll(otherApplications);

        ProjectApplication projectApplication = ProjectApplication.builder()
                .project(project)
                .userId(request.getUserId())
                .cvUrl(request.getCvUrl())
                .status(ProjectApplicationStatus.PENDING.toString())
                .appliedAt(LocalDateTime.now())
                .build();

        projectApplicationRepository.save(projectApplication);

        CvAnalysisRequiredEvent event = CvAnalysisRequiredEvent.newBuilder()
                .setProjectApplicationId(projectApplication.getId().toString())
                .setProjectId(project.getId().toString())
                .setUserId(request.getUserId().toString())
                .setCvUrl(projectApplication.getCvUrl())
                .build();

        eventPublisher.publish("project.cv.analysis", event);

        NotificationEvent notificationEvent = NotificationEvent.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setType("PROJECT_APPLICATION")
                .setTitle("Ứng tuyển dự án thành công")
                .setContent("Bạn đã nộp CV thành công cho dự án \""
                        + project.getTitle() + "\". AI sẽ đánh giá CV của bạn và bạn sẽ nhận email thông báo kết quả. Sau đó, mentor sẽ review và duyệt bạn vào dự án.")
                .putAllData(Map.of(
                        "projectId", project.getId().toString(),
                        "applicationId", projectApplication.getId().toString(),
                        "status", projectApplication.getStatus()
                ))
                .setDeepLink("/my-applications/" + projectApplication.getId())
                .setPriority("HIGH")
                .setTarget(Target.newBuilder()
                        .setUser(UserTarget.newBuilder()
                                .addUserIds(request.getUserId().toString())
                                .build())
                        .build())
                .addAllChannels(List.of(Channel.WEB))
                .setCreatedAt(System.currentTimeMillis())
                .setCategory("PROJECT_APPLICATION")
                .build();

        eventPublisher.publish("notifications", notificationEvent);

        ApplyProjectResponse response = ApplyProjectResponse.builder()
                .id(projectApplication.getId())
                .cvUrl(projectApplication.getCvUrl())
                .status(projectApplication.getStatus())
                .appliedAt(projectApplication.getAppliedAt())
                .build();

        return ApiResponse.success("Đăng ký tham gia dự án thành công", response);
    }

    @Override
    public ApiResponse<List<UserSubmittedCvResponse>> getUserSubmittedCvs(UUID userId) {
        Pageable pageable = PageRequest.of(0, 5);

        List<ProjectApplication> applications = projectApplicationRepository
                .findByUserIdOrderBySubmittedAtDesc(userId, pageable);

        if (applications.isEmpty()) {
            return ApiResponse.success("Không có CV nào được nộp", List.of());
        }

        Map<String, ProjectApplication> latestApplicationByFile = applications.stream()
                .filter(pa -> pa.getCvUrl() != null && !pa.getCvUrl().isEmpty())
                .collect(Collectors.toMap(
                        ProjectApplication::getCvUrl,
                        pa -> pa,
                        (oldPa, newPa) -> newPa
                ));

        List<String> distinctFileLinks = new ArrayList<>(latestApplicationByFile.keySet());

        Map<String, String> fileLinkToNameMap = new HashMap<>();

        if (!distinctFileLinks.isEmpty()) {
            try {
                FileServiceGrpc.FileServiceBlockingStub stub =
                        FileServiceGrpc.newBlockingStub(fileServiceChannel);

                GetFileNamesByLinksRequest req = GetFileNamesByLinksRequest.newBuilder()
                        .addAllFileLinks(distinctFileLinks)
                        .build();

                var grpcResponse = stub.getFileNamesByLinks(req);

                fileLinkToNameMap.putAll(grpcResponse.getFileLinkToNameMap());

            } catch (Exception e) {
                log.error("Lỗi khi gọi file-service GRPC: {}", e.getMessage(), e);
            }
        }

        List<UserSubmittedCvResponse> responses = latestApplicationByFile.values().stream()
                .map(pa -> {
                    String fileLink = pa.getCvUrl();
                    String fileName = fileLinkToNameMap.getOrDefault(fileLink, "Unknown");

                    LocalDateTime submittedAt =
                            pa.getUpdatedAt() != null ? pa.getUpdatedAt() : pa.getCreatedAt();

                    return UserSubmittedCvResponse.builder()
                            .projectName(pa.getProject().getTitle())
                            .submittedAt(submittedAt)
                            .fileLink(fileLink)
                            .fileName(fileName)
                            .build();
                })
                .sorted(Comparator.comparing(UserSubmittedCvResponse::getSubmittedAt).reversed())
                .toList();

        return ApiResponse.success("Lấy danh sách CV đã nộp thành công", responses);
    }

    @Override
    public ApiResponse<ProjectApplicationStatusResponse> getProjectApplicationStatus(UUID projectId, UUID userId) {
        Optional<ProjectApplication> existingAppOpt =
                projectApplicationRepository.findByProject_IdAndUserId(projectId, userId);

        if (existingAppOpt.isEmpty()) {
            return ApiResponse.<ProjectApplicationStatusResponse>builder()
                    .success(false)
                    .data(ProjectApplicationStatusResponse
                            .builder()
                            .projectApplicationId(null)
                            .canApply(true)
                            .fileLink("")
                            .fileName("")
                            .status("")
                            .submittedAt(null)
                            .build())
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        ProjectApplication existing = existingAppOpt.get();

        boolean canApply = existing.getStatus().equals(ProjectApplicationStatus.REJECTED.toString());

        String fileLink = existing.getCvUrl();
        String fileName = "Unknown";

        if (fileLink != null && !fileLink.isEmpty()) {
            try {
                FileServiceGrpc.FileServiceBlockingStub stub =
                        FileServiceGrpc.newBlockingStub(fileServiceChannel);

                GetFileNamesByLinksRequest req = GetFileNamesByLinksRequest.newBuilder()
                        .addFileLinks(fileLink)
                        .build();

                var grpcResponse = stub.getFileNamesByLinks(req);

                fileName = grpcResponse.getFileLinkToNameMap()
                        .getOrDefault(fileLink, "Unknown");

            } catch (Exception e) {
                log.error("Lỗi khi gọi file-service GRPC: {}", e.getMessage(), e);
            }
        }

        return ApiResponse.success("Lấy thành công trạng thái ứng tuyển dự án.",
                ProjectApplicationStatusResponse.builder()
                        .projectApplicationId(existing.getId())
                        .canApply(canApply)
                        .fileLink(fileLink)
                        .fileName(fileName)
                        .submittedAt(existing.getAppliedAt())
                        .status(existing.getStatus())
                        .build());
    }

    @Override
    public ApiResponse<Void> approveApplication(UUID projectApplicationId) {
        ProjectApplication application = projectApplicationRepository.findById(projectApplicationId)
                .orElseThrow(() -> new BusinessException("Application không tồn tại"));

        if (!application.getStatus().equals(ProjectApplicationStatus.PENDING.toString())) {
            throw new BusinessException("Chỉ có thể duyệt application ở trạng thái PENDING");
        }

        boolean isAlreadyMember = projectMemberRepository
                .existsByProject_IdAndUserId(application.getProject().getId(), application.getUserId());
        if (isAlreadyMember) {
            throw new BusinessException("User đã là thành viên của dự án này");
        }

        ProjectMember member = ProjectMember.builder()
                .project(application.getProject())
                .userId(application.getUserId())
                .roleInProject(Role.TALENT.toString())
                .joinedAt(LocalDateTime.now())
                .build();
        projectMemberRepository.save(member);

        application.setStatus(ProjectApplicationStatus.APPROVED.toString());
        application.setReviewNotes("Approved");
        application.setReviewedBy((UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        application.setUpdatedAt(LocalDateTime.now());
        projectApplicationRepository.save(application);

        // Gửi notification
        NotificationEvent notificationEvent = NotificationEvent.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setType("PROJECT_APPLICATION")
                .setTitle("Ứng tuyển dự án được duyệt")
                .setContent("Bạn đã được duyệt tham gia dự án \"" + application.getProject().getTitle() + "\". Chúc mừng!")
                .putAllData(Map.of(
                        "projectId", application.getProject().getId().toString(),
                        "applicationId", application.getId().toString(),
                        "status", application.getStatus()
                ))
                .setDeepLink("/my-applications/" + application.getId())
                .setPriority("HIGH")
                .setTarget(Target.newBuilder()
                        .setUser(UserTarget.newBuilder()
                                .addUserIds(application.getUserId().toString())
                                .build())
                        .build())
                .addAllChannels(List.of(Channel.WEB))
                .setCreatedAt(System.currentTimeMillis())
                .setCategory("PROJECT_APPLICATION")
                .build();

        eventPublisher.publish("notifications", notificationEvent);

        return ApiResponse.success("Duyệt ứng viên thành công", null);
    }

    @Override
    public ApiResponse<Void> rejectApplication(UUID projectApplicationId, RejectRequest request) {
        ProjectApplication application = projectApplicationRepository.findById(projectApplicationId)
                .orElseThrow(() -> new BusinessException("Application không tồn tại"));

        if (!application.getStatus().equals(ProjectApplicationStatus.PENDING.toString())) {
            throw new BusinessException("Chỉ có thể từ chối application ở trạng thái PENDING");
        }

        application.setStatus(ProjectApplicationStatus.REJECTED.toString());
        application.setReviewNotes(request.getReviewNotes());
        application.setReviewedBy((UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        application.setUpdatedAt(LocalDateTime.now());
        projectApplicationRepository.save(application);

        NotificationEvent notificationEvent = NotificationEvent.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setType("PROJECT_APPLICATION")
                .setTitle("Ứng tuyển dự án bị từ chối")
                .setContent("CV của bạn cho dự án \"" + application.getProject().getTitle() + "\" đã bị từ chối. Lý do: " + request.getReviewNotes())
                .putAllData(Map.of(
                        "projectId", application.getProject().getId().toString(),
                        "applicationId", application.getId().toString(),
                        "status", application.getStatus()
                ))
                .setDeepLink("/my-applications/" + application.getId())
                .setPriority("HIGH")
                .setTarget(Target.newBuilder()
                        .setUser(UserTarget.newBuilder()
                                .addUserIds(application.getUserId().toString())
                                .build())
                        .build())
                .addAllChannels(List.of(Channel.WEB))
                .setCreatedAt(System.currentTimeMillis())
                .setCategory("PROJECT_APPLICATION")
                .build();

        eventPublisher.publish("notifications", notificationEvent);

        return ApiResponse.success("Từ chối ứng viên thành công", null);
    }
}
