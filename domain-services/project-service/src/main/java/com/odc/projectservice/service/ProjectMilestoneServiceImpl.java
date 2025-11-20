package com.odc.projectservice.service;

import com.odc.common.constant.Status;
import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.BusinessException;
import com.odc.projectservice.dto.request.CreateProjectMilestoneRequest;
import com.odc.projectservice.dto.request.UpdateProjectMilestoneRequest;
import com.odc.projectservice.dto.response.ProjectMilestoneResponse;
import com.odc.projectservice.entity.Project;
import com.odc.projectservice.entity.ProjectMilestone;
import com.odc.projectservice.repository.ProjectMilestoneRepository;
import com.odc.projectservice.repository.ProjectRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ProjectMilestoneServiceImpl implements ProjectMilestoneService {
    private final ProjectMilestoneRepository projectMilestoneRepository;
    private final ProjectRepository projectRepository;

    @Override
    public ApiResponse<ProjectMilestoneResponse> createProjectMilestone(CreateProjectMilestoneRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new BusinessException("Dự án với ID '" + request.getProjectId() + "' không tồn tại"));


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

        ProjectMilestone updatedMilestone = projectMilestoneRepository.save(existingMilestone);

        ProjectMilestoneResponse responseData = ProjectMilestoneResponse.builder()
                .id(updatedMilestone.getId())
                .projectId(updatedMilestone.getProject().getId())
                .title(updatedMilestone.getTitle())
                .description(updatedMilestone.getDescription())
                .startDate(updatedMilestone.getStartDate())
                .endDate(updatedMilestone.getEndDate())
                .status(updatedMilestone.getStatus())
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

        ProjectMilestoneResponse responseData = convertToProjectMilestoneResponse(milestone);

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

    private ProjectMilestoneResponse convertToProjectMilestoneResponse(ProjectMilestone milestone) {
        return ProjectMilestoneResponse.builder()
                .id(milestone.getId())
                .projectId(milestone.getProject().getId())
                .title(milestone.getTitle())
                .description(milestone.getDescription())
                .startDate(milestone.getStartDate())
                .endDate(milestone.getEndDate())
                .status(milestone.getStatus())
                .build();
    }
}
