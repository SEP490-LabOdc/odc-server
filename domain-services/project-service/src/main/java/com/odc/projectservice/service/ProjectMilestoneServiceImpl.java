package com.odc.projectservice.service;

import com.odc.common.constant.ProjectMilestoneStatus;
import com.odc.common.constant.ProjectStatus;
import com.odc.common.constant.Role;
import com.odc.common.constant.Status;
import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.common.exception.BusinessException;
import com.odc.commonlib.event.EventPublisher;
import com.odc.companyservice.v1.CompanyServiceGrpc;
import com.odc.companyservice.v1.GetCompanyByIdRequest;
import com.odc.companyservice.v1.GetCompanyByIdResponse;
import com.odc.notification.v1.Channel;
import com.odc.notification.v1.NotificationEvent;
import com.odc.notification.v1.Target;
import com.odc.notification.v1.UserTarget;
import com.odc.projectservice.dto.request.*;
import com.odc.projectservice.dto.response.FeedbackResponse;
import com.odc.projectservice.dto.response.MilestoneDocumentResponse;
import com.odc.projectservice.dto.response.ProjectMilestoneResponse;
import com.odc.projectservice.dto.response.TalentMentorInfoResponse;
import com.odc.projectservice.entity.*;
import com.odc.projectservice.repository.*;
import com.odc.userservice.v1.GetUsersByIdsRequest;
import com.odc.userservice.v1.GetUsersByIdsResponse;
import com.odc.userservice.v1.UserInfo;
import com.odc.userservice.v1.UserServiceGrpc;
import io.grpc.ManagedChannel;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final MilestoneMemberRepository milestoneMemberRepository;
    private final @Qualifier("userServiceChannel1") ManagedChannel userServiceChannel;
    private final ManagedChannel companyServiceChannel;
    private final EventPublisher eventPublisher;
    private final MilestoneFeedbackRepository milestoneFeedbackRepository;

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

        List<MilestoneAttachment> attachmentList = new ArrayList<>();
        if (request.getAttachmentUrls() != null) {
            request.getAttachmentUrls().forEach(dto ->
                    attachmentList.add(new MilestoneAttachment(
                            UUID.randomUUID(),
                            dto.getName(),
                            dto.getFileName(),
                            dto.getUrl()
                    ))
            );
        }

        BigDecimal remainingBudget = project.getRemainingBudget() != null
                ? project.getRemainingBudget()
                : BigDecimal.ZERO;

        BigDecimal percentageValue = BigDecimal.valueOf(request.getPercentage() / 100);
        BigDecimal milestoneBudget = remainingBudget.multiply(percentageValue);

        if (remainingBudget.subtract(milestoneBudget).compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Ngân sách còn lại của dự án không đủ để tạo milestone với "
                    + request.getPercentage() + "%");
        }

        ProjectMilestone projectMilestone = ProjectMilestone.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(Status.PENDING.toString())
                .project(project)
                .attachmentUrls(attachmentList)
                .budget(milestoneBudget)
                .build();

        ProjectMilestone savedMilestone = projectMilestoneRepository.save(projectMilestone);

        if (projectMilestoneRepository.countByProjectId(request.getProjectId()) == 1) {
            project.setStatus(ProjectStatus.ON_GOING.toString());
        }
        project.setRemainingBudget(remainingBudget.subtract(milestoneBudget));
        projectRepository.save(project);

        UUID currentUserId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        ProjectMember creatorMember = projectMemberRepository
                .findByProject_IdAndUserId(project.getId(), currentUserId);

        if (creatorMember == null)
            throw new BusinessException("Bạn không thuộc dự án này");

        MilestoneMember creatorMilestoneMember = MilestoneMember.builder()
                .projectMilestone(savedMilestone)
                .projectMember(creatorMember)
                .isLeader(true)
                .joinedAt(LocalDateTime.now())
                .build();

        milestoneMemberRepository.save(creatorMilestoneMember);

        try {
            CompanyServiceGrpc.CompanyServiceBlockingStub companyStub =
                    CompanyServiceGrpc.newBlockingStub(companyServiceChannel);

            GetCompanyByIdRequest getCompanyByIdRequest = GetCompanyByIdRequest.newBuilder()
                    .setCompanyId(project.getCompanyId().toString())
                    .build();
            GetCompanyByIdResponse companyDetailResponse = companyStub.getCompanyById(getCompanyByIdRequest);

            if (companyDetailResponse.getUserId() != null && !companyDetailResponse.getUserId().isEmpty()) {
                UUID companyUserId = UUID.fromString(companyDetailResponse.getUserId());

                UserTarget userTarget = UserTarget.newBuilder()
                        .addUserIds(companyUserId.toString())
                        .build();

                Target target = Target.newBuilder()
                        .setUser(userTarget)
                        .build();

                Map<String, String> dataMap = Map.of(
                        "projectId", project.getId().toString(),
                        "milestoneId", savedMilestone.getId().toString(),
                        "milestoneTitle", savedMilestone.getTitle()
                );

                NotificationEvent notificationEvent = NotificationEvent.newBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setType("NEW_PROJECT_MILESTONE_CREATED")
                        .setTitle("Milestone mới được tạo")
                        .setContent("Milestone \"" + savedMilestone.getTitle() + "\" của dự án \"" + project.getTitle() + "\" đã được tạo.")
                        .putAllData(dataMap)
                        .setDeepLink("/projects/" + project.getId() + "/milestones/" + savedMilestone.getId())
                        .setPriority("HIGH")
                        .setTarget(target)
                        .addChannels(Channel.WEB)
                        .setCreatedAt(System.currentTimeMillis())
                        .setCategory("PROJECT_MANAGEMENT")
                        .build();

                eventPublisher.publish("notifications", notificationEvent);
                log.info("Notification event published to company user: {}", companyUserId);
            }
        } catch (Exception e) {
            log.error("Không thể lấy thông tin công ty qua gRPC để gửi thông báo: {}", e.getMessage(), e);
        }
        ProjectMilestoneResponse responseData = ProjectMilestoneResponse.builder()
                .id(savedMilestone.getId())
                .projectId(savedMilestone.getProject().getId())
                .title(savedMilestone.getTitle())
                .description(savedMilestone.getDescription())
                .startDate(savedMilestone.getStartDate())
                .endDate(savedMilestone.getEndDate())
                .status(savedMilestone.getStatus())
                .attachments(savedMilestone.getAttachmentUrls())
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

        List<MilestoneAttachment> currentAttachments = existingMilestone.getAttachmentUrls() != null
                ? new ArrayList<>(existingMilestone.getAttachmentUrls())
                : new ArrayList<>();

        List<MilestoneAttachment> updatedAttachments = new ArrayList<>();

        if (request.getAttachmentUrls() != null) {
            for (UpdateMilestoneAttachmentRequest dto : request.getAttachmentUrls()) {
                Optional<MilestoneAttachment> existingFileOpt = currentAttachments.stream()
                        .filter(a -> a.getFileName().equals(dto.getFileName()))
                        .findFirst();

                if (existingFileOpt.isPresent()) {
                    MilestoneAttachment existingFile = existingFileOpt.get();
                    existingFile.setName(dto.getName());
                    existingFile.setUrl(dto.getUrl());
                    updatedAttachments.add(existingFile);
                } else {
                    updatedAttachments.add(new MilestoneAttachment(UUID.randomUUID(), dto.getName(), dto.getFileName(), dto.getUrl()));
                }
            }
        }

        existingMilestone.setAttachmentUrls(updatedAttachments);

        ProjectMilestone updatedMilestone = projectMilestoneRepository.save(existingMilestone);

        ProjectMilestoneResponse responseData = ProjectMilestoneResponse.builder()
                .id(updatedMilestone.getId())
                .projectId(updatedMilestone.getProject().getId())
                .title(updatedMilestone.getTitle())
                .description(updatedMilestone.getDescription())
                .budget(updatedMilestone.getBudget())
                .startDate(updatedMilestone.getStartDate())
                .endDate(updatedMilestone.getEndDate())
                .status(updatedMilestone.getStatus())
                .attachments(updatedMilestone.getAttachmentUrls() != null ? updatedMilestone.getAttachmentUrls() : List.of())
                .build();

        return ApiResponse.success("Cập nhật milestone dự án thành công", responseData);
    }

    @Override
    public ApiResponse<List<ProjectMilestoneResponse>> getAllProjectMilestones() {
        List<ProjectMilestone> milestones = projectMilestoneRepository.findAll();
        List<UUID> milestoneIds = milestones.stream()
                .map(ProjectMilestone::getId)
                .toList();

        // Lấy tất cả MilestoneMember active
        List<MilestoneMember> allMembers = milestoneMemberRepository.findByProjectMilestone_IdInAndIsActive(milestoneIds, true);

        // Map milestoneId -> List<MilestoneMember>
        Map<UUID, List<MilestoneMember>> milestoneMembersMap = allMembers.stream()
                .collect(Collectors.groupingBy(mm -> mm.getProjectMilestone().getId()));

        // Lấy tất cả userId để fetch thông tin
        List<UUID> allUserIds = allMembers.stream()
                .map(mm -> mm.getProjectMember().getUserId())
                .distinct()
                .toList();

        Map<UUID, UserInfo> userMap = getUserMapByIds(allUserIds); // helper fetch user info từ gRPC

        // Map milestone -> response
        List<ProjectMilestoneResponse> milestoneResponses = milestones.stream()
                .map(m -> convertToProjectMilestoneResponse(
                        m,
                        milestoneMembersMap.getOrDefault(m.getId(), List.of()),
                        userMap
                ))
                .toList();

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
        List<UUID> milestoneIds = milestones.stream().map(ProjectMilestone::getId).toList();

        // Lấy tất cả MilestoneMember active
        List<MilestoneMember> allMembers = milestoneMemberRepository.findByProjectMilestone_IdInAndIsActive(milestoneIds, true);

        // Map milestoneId -> List<MilestoneMember>
        Map<UUID, List<MilestoneMember>> milestoneMembersMap = allMembers.stream()
                .collect(Collectors.groupingBy(mm -> mm.getProjectMilestone().getId()));

        // Lấy tất cả userId để fetch thông tin
        List<UUID> allUserIds = allMembers.stream()
                .map(mm -> mm.getProjectMember().getUserId())
                .distinct()
                .toList();

        Map<UUID, UserInfo> userMap = getUserMapByIds(allUserIds);

        List<ProjectMilestoneResponse> milestoneResponses = milestones.stream()
                .map(m -> convertToProjectMilestoneResponse(
                        m,
                        milestoneMembersMap.getOrDefault(m.getId(), List.of()),
                        userMap
                ))
                .toList();

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

        List<MilestoneMember> milestoneMembers = milestoneMemberRepository
                .findByProjectMilestone_IdAndIsActive(milestoneId, true);

        List<UUID> allUserIds = milestoneMembers.stream()
                .map(mm -> mm.getProjectMember().getUserId())
                .distinct()
                .toList();

        Map<UUID, UserInfo> userMap = getUserMapByIds(allUserIds);

        ProjectMilestoneResponse responseData = convertToProjectMilestoneResponse(
                milestone,
                milestoneMembers,
                userMap
        );

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
    public ApiResponse<ProjectMilestoneResponse> updateMilestoneStatusToOngoing(UUID milestoneId) {
        UUID mentorId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        ProjectMilestone milestone = projectMilestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new BusinessException("Milestone với ID '" + milestoneId + "' không tồn tại"));

        Project project = milestone.getProject();

        if (ProjectStatus.COMPLETED.toString().equalsIgnoreCase(project.getStatus())) {
            throw new BusinessException("Không thể start milestone vì dự án đã hoàn thành");
        }

        ProjectMember mentorMember = projectMemberRepository
                .findByProjectIdAndUserIdAndRole(project.getId(), mentorId, Role.MENTOR.toString());
        if (mentorMember == null) {
            throw new BusinessException("Bạn không phải mentor của dự án này");
        }

        if (!ProjectMilestoneStatus.PENDING_START.toString().equalsIgnoreCase(milestone.getStatus())) {
            throw new BusinessException("Không thể cập nhật trạng thái. Milestone hiện không ở trạng thái 'PENDING_START'.");
        }

        List<MilestoneMember> activeMembers = milestoneMemberRepository.findByProjectMilestone_IdAndIsActive(milestoneId, true);

        if (activeMembers.isEmpty()) {
            throw new BusinessException("Không thể bắt đầu Milestone vì chưa có thành viên nào tham gia.");
        }

        boolean hasMentorLeader = activeMembers.stream().anyMatch(mm ->
                mm.isLeader() && Role.MENTOR.toString().equalsIgnoreCase(mm.getProjectMember().getRoleInProject()));

        boolean hasTalentLeader = activeMembers.stream().anyMatch(mm ->
                mm.isLeader() && Role.TALENT.toString().equalsIgnoreCase(mm.getProjectMember().getRoleInProject()));

        if (!hasMentorLeader) {
            throw new BusinessException("Milestone cần ít nhất 1 Mentor làm Leader trước khi bắt đầu.");
        }
        if (!hasTalentLeader) {
            throw new BusinessException("Milestone cần ít nhất 1 Talent làm Leader trước khi bắt đầu.");
        }

        LocalDate today = LocalDate.now();
        if (milestone.getStartDate() != null && today.isBefore(milestone.getStartDate())) {
            milestone.setStartDate(today);
        }

        milestone.setStatus(ProjectMilestoneStatus.ON_GOING.toString());
        milestone.setUpdatedBy(mentorId.toString());
        projectMilestoneRepository.save(milestone);

        if (!ProjectStatus.ON_GOING.toString().equalsIgnoreCase(project.getStatus())) {
            project.setStatus(ProjectStatus.ON_GOING.toString());
            projectRepository.save(project);
        }

        ProjectMilestoneResponse responseData = ProjectMilestoneResponse.builder()
                .id(milestone.getId())
                .projectId(project.getId())
                .title(milestone.getTitle())
                .description(milestone.getDescription())
                .budget(milestone.getBudget())
                .startDate(milestone.getStartDate())
                .endDate(milestone.getEndDate())
                .status(milestone.getStatus())
                .attachments(milestone.getAttachmentUrls() != null ? milestone.getAttachmentUrls() : List.of())
                .build();

        return ApiResponse.success("Thay đổi trạng thái của milestone thành công.", responseData);
    }

    @Override
    public ApiResponse<Void> deleteMilestoneAttachment(UUID milestoneId, UUID attachmentId) {
        ProjectMilestone milestone = projectMilestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new BusinessException(
                        "Milestone với ID '" + milestoneId + "' không tồn tại"
                ));

        if (milestone.getAttachmentUrls() == null || milestone.getAttachmentUrls().isEmpty()) {
            throw new BusinessException("Milestone không có attachment nào để xóa");
        }

        List<MilestoneAttachment> updatedAttachments = milestone.getAttachmentUrls().stream()
                .filter(a -> !a.getId().equals(attachmentId))
                .toList();

        if (updatedAttachments.size() == milestone.getAttachmentUrls().size()) {
            throw new BusinessException("Attachment với ID '" + attachmentId + "' không tồn tại");
        }

        milestone.setAttachmentUrls(updatedAttachments);
        projectMilestoneRepository.save(milestone);

        return ApiResponse.success("Xóa attachment thành công", null);
    }

    @Override
    public ApiResponse<Void> approveProjectPlan(UUID milestoneId) {
        ProjectMilestone milestone = projectMilestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new BusinessException("Milestone với ID '" + milestoneId + "' không tồn tại"));

        if (!Status.PENDING.toString().equalsIgnoreCase(milestone.getStatus())) {
            throw new BusinessException("Chỉ milestone ở trạng thái PENDING mới có thể được approve");
        }

        milestone.setStatus(ProjectMilestoneStatus.PENDING_START.toString());
        projectMilestoneRepository.save(milestone);

        Project project = milestone.getProject();
        List<ProjectMember> targetMembers = projectMemberRepository.findByProjectId(project.getId()).stream()
                .filter(pm -> Role.MENTOR.toString().equalsIgnoreCase(pm.getRoleInProject()))
                .toList();

        if (!targetMembers.isEmpty()) {
            Set<UUID> userIds = targetMembers.stream()
                    .map(ProjectMember::getUserId)
                    .collect(Collectors.toSet());

            Map<String, String> dataMap = new HashMap<>();
            dataMap.put("projectId", project.getId().toString());
            dataMap.put("projectTitle", project.getTitle());

            NotificationEvent notificationEvent = NotificationEvent.newBuilder()
                    .setId(UUID.randomUUID().toString())
                    .setType("PROJECT_PLAN_APPROVED")
                    .setTitle("Dự án đã được khách hàng duyệt")
                    .setContent("Project plan của dự án \"" + project.getTitle() + "\" đã được khách hàng duyệt. Chuẩn bị bắt đầu.")
                    .putAllData(dataMap)
                    .setDeepLink("/projects/" + project.getId())
                    .setPriority("HIGH")
                    .setTarget(Target.newBuilder()
                            .setUser(UserTarget.newBuilder()
                                    .addAllUserIds(userIds.stream().map(UUID::toString).toList())
                                    .build())
                            .build())
                    .addChannels(Channel.WEB)
                    .setCreatedAt(System.currentTimeMillis())
                    .setCategory("PROJECT_MANAGEMENT")
                    .build();

            eventPublisher.publish("notifications", notificationEvent);
            log.info("Notification event published to mentors/leaders successfully.");
        }

        return ApiResponse.success("Milestone đã được approve và chuyển sang PENDING_START", null);
    }

    @Override
    public ApiResponse<Void> rejectProjectMilestone(UUID milestoneId, MilestoneRejectRequest request) {

        ProjectMilestone milestone = projectMilestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new BusinessException("Milestone với ID '" + milestoneId + "' không tồn tại"));

        if (!ProjectMilestoneStatus.PENDING.toString().equalsIgnoreCase(milestone.getStatus())) {
            throw new BusinessException("Chỉ milestone ở trạng thái PENDING mới có thể bị reject");
        }

        MilestoneFeedback feedback = MilestoneFeedback.builder()
                .milestone(milestone)
                .userId((UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .content(request.getFeedbackContent())
                .attachmentUrls(request.getAttachmentUrls())
                .build();

        milestoneFeedbackRepository.save(feedback);

        // 2️⃣ Update status của milestone
        milestone.setStatus(ProjectMilestoneStatus.UPDATE_REQUIRED.toString());
        projectMilestoneRepository.save(milestone);


        // 3️⃣ Lấy leader + mentor để gửi notify
        Project project = milestone.getProject();

        List<ProjectMember> targetMembers = projectMemberRepository.findByProjectId(project.getId())
                .stream()
                .filter(pm -> Role.MENTOR.toString().equalsIgnoreCase(pm.getRoleInProject()))
                .toList();

        if (!targetMembers.isEmpty()) {

            Set<UUID> userIds = targetMembers.stream()
                    .map(ProjectMember::getUserId)
                    .collect(Collectors.toSet());

            Map<String, String> dataMap = new HashMap<>();
            dataMap.put("projectId", project.getId().toString());
            dataMap.put("projectTitle", project.getTitle());
            dataMap.put("milestoneId", milestone.getId().toString());

            NotificationEvent notificationEvent = NotificationEvent.newBuilder()
                    .setId(UUID.randomUUID().toString())
                    .setType("PROJECT_PLAN_REJECTED")
                    .setTitle("Milestone bị khách hàng từ chối")
                    .setContent("Khách hàng đã từ chối milestone \"" + milestone.getTitle() + "\". Hãy xem feedback và cập nhật lại.")
                    .putAllData(dataMap)
                    .setDeepLink("/mentor/projects/" + project.getId() + "/" + milestone.getId())
                    .setPriority("HIGH")
                    .setTarget(Target.newBuilder()
                            .setUser(UserTarget.newBuilder()
                                    .addAllUserIds(userIds.stream().map(UUID::toString).toList())
                                    .build())
                            .build())
                    .addChannels(Channel.WEB)
                    .setCreatedAt(System.currentTimeMillis())
                    .setCategory("PROJECT_MANAGEMENT")
                    .build();

            eventPublisher.publish("notifications", notificationEvent);
            log.info("Notification sent to mentor/leader: milestone rejected.");
        }

        return ApiResponse.success("Milestone đã bị reject & status chuyển sang UPDATE_REQUIRED", null);
    }

    @Override
    public ApiResponse<PaginatedResult<FeedbackResponse>> getMilestoneFeedbacks(
            UUID milestoneId,
            Integer page,
            Integer size
    ) {
        int pageIndex = (page != null && page > 0) ? page - 1 : 0;
        int pageSize = (size != null && size > 0) ? size : 10;

        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<MilestoneFeedback> feedbackPage =
                milestoneFeedbackRepository.findByMilestoneId(milestoneId, pageable);

        Page<FeedbackResponse> mappedPage = feedbackPage.map(this::mapToFeedbackResponse);

        return ApiResponse.success(PaginatedResult.from(mappedPage));
    }

    @Override
    public ApiResponse<ProjectMilestoneResponse> addMilestoneAttachments(UUID milestoneId, AddMilestoneAttachmentsRequest request) {

        ProjectMilestone milestone = projectMilestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new BusinessException(
                        "Milestone với ID '" + milestoneId + "' không tồn tại"
                ));

        if (Boolean.TRUE.equals(milestone.getIsDeleted())) {
            throw new BusinessException("Milestone với ID '" + milestoneId + "' đã bị xóa");
        }

        List<MilestoneAttachment> currentAttachments = milestone.getAttachmentUrls() != null
                ? new ArrayList<>(milestone.getAttachmentUrls())
                : new ArrayList<>();

        List<MilestoneAttachment> newAttachments = new ArrayList<>();
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            for (CreateMilestoneAttachmentRequest dto : request.getAttachments()) {
                MilestoneAttachment newAttachment = new MilestoneAttachment(
                        UUID.randomUUID(),
                        dto.getName(),
                        dto.getFileName(),
                        dto.getUrl()
                );
                newAttachments.add(newAttachment);
            }
        }

        List<MilestoneAttachment> mergedAttachments = new ArrayList<>(currentAttachments);
        mergedAttachments.addAll(newAttachments);

        UUID mentorId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        milestone.setAttachmentUrls(mergedAttachments);
        milestone.setUpdatedBy(mentorId.toString()); // Convert UUID to String

        ProjectMilestone updatedMilestone = projectMilestoneRepository.save(milestone);

        ProjectMilestoneResponse responseData = ProjectMilestoneResponse.builder()
                .id(updatedMilestone.getId())
                .projectId(updatedMilestone.getProject().getId())
                .title(updatedMilestone.getTitle())
                .description(updatedMilestone.getDescription())
                .budget(updatedMilestone.getBudget())
                .startDate(updatedMilestone.getStartDate())
                .endDate(updatedMilestone.getEndDate())
                .status(updatedMilestone.getStatus())
                .attachments(updatedMilestone.getAttachmentUrls() != null
                        ? updatedMilestone.getAttachmentUrls()
                        : List.of())
                .build();

        return ApiResponse.success("Thêm attachments cho milestone thành công", responseData);
    }

    @Override
    public ApiResponse<List<MilestoneDocumentResponse>> getMilestoneDocuments(UUID milestoneId) {
        ProjectMilestone milestone = projectMilestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new BusinessException(
                        "Milestone với ID '" + milestoneId + "' không tồn tại"
                ));

        if (Boolean.TRUE.equals(milestone.getIsDeleted())) {
            throw new BusinessException("Milestone với ID '" + milestoneId + "' đã bị xóa");
        }

        List<MilestoneAttachment> attachments = milestone.getAttachmentUrls() != null
                ? milestone.getAttachmentUrls()
                : new ArrayList<>();

        List<MilestoneDocumentResponse> documents = attachments.stream()
                .map(attachment -> MilestoneDocumentResponse.builder()
                        .id(attachment.getId())
                        .fileName(attachment.getFileName())
                        .fileUrl(attachment.getUrl())
                        .s3Key(null) // MilestoneAttachment không có s3Key, có thể để null hoặc extract từ URL
                        .uploadedAt(milestone.getUpdatedAt() != null ? milestone.getUpdatedAt() : milestone.getCreatedAt())
                        .entityId(milestoneId.toString())
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.success("Lấy danh sách tài liệu milestone thành công", documents);
    }

    private FeedbackResponse mapToFeedbackResponse(MilestoneFeedback fb) {
        FeedbackResponse.FeedbackResponseBuilder builder = FeedbackResponse.builder()
                .id(fb.getId())
                .userId(fb.getUserId())
                .content(fb.getContent())
                .attachmentUrls(fb.getAttachmentUrls())
                .createdAt(fb.getCreatedAt());

        return builder.build();
    }

    private ProjectMilestoneResponse convertToProjectMilestoneResponse(
            ProjectMilestone milestone,
            List<MilestoneMember> milestoneMembers,
            Map<UUID, UserInfo> userMap) {

        Map<Role, List<TalentMentorInfoResponse>> memberMap = milestoneMembers.stream()
                .collect(Collectors.groupingBy(
                        mm -> Role.valueOf(mm.getProjectMember().getRoleInProject()),
                        Collectors.collectingAndThen(
                                Collectors.toMap(
                                        mm -> mm.getProjectMember().getUserId(),
                                        mm -> buildMemberResponse(mm, userMap),
                                        (existing, replacement) -> existing
                                ),
                                map -> map.values().stream().toList()
                        )
                ));

        return ProjectMilestoneResponse.builder()
                .id(milestone.getId())
                .projectId(milestone.getProject().getId())
                .projectName(milestone.getProject().getTitle())
                .title(milestone.getTitle())
                .description(milestone.getDescription())
                .budget(milestone.getBudget())
                .startDate(milestone.getStartDate())
                .endDate(milestone.getEndDate())
                .status(milestone.getStatus())
                .attachments(
                        milestone.getAttachmentUrls() != null
                                ? milestone.getAttachmentUrls()
                                : List.of()
                )
                .talents(memberMap.getOrDefault(Role.TALENT, List.of()))
                .mentors(memberMap.getOrDefault(Role.MENTOR, List.of()))
                .build();
    }

    private TalentMentorInfoResponse buildMemberResponse(
            MilestoneMember mm,
            Map<UUID, UserInfo> userMap) {

        ProjectMember pm = mm.getProjectMember();
        UserInfo userInfo = userMap.get(pm.getUserId());

        return TalentMentorInfoResponse.builder()
                .userId(pm.getUserId())
                .name(userInfo != null ? userInfo.getFullName() : "Unknown")
                .avatar(userInfo != null ? userInfo.getAvatarUrl() : "")
                .email(userInfo != null ? userInfo.getEmail() : "")
                .phone(userInfo != null ? userInfo.getPhone() : "")
                .isLeader(mm.isLeader()) // ✅ milestone-level leader
                .build();
    }

    private Map<UUID, UserInfo> getUserMapByIds(List<UUID> userIds) {
        if (userIds.isEmpty()) return Map.of();

        try {
            UserServiceGrpc.UserServiceBlockingStub userStub = UserServiceGrpc.newBlockingStub(userServiceChannel);
            GetUsersByIdsResponse usersResponse = userStub.getUsersByIds(
                    GetUsersByIdsRequest.newBuilder()
                            .addAllUserId(userIds.stream().map(UUID::toString).toList())
                            .build()
            );
            return usersResponse.getUsersList().stream()
                    .collect(Collectors.toMap(
                            u -> UUID.fromString(u.getUserId()),
                            u -> u
                    ));
        } catch (Exception e) {
            log.error("Lỗi khi lấy thông tin user: {}", e.getMessage());
            return Map.of();
        }
    }
}
