package com.odc.projectservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.common.dto.SearchRequest;
import com.odc.common.exception.BusinessException;
import com.odc.common.specification.GenericSpecification;
import com.odc.projectservice.dto.request.CreateProjectRequest;
import com.odc.projectservice.dto.request.UpdateProjectRequest;
import com.odc.projectservice.dto.response.*;
import com.odc.projectservice.entity.Project;
import com.odc.projectservice.entity.ProjectApplication;
import com.odc.projectservice.entity.ProjectMember;
import com.odc.projectservice.entity.Skill;
import com.odc.projectservice.repository.ProjectApplicationRepository;
import com.odc.projectservice.repository.ProjectMemberRepository;
import com.odc.projectservice.repository.ProjectRepository;
import com.odc.projectservice.repository.SkillRepository;
import com.odc.userservice.v1.*;
import io.grpc.ManagedChannel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Transactional
public class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository projectRepository;
    private final SkillRepository skillRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectApplicationRepository projectApplicationRepository;

    private final ManagedChannel userServiceChannel;

    // Constructor với @Qualifier - THÊM CONSTRUCTOR NÀY
    public ProjectServiceImpl(
            ProjectRepository projectRepository,
            SkillRepository skillRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectApplicationRepository projectApplicationRepository,

            @Qualifier("userServiceChannel") ManagedChannel userServiceChannel) {
        this.projectRepository = projectRepository;
        this.skillRepository = skillRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectApplicationRepository = projectApplicationRepository;
        this.userServiceChannel = userServiceChannel;
    }

    private GetUserByIdResponse getUserByIdViaGrpc(UUID userId) {
        try {
            return UserServiceGrpc
                    .newBlockingStub(userServiceChannel)
                    .getUserById(GetUserByIdRequest.newBuilder()
                            .setUserId(userId.toString())
                            .build());
        } catch (Exception e) {
            // Log error nếu cần
            return null;
        }
    }


    @Override
    public ApiResponse<ProjectResponse> createProject(CreateProjectRequest request) {
        if (projectRepository.existsByCompanyIdAndTitle(request.getCompanyId(), request.getTitle())) {
            throw new BusinessException("Dự án với tiêu đề '" + request.getTitle() + "' đã tồn tại trong công ty này");
        }
        Set<Skill> skills = Set.of();

        if (request.getSkillIds() != null && !request.getSkillIds().isEmpty()) {
            skills = skillRepository.findAllById(request.getSkillIds()).stream()
                    .collect(Collectors.toSet());

            // Kiểm tra xem tất cả skillIds có tồn tại không
            if (skills.size() != request.getSkillIds().size()) {
                throw new BusinessException("Có một số kỹ năng không tồn tại");
            }
        }

        Project project = Project.builder()
                .companyId(request.getCompanyId())
                .title(request.getTitle())
                .description(request.getDescription())
                .skills(skills)
                .build();

        Project savedProject = projectRepository.save(project);
        ProjectResponse responseData = convertToProjectResponse(savedProject);

        return ApiResponse.success("Tạo dự án thành công", responseData);
    }

    @Override
    public ApiResponse<ProjectResponse> updateProject(UUID projectId, UpdateProjectRequest request) {
        Project existingProject = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("Dự án với ID '" + projectId + "' không tồn tại"));

        if (!existingProject.getTitle().equals(request.getTitle())) {
            if (projectRepository.existsByCompanyIdAndTitleAndIdNot(existingProject.getCompanyId(), request.getTitle(), projectId)) {
                throw new BusinessException("Dự án với tiêu đề '" + request.getTitle() + "' đã tồn tại trong công ty này");
            }
        }
        Set<Skill> skills = Set.of();
        if (request.getSkillIds() != null && !request.getSkillIds().isEmpty()) {
            skills = skillRepository.findAllById(request.getSkillIds()).stream()
                    .collect(Collectors.toSet());

            // Kiểm tra xem tất cả skillIds có tồn tại không
            if (skills.size() != request.getSkillIds().size()) {
                throw new BusinessException("Có một số kỹ năng không tồn tại");
            }
        }
        existingProject.setMentorId(request.getMentorId());
        existingProject.setTitle(request.getTitle());
        existingProject.setDescription(request.getDescription());
        existingProject.setStatus(request.getStatus());
        existingProject.setStartDate(request.getStartDate());
        existingProject.setEndDate(request.getEndDate());
        existingProject.setBudget(request.getBudget());
        existingProject.setSkills(skills);

        Project updatedProject = projectRepository.save(existingProject);
        ProjectResponse responseData = convertToProjectResponse(updatedProject);
        return ApiResponse.success("Cập nhật dự án thành công", responseData);
    }

    @Override
    public ApiResponse<Void> deleteProject(UUID projectId) {
        Project existingProject = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("Dự án với ID '" + projectId + "' không tồn tại"));

        projectRepository.delete(existingProject);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Xóa dự án thành công")
                .timestamp(LocalDateTime.now())
                .data(null)
                .build();
    }

    @Override
    public ApiResponse<ProjectResponse> getProjectById(UUID projectId) {
        Project existingProject = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("Dự án với ID '" + projectId + "' không tồn tại"));

        ProjectResponse responseData = convertToProjectResponse(existingProject);

        return ApiResponse.<ProjectResponse>builder()
                .success(true)
                .message("Lấy thông tin dự án thành công")
                .timestamp(LocalDateTime.now())
                .data(responseData)
                .build();
    }

    @Override
    public ApiResponse<List<ProjectResponse>> getAllProjects() {

        List<ProjectResponse> projects = projectRepository.findAll()
                .stream()
                .map(this::convertToProjectResponse)
                .collect(Collectors.toList());

        return ApiResponse.<List<ProjectResponse>>builder()
                .success(true)
                .message("Lấy danh sách dự án thành công")
                .timestamp(LocalDateTime.now())
                .data(projects)
                .build();
    }

    @Override
    public ApiResponse<List<ProjectResponse>> searchProjects(SearchRequest request) {
        Specification<Project> specification = new GenericSpecification<>(request.getFilters());

        List<Sort.Order> orders = new ArrayList<>();
        if (request.getSorts() != null && !request.getSorts().isEmpty()) {
            for (com.odc.common.dto.SortRequest sortRequest : request.getSorts()) {
                orders.add(new Sort.Order(sortRequest.getDirection(), sortRequest.getKey()));
            }
        }
        Sort sort = Sort.by(orders);

        List<ProjectResponse> projects = projectRepository.findAll(specification, sort)
                .stream()
                .map(this::convertToProjectResponse)
                .collect(Collectors.toList());

        return ApiResponse.<List<ProjectResponse>>builder()
                .success(true)
                .message("Tìm kiếm dự án thành công")
                .timestamp(LocalDateTime.now())
                .data(projects)
                .build();
    }

    @Override
    public ApiResponse<PaginatedResult<ProjectResponse>> searchProjectsWithPagination(SearchRequest request) {
        Specification<Project> specification = new GenericSpecification<>(request.getFilters());

        List<Sort.Order> orders = new ArrayList<>();
        if (request.getSorts() != null && !request.getSorts().isEmpty()) {
            for (com.odc.common.dto.SortRequest sortRequest : request.getSorts()) {
                orders.add(new Sort.Order(sortRequest.getDirection(), sortRequest.getKey()));
            }
        }
        Sort sort = Sort.by(orders);

        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize(), sort);

        Page<ProjectResponse> page = projectRepository.findAll(specification, pageable)
                .map(this::convertToProjectResponse);

        return ApiResponse.success(PaginatedResult.from(page));
    }

    @Override
    public ApiResponse<List<UserParticipantResponse>> getProjectParticipants(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("Dự án với ID '" + projectId + "' không tồn tại"));

        List<UserParticipantResponse> participants = new ArrayList<>();

        // Get mentor info và thêm vào list
        if (project.getMentorId() != null) {
            GetUserByIdResponse mentorResponse = getUserByIdViaGrpc(project.getMentorId());
            if (mentorResponse != null) {
                UserParticipantResponse mentorParticipant = UserParticipantResponse.builder()
                        .id(UUID.fromString(mentorResponse.getId()))
                        .name(mentorResponse.getFullName())
                        .roleName("Mentor") // hoặc null nếu không có roleName cho mentor
                        .isLeader(false) // mentor không phải leader theo mặc định
                        .build();
                participants.add(mentorParticipant);
            }
        }

        // Get talents info và thêm vào list
        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);
        List<UserParticipantResponse> talentParticipants = members.stream()
                .map(member -> {
                    GetUserByIdResponse userResponse = getUserByIdViaGrpc(member.getUserId());
                    if (userResponse != null) {
                        return UserParticipantResponse.builder()
                                .id(UUID.fromString(userResponse.getId()))
                                .name(userResponse.getFullName())
                                .roleName(member.getRoleInProject())
                                .isLeader(member.isLeader())
                                .build();
                    }
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        participants.addAll(talentParticipants);

        return ApiResponse.<List<UserParticipantResponse>>builder()
                .success(true)
                .message("Lấy thông tin người tham gia dự án thành công")
                .timestamp(LocalDateTime.now())
                .data(participants)
                .build();
    }

    @Override
    public ApiResponse<PaginatedResult<GetStudentProjectResponse>> getHiringProjects(Integer page, Integer pageSize) {
        return null;
    }

    @Override
    public ApiResponse<List<GetProjectApplicationResponse>> getProjectApplications(UUID projectId) {
        List<ProjectApplication> projectApplicationList = projectApplicationRepository.findByProjectId(projectId);

        if (projectApplicationList.isEmpty()) {
            return ApiResponse.success(List.of()); // trả về list rỗng nếu không có
        }

        List<String> userIds = projectApplicationList.stream()
                .map(pa -> pa.getUserId().toString())
                .toList();

        UserServiceGrpc.UserServiceBlockingStub userStub = UserServiceGrpc.newBlockingStub(userServiceChannel);
        GetNameResponse userNamesResponse = userStub.getName(
                GetNameRequest.newBuilder()
                        .addAllIds(userIds)
                        .build());

        Map<String, String> userIdToNameMap = userNamesResponse.getMapMap();

        List<GetProjectApplicationResponse> responses = projectApplicationList.stream()
                .map(pa -> GetProjectApplicationResponse.builder()
                        .id(pa.getId())
                        .userId(pa.getUserId())
                        .name(userIdToNameMap.getOrDefault(pa.getUserId().toString(), "Unknown"))
                        .cvUrl(pa.getCvUrl())
                        .status(pa.getStatus())
                        .appliedAt(pa.getAppliedAt())
                        .build()
                )
                .toList();

        return ApiResponse.success(responses);
    }

    private ProjectResponse convertToProjectResponse(Project project) {
        Set<SkillResponse> skillResponses = project.getSkills().stream()
                .map(skill -> SkillResponse.builder()
                        .id(skill.getId())
                        .name(skill.getName())
                        .description(skill.getDescription())
                        .createdAt(skill.getCreatedAt())
                        .updatedAt(skill.getUpdatedAt())
                        .build())
                .collect(Collectors.toSet());

        return ProjectResponse.builder()
                .id(project.getId())
                .companyId(project.getCompanyId())
                .mentorId(project.getMentorId())
                .title(project.getTitle())
                .description(project.getDescription())
                .status(project.getStatus())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .budget(project.getBudget())
                .skills(skillResponses)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
