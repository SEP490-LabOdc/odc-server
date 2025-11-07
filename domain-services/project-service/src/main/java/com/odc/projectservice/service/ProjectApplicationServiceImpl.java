package com.odc.projectservice.service;

import com.odc.common.constant.Role;
import com.odc.common.constant.Status;
import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.BusinessException;
import com.odc.projectservice.dto.request.ApplyProjectRequest;
import com.odc.projectservice.dto.response.ApplyProjectResponse;
import com.odc.projectservice.entity.Project;
import com.odc.projectservice.entity.ProjectApplication;
import com.odc.projectservice.repository.ProjectApplicationRepository;
import com.odc.projectservice.repository.ProjectRepository;
import com.odc.userservice.v1.CheckRoleByUserIdRequest;
import com.odc.userservice.v1.UserServiceGrpc;
import io.grpc.ManagedChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProjectApplicationServiceImpl implements ProjectApplicationService {
    private final ProjectRepository projectRepository;
    private final ProjectApplicationRepository projectApplicationRepository;
    private final ManagedChannel userServiceChannel;

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

        ApplyProjectResponse response = ApplyProjectResponse.builder()
                .id(projectApplication.getId())
                .cvUrl(projectApplication.getCvUrl())
                .status(projectApplication.getStatus())
                .appliedAt(projectApplication.getAppliedAt())
                .build();

        return ApiResponse.success("Đăng ký tham gia dự án thành công", response);
    }
}
