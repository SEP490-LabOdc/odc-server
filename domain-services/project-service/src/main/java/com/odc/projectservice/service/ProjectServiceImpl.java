package com.odc.projectservice.service;

import com.odc.common.constant.ProjectStatus;
import com.odc.common.constant.Role;
import com.odc.common.constant.Status;
import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.common.dto.SearchRequest;
import com.odc.common.exception.BusinessException;
import com.odc.common.specification.GenericSpecification;
import com.odc.common.util.EnumUtil;
import com.odc.companyservice.v1.CompanyServiceGrpc;
import com.odc.companyservice.v1.GetCompanyByUserIdRequest;
import com.odc.companyservice.v1.GetCompanyByUserIdResponse;
import com.odc.projectservice.dto.request.CreateProjectRequest;
import com.odc.projectservice.dto.request.UpdateProjectOpenStatusRequest;
import com.odc.projectservice.dto.request.UpdateProjectRequest;
import com.odc.projectservice.dto.request.UpdateProjectStatusRequest;
import com.odc.projectservice.dto.response.*;
import com.odc.projectservice.entity.Project;
import com.odc.projectservice.entity.ProjectApplication;
import com.odc.projectservice.entity.ProjectMember;
import com.odc.projectservice.entity.Skill;
import com.odc.projectservice.repository.*;
import com.odc.userservice.v1.GetNameRequest;
import com.odc.userservice.v1.GetNameResponse;
import com.odc.userservice.v1.UserServiceGrpc;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository projectRepository;
    private final SkillRepository skillRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectApplicationRepository projectApplicationRepository;
    private final ProjectDocumentRepository projectDocumentRepository;

    private final ManagedChannel userServiceChannel;
    private final ManagedChannel companyServiceChannel;

    // Constructor với @Qualifier - THÊM CONSTRUCTOR NÀY
    public ProjectServiceImpl(
            ProjectRepository projectRepository,
            SkillRepository skillRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectApplicationRepository projectApplicationRepository,
            ProjectDocumentRepository projectDocumentRepository,

            @Qualifier("userServiceChannel") ManagedChannel userServiceChannel,
            @Qualifier("companyServiceChannel") ManagedChannel companyServiceChannel) {
        this.projectRepository = projectRepository;
        this.skillRepository = skillRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectApplicationRepository = projectApplicationRepository;
        this.projectDocumentRepository = projectDocumentRepository;
        this.userServiceChannel = userServiceChannel;
        this.companyServiceChannel = companyServiceChannel;
    }

    @Override
    public ApiResponse<ProjectResponse> createProject(UUID userId, CreateProjectRequest request) {
        CompanyServiceGrpc.CompanyServiceBlockingStub companyStub =
                CompanyServiceGrpc.newBlockingStub(companyServiceChannel);

        GetCompanyByUserIdRequest companyRequest = GetCompanyByUserIdRequest.newBuilder()
                .setUserId(userId.toString())
                .build();

        GetCompanyByUserIdResponse companyResponse = companyStub.getCompanyByUserId(companyRequest);

        if (projectRepository.existsByCompanyIdAndTitle(UUID.fromString(companyResponse.getCompanyId()), request.getTitle())) {
            throw new BusinessException("Dự án với tiêu đề '" + request.getTitle() + "' đã tồn tại trong công ty này");
        }

        Set<Skill> skills = Set.of();

        if (request.getSkillIds() != null && !request.getSkillIds().isEmpty()) {
            skills = new HashSet<>(skillRepository.findAllById(request.getSkillIds()));

            if (skills.size() != request.getSkillIds().size()) {
                throw new BusinessException("Có một số kỹ năng không tồn tại");
            }
        }

        Project project = Project.builder()
                .companyId(UUID.fromString(companyResponse.getCompanyId()))
                .title(request.getTitle())
                .description(request.getDescription())
                .status(Status.PENDING.toString())
                .budget(request.getBudget())
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


        List<ProjectMember> mentorMembers = projectMemberRepository.findByProjectId(projectId).stream()
                .filter(pm -> Role.MENTOR.toString().equalsIgnoreCase(pm.getRoleInProject()))
                .toList();


        List<String> mentorUserIds = mentorMembers.stream()
                .map(pm -> pm.getUserId().toString())
                .toList();

        // Gọi gRPC getName để lấy tên của mentors
        Map<String, String> userIdToNameMap;
        if (!mentorUserIds.isEmpty()) {
            UserServiceGrpc.UserServiceBlockingStub userStub = UserServiceGrpc.newBlockingStub(userServiceChannel);
            GetNameResponse userNamesResponse = userStub.getName(
                    GetNameRequest.newBuilder()
                            .addAllIds(mentorUserIds)
                            .build()
            );
            userIdToNameMap = userNamesResponse.getMapMap();
        } else {
            userIdToNameMap = Map.of();
        }


        List<UserParticipantResponse> mentors = mentorMembers.stream()
                .map(pm -> UserParticipantResponse.builder()
                        .id(pm.getUserId())
                        .name(userIdToNameMap.getOrDefault(pm.getUserId().toString(), "Unknown"))
                        .roleName(Role.MENTOR.toString())
                        .isLeader(pm.isLeader())
                        .build())
                .sorted(Comparator.comparing((UserParticipantResponse m) -> !m.isLeader()))
                .toList();


        ProjectResponse responseData = convertToProjectResponse(existingProject, mentors);

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

        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);

        List<String> userIds = members.stream()
                .map(member -> member.getUserId().toString())
                .toList();

        if (userIds.isEmpty()) {
            return ApiResponse.<List<UserParticipantResponse>>builder()
                    .success(true)
                    .message("Không có người tham gia dự án")
                    .timestamp(LocalDateTime.now())
                    .data(Collections.emptyList())
                    .build();
        }

        UserServiceGrpc.UserServiceBlockingStub userStub = UserServiceGrpc.newBlockingStub(userServiceChannel);
        GetNameResponse userNamesResponse = userStub.getName(
                GetNameRequest.newBuilder()
                        .addAllIds(userIds)
                        .build());

        Map<String, String> userIdToNameMap = userNamesResponse.getMapMap();

        List<UserParticipantResponse> participants = members.stream()
                .map(member -> {
                    String name = userIdToNameMap.get(member.getUserId().toString());
                    if (name == null) return null;

                    String roleName = Role.TALENT.toString();
                    if (Role.MENTOR.toString().equalsIgnoreCase(member.getRoleInProject())) {
                        roleName = Role.MENTOR.toString();
                    }

                    return UserParticipantResponse.builder()
                            .id(member.getUserId())
                            .name(name)
                            .roleName(roleName)
                            .isLeader(member.isLeader())
                            .build();
                })
                .filter(Objects::nonNull).sorted(Comparator
                        .comparing((UserParticipantResponse p) -> !p.getRoleName().equals("MENTOR"))
                        .thenComparing(p -> !p.isLeader())).collect(Collectors.toList());

        return ApiResponse.<List<UserParticipantResponse>>builder()
                .success(true)
                .message("Lấy thông tin người tham gia dự án thành công")
                .timestamp(LocalDateTime.now())
                .data(participants)
                .build();
    }

    @Override
    public ApiResponse<PaginatedResult<GetHiringProjectDetailResponse>> getHiringProjects(Integer page, Integer pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "updatedAt", "createdAt"));

        Page<Project> projectPage = projectRepository.findByIsOpenForApplications(true, pageable);

        Page<GetHiringProjectDetailResponse> mappedPage = projectPage.map(project -> {

            List<ProjectMember> mentorMembers = projectMemberRepository.findByProjectId(project.getId()).stream()
                    .filter(pm -> Role.MENTOR.toString().equalsIgnoreCase(pm.getRoleInProject()))
                    .toList();

            List<String> mentorUserIds = mentorMembers.stream()
                    .map(pm -> pm.getUserId().toString())
                    .toList();

            Map<String, String> userIdToNameMap;

            if (!mentorUserIds.isEmpty()) {
                UserServiceGrpc.UserServiceBlockingStub userStub = UserServiceGrpc.newBlockingStub(userServiceChannel);
                GetNameResponse userNamesResponse = userStub.getName(
                        GetNameRequest.newBuilder()
                                .addAllIds(mentorUserIds)
                                .build()
                );
                userIdToNameMap = userNamesResponse.getMapMap();
            } else {
                userIdToNameMap = Map.of();
            }

            List<UserParticipantResponse> mentors = mentorMembers.stream()
                    .map(pm -> UserParticipantResponse.builder()
                            .id(pm.getUserId())
                            .name(userIdToNameMap.getOrDefault(pm.getUserId().toString(), "Unknown"))
                            .roleName(Role.MENTOR.toString())
                            .isLeader(pm.isLeader())
                            .build())
                    .sorted(Comparator.comparing(p -> !p.isLeader()))
                    .toList();

            List<SkillResponse> skills = project.getSkills().stream()
                    .map(skill -> SkillResponse.builder()
                            .id(skill.getId())
                            .name(skill.getName())
                            .description(skill.getDescription())
                            .build())
                    .toList();

            int applicantsCount = projectApplicationRepository.countByProjectId(project.getId());

            return GetHiringProjectDetailResponse.builder()
                    .projectId(project.getId())
                    .projectName(project.getTitle())
                    .description(project.getDescription())
                    .startDate(project.getStartDate())
                    .endDate(project.getEndDate())
                    .currentApplicants(applicantsCount)
                    .mentors(mentors)
                    .skills(skills)
                    .build();
        });

        return ApiResponse.success(PaginatedResult.from(mappedPage));
    }


    @Override
    public ApiResponse<List<GetProjectApplicationResponse>> getProjectApplications(UUID projectId) {
        List<ProjectApplication> projectApplicationList = projectApplicationRepository.findByProjectId(projectId);

        if (projectApplicationList.isEmpty()) {
            return ApiResponse.success(List.of());
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

    @Override
    public ApiResponse<GetCompanyProjectResponse> getProjectsByUserId(UUID userId) {
        CompanyServiceGrpc.CompanyServiceBlockingStub companyStub =
                CompanyServiceGrpc.newBlockingStub(companyServiceChannel);

        GetCompanyByUserIdRequest companyRequest = GetCompanyByUserIdRequest.newBuilder()
                .setUserId(userId.toString())
                .build();

        GetCompanyByUserIdResponse companyResponse = companyStub.getCompanyByUserId(companyRequest);

        List<Project> projectList = projectRepository.findByCompanyId(UUID.fromString(companyResponse.getCompanyId()));

        List<GetProjectResponse> projects = projectList.stream()
                .map(this::convertToCompanyProjectResponse)
                .toList();

        GetCompanyProjectResponse response = GetCompanyProjectResponse.builder()
                .companyId(UUID.fromString(companyResponse.getCompanyId()))
                .companyName(companyResponse.getCompanyName())
                .projectResponses(projects.isEmpty() ? List.of() : projects)
                .build();

        return ApiResponse.success("Lấy danh sách dự án công ty thành công", response);
    }

    @Override
    public ApiResponse<Void> updateIsOpenForApplications(UUID projectId, UpdateProjectOpenStatusRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("Dự án với ID '" + projectId + "' không tồn tại"));

        project.setIsOpenForApplications(request.getIsOpenForApplications());
        projectRepository.save(project);

        return ApiResponse.success("Cập nhật trạng thái tuyển thành viên thành công.", null);
    }

    @Override
    public ApiResponse<Void> updateProjectStatus(UUID projectId, UpdateProjectStatusRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("Dự án với ID '" + projectId + "' không tồn tại"));

        if (!EnumUtil.isEnumValueExist(request.getStatus(), ProjectStatus.class)) {
            throw new BusinessException("Trạng thái dự án không hợp lệ.");
        }

        project.setStatus(request.getStatus());
        projectRepository.save(project);

        return ApiResponse.success("Cập nhật thành công trạng thái dự án.", null);
    }

    private ProjectResponse convertToProjectResponse(Project project) {
        Set<SkillResponse> skillResponses = project.getSkills().stream()
                .map(skill -> SkillResponse.builder()
                        .id(skill.getId())
                        .name(skill.getName())
                        .description(skill.getDescription())
                        .build())
                .collect(Collectors.toSet());

        return ProjectResponse.builder()
                .id(project.getId())
                .companyId(project.getCompanyId())
                .title(project.getTitle())
                .description(project.getDescription())
                .status(project.getStatus())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .budget(project.getBudget())
                .skills(skillResponses)
                .mentors(List.of())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    private ProjectResponse convertToProjectResponse(Project project, List<UserParticipantResponse> mentors) {
        Set<SkillResponse> skillResponses = project.getSkills().stream()
                .map(skill -> SkillResponse.builder()
                        .id(skill.getId())
                        .name(skill.getName())
                        .description(skill.getDescription())
                        .build())
                .collect(Collectors.toSet());

        return ProjectResponse.builder()
                .id(project.getId())
                .companyId(project.getCompanyId())
                .title(project.getTitle())
                .description(project.getDescription())
                .status(project.getStatus())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .budget(project.getBudget())
                .skills(skillResponses)
                .mentors(mentors)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    private GetProjectResponse convertToCompanyProjectResponse(Project project) {
        Set<SkillResponse> skillResponses = project.getSkills().stream()
                .map(skill -> SkillResponse.builder()
                        .id(skill.getId())
                        .name(skill.getName())
                        .description(skill.getDescription())
                        .build())
                .collect(Collectors.toSet());

        return GetProjectResponse.builder()
                .id(project.getId())
                .title(project.getTitle())
                .description(project.getDescription())
                .status(project.getStatus())
                .startDate(project.getStartDate() != null ? project.getStartDate().toString() : null)
                .endDate(project.getEndDate() != null ? project.getEndDate().toString() : null)
                .budget(project.getBudget() != null ? project.getBudget().toString() : null)
                .skills(skillResponses)
                .build();
    }
}
