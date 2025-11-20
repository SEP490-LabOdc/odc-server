package com.odc.projectservice.service;

import com.odc.common.constant.Role;
import com.odc.common.constant.Status;
import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.BusinessException;
import com.odc.commonlib.event.EventPublisher;
import com.odc.notification.v1.Channel;
import com.odc.notification.v1.NotificationEvent;
import com.odc.notification.v1.Target;
import com.odc.notification.v1.UserTarget;
import com.odc.projectservice.dto.request.ApplyProjectRequest;
import com.odc.projectservice.dto.response.ApplyProjectResponse;
import com.odc.projectservice.entity.Project;
import com.odc.projectservice.entity.ProjectApplication;
import com.odc.projectservice.repository.ProjectApplicationRepository;
import com.odc.projectservice.repository.ProjectRepository;
import com.odc.userservice.v1.CheckRoleByUserIdRequest;
import com.odc.userservice.v1.UserServiceGrpc;
import io.grpc.ManagedChannel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProjectApplicationServiceImpl implements ProjectApplicationService {
    private final ProjectRepository projectRepository;
    private final ProjectApplicationRepository projectApplicationRepository;
    private final ManagedChannel userServiceChannel;
    private final EventPublisher eventPublisher;

    public ProjectApplicationServiceImpl(ProjectRepository projectRepository,
                                         ProjectApplicationRepository projectApplicationRepository,
                                         @Qualifier("userServiceChannel1") ManagedChannel userServiceChannel1,
                                         EventPublisher eventPublisher) {
        this.projectRepository = projectRepository;
        this.projectApplicationRepository = projectApplicationRepository;
        this.userServiceChannel = userServiceChannel1;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public ApiResponse<ApplyProjectResponse> applyProject(ApplyProjectRequest request) {
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

        ProjectApplication projectApplication = ProjectApplication.builder()
                .project(project)
                .userId(request.getUserId())
                .cvUrl(request.getCvUrl())
                .status(Status.PENDING.toString())
                .appliedAt(LocalDateTime.now())
                .build();

        projectApplicationRepository.save(projectApplication);


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
}
