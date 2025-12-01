package com.odc.projectservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odc.common.constant.ProjectStatus;
import com.odc.common.constant.Role;
import com.odc.common.constant.Status;
import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.common.dto.SearchRequest;
import com.odc.common.exception.BusinessException;
import com.odc.common.specification.GenericSpecification;
import com.odc.common.util.EnumUtil;
import com.odc.commonlib.event.EventPublisher;
import com.odc.companyservice.v1.*;
import com.odc.notification.v1.Channel;
import com.odc.notification.v1.NotificationEvent;
import com.odc.notification.v1.RoleTarget;
import com.odc.notification.v1.Target;
import com.odc.projectservice.dto.request.CreateProjectRequest;
import com.odc.projectservice.dto.request.UpdateProjectOpenStatusRequest;
import com.odc.projectservice.dto.request.UpdateProjectRequest;
import com.odc.projectservice.dto.request.UpdateProjectStatusRequest;
import com.odc.projectservice.dto.response.*;
import com.odc.projectservice.entity.*;
import com.odc.projectservice.repository.*;
import com.odc.projectservice.v1.ProjectUpdateRequiredEvent;
import com.odc.userservice.v1.*;
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
    private final ProjectMilestoneRepository projectMilestoneRepository;
    private final EventPublisher eventPublisher;
    private final ManagedChannel userServiceChannel;
    private final ManagedChannel companyServiceChannel;
    private final ObjectMapper objectMapper;

    // Constructor với @Qualifier - THÊM CONSTRUCTOR NÀY
    public ProjectServiceImpl(
            ProjectRepository projectRepository,
            SkillRepository skillRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectApplicationRepository projectApplicationRepository,
            ProjectDocumentRepository projectDocumentRepository,
            ProjectMilestoneRepository projectMilestoneRepository,
            EventPublisher eventPublisher,
            ObjectMapper objectMapper,
            @Qualifier("userServiceChannel1") ManagedChannel userServiceChannel,
            @Qualifier("companyServiceChannel") ManagedChannel companyServiceChannel) {
        this.projectRepository = projectRepository;
        this.skillRepository = skillRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectApplicationRepository = projectApplicationRepository;
        this.projectDocumentRepository = projectDocumentRepository;
        this.eventPublisher = eventPublisher;
        this.userServiceChannel = userServiceChannel;
        this.companyServiceChannel = companyServiceChannel;
        this.projectMilestoneRepository = projectMilestoneRepository;
        this.objectMapper = objectMapper;
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
                .isOpenForApplications(false)
                .budget(request.getBudget())
                .skills(skills)
                .build();

        Project savedProject = projectRepository.save(project);
        ProjectResponse responseData = convertToProjectResponse(savedProject);

        RoleTarget roleTarget = RoleTarget.newBuilder()
                .addRoles(Role.LAB_ADMIN.toString())
                .build();

        Target target = Target.newBuilder()
                .setRole(roleTarget)
                .build();

        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("projectId", savedProject.getId().toString());
        dataMap.put("companyId", companyResponse.getCompanyId());
        dataMap.put("projectTitle", savedProject.getTitle());

        NotificationEvent notificationEvent = NotificationEvent.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setType("NEW_PROJECT_CREATED")
                .setTitle("New Project Created")
                .setContent("A new project titled \"" + savedProject.getTitle() + "\" has been created and is awaiting approval.")
                .putAllData(dataMap)
                .setDeepLink("/approve-project?id=" + savedProject.getId())
                .setPriority("HIGH")
                .setTarget(target)
                .addAllChannels(List.of(Channel.WEB))
                .setCreatedAt(System.currentTimeMillis())
                .setCategory("PROJECT_MANAGEMENT")
                .build();

        eventPublisher.publish("notifications", notificationEvent);
        log.info("Notification event published successfully.");

        return ApiResponse.success("Tạo dự án thành công", responseData);
    }

    @Override
    public ApiResponse<ProjectResponse> updateProject(UUID projectId, UpdateProjectRequest request) {
        Project existingProject = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("Dự án với ID '" + projectId + "' không tồn tại"));

        if (!existingProject.getStatus().equals(ProjectStatus.UPDATE_REQUIRED.toString())) {
            throw new BusinessException("Dự án này không yêu cầu cập nhật nữa.");
        }

        if (!existingProject.getTitle().equals(request.getTitle())) {
            if (projectRepository.existsByCompanyIdAndTitleAndIdNot(existingProject.getCompanyId(), request.getTitle(), projectId)) {
                throw new BusinessException("Dự án với tiêu đề '" + request.getTitle() + "' đã tồn tại trong công ty này");
            }
        }
        Set<Skill> skills = Set.of();
        if (request.getSkillIds() != null && !request.getSkillIds().isEmpty()) {
            skills = new HashSet<>(skillRepository.findAllById(request.getSkillIds()));

            if (skills.size() != request.getSkillIds().size()) {
                throw new BusinessException("Có một số kỹ năng không tồn tại");
            }
        }
        existingProject.setTitle(request.getTitle());
        existingProject.setDescription(request.getDescription());
        existingProject.setBudget(request.getBudget());
        existingProject.setSkills(skills);
        existingProject.setStatus(ProjectStatus.PENDING.toString());

        Project updatedProject = projectRepository.save(existingProject);
        ProjectResponse responseData = convertToProjectResponse(updatedProject);

        RoleTarget roleTarget = RoleTarget.newBuilder()
                .addRoles(Role.LAB_ADMIN.toString())
                .build();

        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("projectId", updatedProject.getId().toString());
        dataMap.put("companyId", existingProject.getCompanyId().toString());
        dataMap.put("projectTitle", updatedProject.getTitle());

        Target target = Target.newBuilder()
                .setRole(roleTarget)
                .build();

        NotificationEvent notificationEvent = NotificationEvent.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setType("PROJECT_UPDATED")
                .setTitle("Project Updated - Awaiting Review")
                .setContent("Project titled \"" + updatedProject.getTitle() + "\" has been updated by the company user and is awaiting review.")
                .putAllData(dataMap)
                .setDeepLink("/review-project?id=" + updatedProject.getId())
                .setPriority("HIGH")
                .setTarget(target)
                .addAllChannels(List.of(Channel.WEB))
                .setCreatedAt(System.currentTimeMillis())
                .setCategory("PROJECT_MANAGEMENT")
                .build();

        log.info("Built notification event for LAB_ADMIN: {}", notificationEvent);

        eventPublisher.publish("notifications", notificationEvent);
        log.info("Notification event published successfully.");

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

        List<ProjectMember> allMembers = projectMemberRepository.findByProjectId(projectId);

        List<ProjectMember> mentorMembers = allMembers.stream()
                .filter(pm -> Role.MENTOR.toString().equalsIgnoreCase(pm.getRoleInProject()))
                .toList();

        List<ProjectMember> talentMembers = allMembers.stream()
                .filter(pm -> Role.TALENT.toString().equalsIgnoreCase(pm.getRoleInProject()))
                .toList();

        List<String> allUserIds = allMembers.stream()
                .map(pm -> pm.getUserId().toString())
                .distinct()
                .toList();

        Map<String, String> userIdToNameMapTemp;
        if (!allUserIds.isEmpty()) {
            try {
                UserServiceGrpc.UserServiceBlockingStub userStub = UserServiceGrpc.newBlockingStub(userServiceChannel);
                GetNameResponse userNamesResponse = userStub.getName(
                        GetNameRequest.newBuilder()
                                .addAllIds(allUserIds)
                                .build()
                );
                userIdToNameMapTemp = userNamesResponse.getMapMap();
            } catch (Exception e) {
                log.error("Không thể lấy thông tin user qua gRPC: {}", e.getMessage(), e);
                userIdToNameMapTemp = Map.of();
            }
        } else {
            userIdToNameMapTemp = Map.of();
        }

        final Map<String, String> userIdToNameMap = userIdToNameMapTemp;

        List<UserParticipantResponse> mentors = mentorMembers.stream()
                .map(pm -> UserParticipantResponse.builder()
                        .id(pm.getUserId())
                        .name(userIdToNameMap.getOrDefault(pm.getUserId().toString(), "Unknown"))
                        .roleName(Role.MENTOR.toString())
                        .isLeader(pm.isLeader())
                        .build())
                .sorted(Comparator.comparing((UserParticipantResponse m) -> !m.isLeader()))
                .toList();

        List<UserParticipantResponse> talents = talentMembers.stream()
                .map(pm -> UserParticipantResponse.builder()
                        .id(pm.getUserId())
                        .name(userIdToNameMap.getOrDefault(pm.getUserId().toString(), "Unknown"))
                        .roleName(Role.TALENT.toString())
                        .isLeader(pm.isLeader())
                        .build())
                .sorted(Comparator.comparing((UserParticipantResponse t) -> !t.isLeader()))
                .toList();

        UUID createdByUserId = null;
        String createdByName = null;
        String createdByAvatar = null;
        if (existingProject.getCreatedBy() != null && !existingProject.getCreatedBy().isEmpty()) {
            try {
                createdByUserId = UUID.fromString(existingProject.getCreatedBy());
                UserServiceGrpc.UserServiceBlockingStub userStub = UserServiceGrpc.newBlockingStub(userServiceChannel);
                GetUserByIdResponse userResponse = userStub.getUserById(
                        GetUserByIdRequest.newBuilder()
                                .setUserId(existingProject.getCreatedBy())
                                .build()
                );
                createdByName = userResponse.getFullName();
                createdByAvatar = userResponse.getAvatarUrl();
            } catch (Exception e) {
                log.warn("Không thể lấy thông tin người tạo project: {}", e.getMessage());
            }
        }

        UUID currentMilestoneId = null;
        String currentMilestoneName = null;
        List<ProjectMilestone> milestones = projectMilestoneRepository.findByProjectId(projectId);
        if (milestones != null && !milestones.isEmpty()) {
            ProjectMilestone currentMilestone = milestones.stream()
                    .filter(m -> Status.ACTIVE.toString().equalsIgnoreCase(m.getStatus()) ||
                            (!Status.COMPLETED.toString().equalsIgnoreCase(m.getStatus()) &&
                                    !Status.CANCELED.toString().equalsIgnoreCase(m.getStatus())))
                    .min(Comparator.comparing(ProjectMilestone::getStartDate,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElse(null);

            if (currentMilestone != null) {
                currentMilestoneId = currentMilestone.getId();
                currentMilestoneName = currentMilestone.getTitle();
            }
        }

        String companyName = null;
        if (existingProject.getCompanyId() != null) {
            try {
                CompanyServiceGrpc.CompanyServiceBlockingStub companyStub =
                        CompanyServiceGrpc.newBlockingStub(companyServiceChannel);

                GetCompaniesByIdsRequest request = GetCompaniesByIdsRequest.newBuilder()
                        .addCompanyIds(existingProject.getCompanyId().toString())
                        .build();

                GetCompaniesByIdsResponse response = companyStub.getCompaniesByIds(request);

                companyName = response.getCompanyNamesMap().get(existingProject.getCompanyId().toString());

            } catch (Exception e) {
                log.error("Không thể lấy thông tin công ty với ID {}: {}", existingProject.getCompanyId(), e.getMessage(), e);
            }
        }

        ProjectResponse responseData = convertToProjectResponse(existingProject, mentors, talents,
                createdByUserId, createdByName, createdByAvatar,
                currentMilestoneId, currentMilestoneName, companyName);

        return ApiResponse.success("Lấy thông tin dự án thành công", responseData);
    }

    @Override
    public ApiResponse<List<ProjectResponse>> getAllProjects() {
        List<Project> projects = projectRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"));

        Set<UUID> companyIds = projects.stream()
                .map(Project::getCompanyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, String> companyIdToNameMap = new HashMap<>();
        if (!companyIds.isEmpty()) {
            CompanyServiceGrpc.CompanyServiceBlockingStub companyStub =
                    CompanyServiceGrpc.newBlockingStub(companyServiceChannel);

            try {
                GetCompaniesByIdsRequest request = GetCompaniesByIdsRequest.newBuilder()
                        .addAllCompanyIds(
                                companyIds.stream()
                                        .map(UUID::toString)
                                        .toList()
                        )
                        .build();

                GetCompaniesByIdsResponse response = companyStub.getCompaniesByIds(request);

                response.getCompanyNamesMap().forEach((id, name) -> {
                    try {
                        companyIdToNameMap.put(UUID.fromString(id), name);
                    } catch (Exception e) {
                        log.warn("Lỗi khi convert companyId {}: {}", id, e.getMessage());
                    }
                });

            } catch (Exception e) {
                log.error("Không thể lấy danh sách công ty qua gRPC: {}", e.getMessage(), e);
            }
        }

        Map<UUID, List<ProjectMember>> projectMembersMap = new HashMap<>();
        for (Project project : projects) {
            List<ProjectMember> members = projectMemberRepository.findByProjectId(project.getId());
            projectMembersMap.put(project.getId(), members);
        }

        Set<String> allUserIds = projectMembersMap.values().stream()
                .flatMap(List::stream)
                .map(member -> member.getUserId().toString())
                .collect(Collectors.toSet());

        Map<String, String> userIdToNameMap = new HashMap<>();
        if (!allUserIds.isEmpty()) {
            try {
                UserServiceGrpc.UserServiceBlockingStub userStub =
                        UserServiceGrpc.newBlockingStub(userServiceChannel);
                GetNameResponse userNamesResponse = userStub.getName(
                        GetNameRequest.newBuilder()
                                .addAllIds(new ArrayList<>(allUserIds))
                                .build());
                userIdToNameMap = userNamesResponse.getMapMap();
            } catch (Exception e) {
                log.error("Không thể lấy danh sách user qua gRPC: {}", e.getMessage(), e);
            }
        }

        final Map<String, String> finalUserIdToNameMap = userIdToNameMap;
        List<ProjectResponse> projectResponses = projects.stream()
                .map(project -> {
                    String companyName = companyIdToNameMap.getOrDefault(project.getCompanyId(), null);

                    List<ProjectMember> members = projectMembersMap.getOrDefault(project.getId(), Collections.emptyList());

                    List<UserParticipantResponse> mentors = members.stream()
                            .filter(member -> Role.MENTOR.toString().equalsIgnoreCase(member.getRoleInProject()))
                            .map(member -> {
                                String name = finalUserIdToNameMap.getOrDefault(member.getUserId().toString(), "Unknown");
                                return UserParticipantResponse.builder()
                                        .id(member.getUserId())
                                        .name(name)
                                        .roleName(Role.MENTOR.toString())
                                        .isLeader(member.isLeader())
                                        .build();
                            })
                            .sorted(Comparator.comparing((UserParticipantResponse p) -> !p.isLeader()))
                            .collect(Collectors.toList());

                    List<UserParticipantResponse> talents = members.stream()
                            .filter(member -> Role.USER.toString().equalsIgnoreCase(member.getRoleInProject()))
                            .map(member -> {
                                String name = finalUserIdToNameMap.getOrDefault(member.getUserId().toString(), "Unknown");
                                return UserParticipantResponse.builder()
                                        .id(member.getUserId())
                                        .name(name)
                                        .roleName(Role.TALENT.toString())
                                        .isLeader(member.isLeader())
                                        .build();
                            })
                            .sorted(Comparator.comparing((UserParticipantResponse p) -> !p.isLeader()))
                            .collect(Collectors.toList());

                    return convertToProjectResponse(project, mentors, talents, null, null, null,
                            null, null, companyName);
                })
                .collect(Collectors.toList());

        return ApiResponse.success("Lấy danh sách dự án thành công", projectResponses);
    }

    @Override
    public ApiResponse<List<ProjectResponse>> searchProjects(SearchRequest request) {
        Specification<Project> specification = new GenericSpecification<>(request.getFilters());

        List<Sort.Order> orders = new ArrayList<>();
        if (request.getSorts() != null && !request.getSorts().isEmpty()) {
            for (com.odc.common.dto.SortRequest sortRequest : request.getSorts()) {
                orders.add(new Sort.Order(sortRequest.getDirection(), sortRequest.getKey()));
            }
        } else {
            orders.add(new Sort.Order(Sort.Direction.DESC, "updatedAt"));
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
        } else {
            orders.add(new Sort.Order(Sort.Direction.DESC, "updatedAt"));
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

                    String roleName = Role.USER.toString();
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
        List<ProjectApplication> projectApplicationList = projectApplicationRepository
                .findByProjectId(projectId)
                .stream()
                .sorted(Comparator.comparing(ProjectApplication::getUpdatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

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
                .map(pa -> {
                    AiScanResultResponse aiResponse = null;

                    if (pa.getAiScanResult() != null) {
                        try {
                            aiResponse = objectMapper.convertValue(pa.getAiScanResult(), AiScanResultResponse.class);
                        } catch (Exception e) {
                            log.error("Lỗi map AI result: {}", e.getMessage());
                        }
                    }

                    return GetProjectApplicationResponse.builder()
                            .id(pa.getId())
                            .userId(pa.getUserId())
                            .name(userIdToNameMap.getOrDefault(pa.getUserId().toString(), "Unknown"))
                            .cvUrl(pa.getCvUrl())
                            .status(pa.getStatus())
                            .appliedAt(pa.getAppliedAt())
                            .aiScanResult(aiResponse)
                            .build();
                })
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

        List<Project> projectList = projectRepository
                .findByCompanyIdOrderByUpdatedAtDesc(UUID.fromString(companyResponse.getCompanyId()));

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

        if (!EnumUtil.isEnumValueExist(request.getStatus().toUpperCase(), ProjectStatus.class)) {
            throw new BusinessException("Trạng thái dự án không hợp lệ.");
        }

        GetCompanyByIdResponse getCompanyByIdResponse = CompanyServiceGrpc
                .newBlockingStub(companyServiceChannel)
                .getCompanyById(
                        GetCompanyByIdRequest.newBuilder()
                                .setCompanyId(project.getCompanyId().toString())
                                .build()
                );

        if (request.getStatus().toUpperCase().equals(ProjectStatus.UPDATE_REQUIRED.toString())) {
            eventPublisher.publish("project.update-required",
                    ProjectUpdateRequiredEvent.newBuilder()
                            .setProjectId(project.getId().toString())
                            .setProjectTitle(project.getTitle())
                            .setCompanyId(project.getCompanyId().toString())
                            .setCompanyName(getCompanyByIdResponse != null ? getCompanyByIdResponse.getCompanyName() : "")
                            .setContactPersonEmail(getCompanyByIdResponse != null ? getCompanyByIdResponse.getContactPersonEmail() : "")
                            .build()
            );
        }

        project.setStatus(request.getStatus());
        projectRepository.save(project);

        return ApiResponse.success("Cập nhật thành công trạng thái dự án.", null);
    }

    @Override
    public ApiResponse<List<ProjectResponse>> getMyProjects(UUID userId, String status) {
        if (status != null && !status.trim().isEmpty()) {
            if (!EnumUtil.isEnumValueExist(status.toUpperCase(), ProjectStatus.class)) {
                throw new BusinessException("Trạng thái dự án không hợp lệ: " + status);
            }
        }

        List<ProjectMember> projectMembers = projectMemberRepository.findByUserId(userId);

        if (projectMembers.isEmpty()) {
            return ApiResponse.success("Không có dự án nào", List.of());
        }

        List<UUID> projectIds = projectMembers.stream()
                .map(pm -> pm.getProject().getId())
                .distinct()
                .toList();

        List<Project> projects = projectRepository.findAllById(projectIds);

        if (status != null && !status.trim().isEmpty()) {
            String statusUpper = status.toUpperCase();
            projects = projects.stream()
                    .filter(p -> statusUpper.equalsIgnoreCase(p.getStatus()))
                    .toList();
        }

        Set<UUID> companyIds = projects.stream()
                .map(Project::getCompanyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, String> companyIdToNameMap = new HashMap<>();
        if (!companyIds.isEmpty()) {
            CompanyServiceGrpc.CompanyServiceBlockingStub companyStub =
                    CompanyServiceGrpc.newBlockingStub(companyServiceChannel);

            try {
                GetCompaniesByIdsRequest request = GetCompaniesByIdsRequest.newBuilder()
                        .addAllCompanyIds(
                                companyIds.stream()
                                        .map(UUID::toString)
                                        .toList()
                        )
                        .build();

                GetCompaniesByIdsResponse response = companyStub.getCompaniesByIds(request);

                response.getCompanyNamesMap().forEach((id, name) -> {
                    try {
                        companyIdToNameMap.put(UUID.fromString(id), name);
                    } catch (Exception e) {
                        log.warn("Lỗi khi convert companyId {}: {}", id, e.getMessage());
                    }
                });

            } catch (Exception e) {
                log.error("Không thể lấy danh sách công ty qua gRPC: {}", e.getMessage(), e);
            }
        }

        Map<UUID, List<ProjectMember>> projectMembersMap = new HashMap<>();
        for (Project project : projects) {
            List<ProjectMember> members = projectMemberRepository.findByProjectId(project.getId());
            projectMembersMap.put(project.getId(), members);
        }

        Set<String> allUserIds = projectMembersMap.values().stream()
                .flatMap(List::stream)
                .map(member -> member.getUserId().toString())
                .collect(Collectors.toSet());

        Set<String> createdByUserIds = projects.stream()
                .map(Project::getCreatedBy)
                .filter(Objects::nonNull)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        allUserIds.addAll(createdByUserIds);

        Map<String, String> userIdToNameMap = new HashMap<>();
        Map<String, UserInfo> userIdToUserInfoMap = new HashMap<>();

        if (!allUserIds.isEmpty()) {
            try {
                UserServiceGrpc.UserServiceBlockingStub userStub = UserServiceGrpc.newBlockingStub(userServiceChannel);

                GetNameResponse userNamesResponse = userStub.getName(
                        GetNameRequest.newBuilder()
                                .addAllIds(new ArrayList<>(allUserIds))
                                .build()
                );
                userIdToNameMap = userNamesResponse.getMapMap();

                if (!createdByUserIds.isEmpty()) {
                    GetUsersByIdsResponse usersResponse = userStub.getUsersByIds(
                            GetUsersByIdsRequest.newBuilder()
                                    .addAllUserId(new ArrayList<>(createdByUserIds))
                                    .build()
                    );

                    userIdToUserInfoMap = usersResponse.getUsersList().stream()
                            .collect(Collectors.toMap(
                                    u -> u.getUserId(),
                                    u -> u
                            ));
                }
            } catch (Exception e) {
                log.error("Không thể lấy thông tin user qua gRPC: {}", e.getMessage(), e);
            }
        }

        final Map<String, String> finalUserIdToNameMap = userIdToNameMap;
        final Map<String, UserInfo> finalUserIdToUserInfoMap = userIdToUserInfoMap;

        List<ProjectResponse> projectResponses = projects.stream()
                .map(project -> {
                    String companyName = companyIdToNameMap.getOrDefault(project.getCompanyId(), null);

                    List<ProjectMember> members = projectMembersMap.getOrDefault(project.getId(), Collections.emptyList());

                    List<ProjectMember> mentorMembers = members.stream()
                            .filter(pm -> Role.MENTOR.toString().equalsIgnoreCase(pm.getRoleInProject()))
                            .toList();

                    List<ProjectMember> talentMembers = members.stream()
                            .filter(pm -> Role.TALENT.toString().equalsIgnoreCase(pm.getRoleInProject()))
                            .toList();

                    List<UserParticipantResponse> mentors = mentorMembers.stream()
                            .map(pm -> UserParticipantResponse.builder()
                                    .id(pm.getUserId())
                                    .name(finalUserIdToNameMap.getOrDefault(pm.getUserId().toString(), "Unknown"))
                                    .roleName(Role.MENTOR.toString())
                                    .isLeader(pm.isLeader())
                                    .build())
                            .sorted(Comparator.comparing((UserParticipantResponse m) -> !m.isLeader()))
                            .toList();

                    List<UserParticipantResponse> talents = talentMembers.stream()
                            .map(pm -> UserParticipantResponse.builder()
                                    .id(pm.getUserId())
                                    .name(finalUserIdToNameMap.getOrDefault(pm.getUserId().toString(), "Unknown"))
                                    .roleName(Role.TALENT.toString())
                                    .isLeader(pm.isLeader())
                                    .build())
                            .sorted(Comparator.comparing((UserParticipantResponse t) -> !t.isLeader()))
                            .toList();

                    UUID createdByUserId = null;
                    String createdByName = null;
                    String createdByAvatar = null;
                    if (project.getCreatedBy() != null && !project.getCreatedBy().isEmpty()) {
                        try {
                            createdByUserId = UUID.fromString(project.getCreatedBy());
                            UserInfo userInfo = finalUserIdToUserInfoMap.get(project.getCreatedBy());
                            if (userInfo != null) {
                                createdByName = userInfo.getFullName();
                                createdByAvatar = userInfo.getAvatarUrl();
                            } else {
                                // Fallback nếu không có trong map
                                createdByName = finalUserIdToNameMap.getOrDefault(project.getCreatedBy(), "Unknown");
                            }
                        } catch (Exception e) {
                            log.warn("Không thể lấy thông tin người tạo project: {}", e.getMessage());
                        }
                    }

                    UUID currentMilestoneId = null;
                    String currentMilestoneName = null;
                    List<ProjectMilestone> milestones = projectMilestoneRepository.findByProjectId(project.getId());
                    if (milestones != null && !milestones.isEmpty()) {
                        ProjectMilestone currentMilestone = milestones.stream()
                                .filter(m -> Status.ACTIVE.toString().equalsIgnoreCase(m.getStatus()) ||
                                        (!Status.COMPLETED.toString().equalsIgnoreCase(m.getStatus()) &&
                                                !Status.CANCELED.toString().equalsIgnoreCase(m.getStatus())))
                                .min(Comparator.comparing(ProjectMilestone::getStartDate,
                                        Comparator.nullsLast(Comparator.naturalOrder())))
                                .orElse(null);

                        if (currentMilestone != null) {
                            currentMilestoneId = currentMilestone.getId();
                            currentMilestoneName = currentMilestone.getTitle();
                        }
                    }

                    return convertToProjectResponse(project, mentors, talents,
                            createdByUserId, createdByName, createdByAvatar,
                            currentMilestoneId, currentMilestoneName, companyName);
                })
                .collect(Collectors.toList());

        String message = status != null && !status.trim().isEmpty()
                ? String.format("Lấy danh sách dự án của bạn với trạng thái '%s' thành công", status)
                : "Lấy danh sách dự án của bạn thành công";

        return ApiResponse.success(message, projectResponses);
    }

    @Override
    public ApiResponse<PaginatedResult<GetTalentApplicationResponse>> getTalentApplications(
            UUID userId, String search, int page, int size) {

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("updatedAt").descending());

        Page<ProjectApplication> applicationPage;

        if (search != null && !search.trim().isEmpty()) {
            applicationPage = projectApplicationRepository
                    .searchByUserIdAndProjectTitle(userId, search.toLowerCase().trim(), pageable);
        } else {
            applicationPage = projectApplicationRepository
                    .findByUserIdAndNotDeleted(userId, pageable);
        }

        Page<GetTalentApplicationResponse> mappedPage = applicationPage.map(app ->
                GetTalentApplicationResponse.builder()
                        .id(app.getId())
                        .userId(app.getUserId())
                        .projectId(app.getProject() != null ? app.getProject().getId() : null)
                        .projectName(app.getProject() != null ? app.getProject().getTitle() : null)
                        .cvUrl(app.getCvUrl())
                        .status(app.getStatus())
                        .appliedAt(app.getAppliedAt())
                        .updatedAt(app.getUpdatedAt())
                        .build()
        );

        return ApiResponse.success(
                "Lấy danh sách đơn ứng tuyển thành công",
                PaginatedResult.from(mappedPage)
        );
    }

    @Override
    public ApiResponse<PaginatedResult<ProjectResponse>> getRelatedProjects(UUID projectId, int page, int size) {
        Project currentProject = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("Dự án với ID '" + projectId + "' không tồn tại"));

        Set<UUID> skillIds = currentProject.getSkills().stream()
                .map(Skill::getId)
                .collect(Collectors.toSet());

        if (skillIds.isEmpty()) {
            return ApiResponse.success("Không tìm thấy dự án liên quan",
                    PaginatedResult.<ProjectResponse>builder()
                            .data(List.of())
                            .currentPage(page)
                            .totalElements(0)
                            .totalPages(0)
                            .build());
        }

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Project> relatedProjectsPage = projectRepository.findRelatedProjectsBySkills(skillIds, projectId, pageable);

        Set<UUID> companyIds = relatedProjectsPage.getContent().stream()
                .map(Project::getCompanyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, String> companyIdToNameMap = new HashMap<>();
        if (!companyIds.isEmpty()) {
            try {
                CompanyServiceGrpc.CompanyServiceBlockingStub companyStub =
                        CompanyServiceGrpc.newBlockingStub(companyServiceChannel);

                GetCompaniesByIdsRequest request = GetCompaniesByIdsRequest.newBuilder()
                        .addAllCompanyIds(companyIds.stream().map(UUID::toString).toList())
                        .build();

                GetCompaniesByIdsResponse response = companyStub.getCompaniesByIds(request);
                response.getCompanyNamesMap().forEach((id, name) -> {
                    companyIdToNameMap.put(UUID.fromString(id), name);
                });
            } catch (Exception e) {
                log.error("Lỗi khi lấy danh sách công ty qua gRPC: {}", e.getMessage());
            }
        }

        Page<ProjectResponse> responsePage = relatedProjectsPage.map(project -> {
            String companyName = companyIdToNameMap.getOrDefault(project.getCompanyId(), null);
            return convertToProjectResponse(project, List.of(), List.of(), null, null, null, null, null, companyName);
        });

        return ApiResponse.success("Lấy danh sách dự án liên quan thành công", PaginatedResult.from(responsePage));
    }

    private ProjectResponse convertToProjectResponse(Project project) {
        return convertToProjectResponse(project, List.of(), List.of(), null, null, null, null, null, null);
    }


    private ProjectResponse convertToProjectResponse(Project project, List<UserParticipantResponse> mentors, List<UserParticipantResponse> talents,
                                                     UUID createdBy, String createdByName, String createdByAvatar,
                                                     UUID currentMilestoneId, String currentMilestoneName,
                                                     String companyName) {
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
                .isOpenForApplications(project.getIsOpenForApplications())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .budget(project.getBudget())
                .skills(skillResponses)
                .mentors(mentors)
                .talents(talents)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .createdBy(createdBy)
                .createdByName(createdByName)
                .createdByAvatar(createdByAvatar)
                .currentMilestoneId(currentMilestoneId)
                .currentMilestoneName(currentMilestoneName)
                .companyName(companyName)
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
