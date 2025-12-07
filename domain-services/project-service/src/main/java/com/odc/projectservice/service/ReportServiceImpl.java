package com.odc.projectservice.service;

import com.odc.common.constant.*;
import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.common.exception.BusinessException;
import com.odc.common.exception.ResourceNotFoundException;
import com.odc.companyservice.v1.CompanyServiceGrpc;
import com.odc.companyservice.v1.GetCompanyByIdRequest;
import com.odc.projectservice.dto.request.CreateReportRequest;
import com.odc.projectservice.dto.request.UpdateReportRequest;
import com.odc.projectservice.dto.request.UpdateReportStatusRequest;
import com.odc.projectservice.dto.response.ReportResponse;
import com.odc.projectservice.entity.Project;
import com.odc.projectservice.entity.ProjectMilestone;
import com.odc.projectservice.entity.Report;
import com.odc.projectservice.repository.ProjectMemberRepository;
import com.odc.projectservice.repository.ProjectMilestoneRepository;
import com.odc.projectservice.repository.ProjectRepository;
import com.odc.projectservice.repository.ReportRepository;
import com.odc.userservice.v1.GetUsersByIdsRequest;
import com.odc.userservice.v1.GetUsersByIdsResponse;
import com.odc.userservice.v1.UserInfo;
import com.odc.userservice.v1.UserServiceGrpc;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class ReportServiceImpl implements ReportService {

    private static final Set<String> ALLOWED_REPORT_STATUSES = Set.of(
            ProjectStatus.PLANNING.toString(),
            ProjectStatus.ON_GOING.toString(),
            ProjectStatus.PAUSED.toString(),
            ProjectStatus.COMPLETED.toString()
    );
    private final ReportRepository reportRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMilestoneRepository projectMilestoneRepository;
    @Qualifier("userServiceChannel1")
    private final ManagedChannel userServiceChannel;
    @Qualifier("companyServiceChannel")
    private final ManagedChannel companyServiceChannel;

    public ReportServiceImpl(ReportRepository reportRepository,
                             ProjectRepository projectRepository,
                             ProjectMemberRepository projectMemberRepository,
                             ProjectMilestoneRepository projectMilestoneRepository,
                             ManagedChannel userServiceChannel,
                             ManagedChannel companyServiceChannel) {
        this.reportRepository = reportRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectMilestoneRepository = projectMilestoneRepository;
        this.userServiceChannel = userServiceChannel;
        this.companyServiceChannel = companyServiceChannel;
    }

    @Override
    public ApiResponse<List<ReportResponse>> createReport(UUID userId, CreateReportRequest request) {
        // 1. Validate Project & Status
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new BusinessException("Dự án không tồn tại"));

        if (!ALLOWED_REPORT_STATUSES.contains(project.getStatus())) {
            throw new BusinessException(
                    "Không thể tạo báo cáo khi dự án đang ở trạng thái: " + project.getStatus()
            );
        }

        // 2. Validate Input Content
        if (ReportType.DAILY_REPORT.toString().equals(request.getReportType())) {
            if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                throw new BusinessException("Daily Report yêu cầu nội dung text");
            }
        } else {
            if (request.getAttachmentsUrl() == null || request.getAttachmentsUrl().isEmpty()) {
                throw new BusinessException("Báo cáo này yêu cầu đính kèm file");
            }
        }

        // 3. Validate Milestone (nếu có)
        ProjectMilestone milestone = null;
        if (ReportType.MILESTONE_REPORT.toString().equals(request.getReportType())) {
            if (request.getMilestoneId() == null) {
                throw new BusinessException("Báo cáo milestone yêu cầu phải có milestoneId");
            }
            milestone = projectMilestoneRepository.findById(request.getMilestoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Milestone không tồn tại"));

            if (!milestone.getProject().getId().equals(project.getId())) {
                throw new BusinessException("Milestone không thuộc về dự án này");
            }
        }

        List<UUID> validRecipientIds = projectMemberRepository.findUserIdsByProjectIdAndUserIdIn(
                project.getId(),
                request.getRecipientIds()
        );

        // 2. Tìm ra những ID không nằm trong danh sách hợp lệ
        List<UUID> invalidRecipientIds = request.getRecipientIds().stream()
                .filter(id -> !validRecipientIds.contains(id))
                .toList();

        // 3. Nếu có ID không hợp lệ, báo lỗi ngay lập tức (hoặc log warning tùy business)
        if (!invalidRecipientIds.isEmpty()) {
            throw new BusinessException("Các người dùng sau không phải thành viên dự án: " + invalidRecipientIds);
        }

        // 4. Lặp qua danh sách người nhận và tạo Report
        List<Report> savedReports = new ArrayList<>();
        List<UUID> recipientIds = request.getRecipientIds();


        for (UUID recipientId : recipientIds) {
            // Tránh trường hợp tự gửi cho chính mình (tùy nghiệp vụ)
            if (recipientId.equals(userId)) {
                throw new BusinessException("Bạn không thể tự gửi report cho chính mình.");
            }

            Report report = Report.builder()
                    .project(project)
                    .reporterId(userId)
                    .recipientId(recipientId) // Set từng người nhận
                    .reportType(request.getReportType())
                    .content(request.getContent())
                    .attachmentsUrl(request.getAttachmentsUrl())
                    .reportingDate(LocalDate.now())
                    .status(ReportStatus.SUBMITTED.toString())
                    .milestone(milestone)
                    .build();

            savedReports.add(report);
        }

        if (savedReports.isEmpty()) {
            throw new BusinessException("Danh sách người nhận không hợp lệ hoặc rỗng");
        }

        // Lưu Batch (Tối ưu hơn lưu từng cái trong vòng lặp)
        List<Report> results = reportRepository.saveAll(savedReports);

        // 5. Prepare Response (Fetch User Info 1 lần để tối ưu performance)
        // Lấy thông tin người tạo (reporter)
        Map<String, UserInfo> userMap = fetchUserInfoBatch(List.of(userId));

        List<ReportResponse> responseList = results.stream()
                .map(r -> mapToResponse(r, userMap))
                .collect(Collectors.toList());

        return ApiResponse.success("Đã gửi báo cáo thành công tới " + results.size() + " người nhận", responseList);
    }

    @Override
    public ApiResponse<ReportResponse> updateReport(UUID userId, UUID reportId, UpdateReportRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Báo cáo không tồn tại"));

        if (!report.getReporterId().equals(userId)) {
            throw new BusinessException("Không có quyền sửa báo cáo này");
        }
        if (!ReportStatus.SUBMITTED.toString().equals(report.getStatus())) {
            throw new BusinessException("Chỉ sửa được khi trạng thái là SUBMITTED");
        }

        if (request.getContent() != null) report.setContent(request.getContent());
        if (request.getAttachmentsUrl() != null) report.setAttachmentsUrl(request.getAttachmentsUrl());

        Report updatedReport = reportRepository.save(report);
        Map<String, UserInfo> userMap = fetchUserInfoBatch(List.of(userId));

        return ApiResponse.success("Cập nhật thành công", mapToResponse(updatedReport, userMap));
    }

    @Override
    public ApiResponse<Void> reviewReport(UUID userId, UUID reportId, UpdateReportStatusRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Báo cáo không tồn tại"));

        ProjectMilestone projectMilestone = report.getMilestone();
        if (projectMilestone != null &&
                !projectMilestone.getId().equals(request.getMilestoneId())) {
            throw new BusinessException("Report hiện tại không phải của milestone với id: " + request.getMilestoneId());
        }

        if (projectMilestone != null) {
            if (Status.APPROVED.toString().equalsIgnoreCase(report.getStatus())) {
                projectMilestone.setStatus(ProjectMilestoneStatus.COMPLETED.toString());
            } else {
                projectMilestone.setStatus(ProjectMilestoneStatus.UPDATE_REQUIRED.toString());
            }
            projectMilestoneRepository.save(projectMilestone);
        }

        boolean isRecipient = report.getRecipientId() != null && report.getRecipientId().equals(userId);

        if (!isRecipient) {
            throw new BusinessException("Bạn không có quyền duyệt báo cáo này");
        }

        report.setStatus(request.getStatus());
        report.setFeedback(request.getFeedback());
        reportRepository.save(report);

        return ApiResponse.success("Đã cập nhật trạng thái báo cáo", null);
    }

    @Override
    public ApiResponse<PaginatedResult<ReportResponse>> getReceivedReports(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("reportingDate").descending());
        Page<Report> reportPage;

        // Nếu là Admin, xem cả report gửi cho mình và report hệ thống (null)
        if (isUserAdmin()) {
            reportPage = reportRepository.findByRecipientIdIsNull(pageable);
            // Note: Có thể cần logic merge cả findByRecipientId(userId) nếu muốn admin nhận cả 2 loại
        } else {
            reportPage = reportRepository.findByRecipientId(userId, pageable);
        }

        return ApiResponse.success(mapPageToResponse(reportPage));
    }

    @Override
    public ApiResponse<PaginatedResult<ReportResponse>> getSentReports(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("reportingDate").descending());
        Page<Report> reportPage = reportRepository.findByReporterId(userId, pageable);
        return ApiResponse.success(mapPageToResponse(reportPage));
    }

    @Override
    public ApiResponse<PaginatedResult<ReportResponse>> getProjectReports(UUID projectId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("reportingDate").descending());
        Page<Report> reportPage = reportRepository.findByProjectId(projectId, pageable);
        return ApiResponse.success(mapPageToResponse(reportPage));
    }

    @Override
    public ApiResponse<PaginatedResult<ReportResponse>> getReportsByMilestoneId(UUID milestoneId, int page, int size) {
        projectMilestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone không tồn tại"));

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("reportingDate").descending());
        Page<Report> reportPage = reportRepository.findByMilestone_Id(milestoneId, pageable);

        return ApiResponse.success(mapPageToResponse(reportPage));
    }

    @Override
    public ApiResponse<ReportResponse> getReportDetail(UUID reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Báo cáo không tồn tại"));

        Map<String, UserInfo> userMap = fetchUserInfoBatch(List.of(report.getReporterId()));
        return ApiResponse.success(mapToResponse(report, userMap));
    }

    // --- Helper Methods & Logic ---

    private UUID resolveRecipient(Project project, String role, boolean isLeader) {
        // Talent -> Leader
//        if (Role.TALENT.toString().equalsIgnoreCase(role)) {
//            return isLeader ? findFirstMentorId(project.getId()) : findLeaderId(project.getId());
//        }

        // Mentor/Lab Admin -> Company
        if (Role.MENTOR.toString().equalsIgnoreCase(role) || Role.LAB_ADMIN.toString().equalsIgnoreCase(role)) {
            return getCompanyUserId(project.getCompanyId());
        }
        throw new BusinessException("Không xác định được luồng báo cáo cho role: " + role);
    }

//    private UUID findLeaderId(UUID projectId) {
//        List<ProjectMember> leaders = projectMemberRepository.findByProjectIdAndRoleAndIsLeaderTrue(projectId, Role.TALENT.toString());
//        if (leaders.isEmpty()) return findFirstMentorId(projectId);
//        return leaders.get(0).getUserId();
//    }
//
//    private UUID findFirstMentorId(UUID projectId) {
//        List<ProjectMember> mentors = projectMemberRepository.findByProjectIdAndRole(projectId, Role.MENTOR.toString());
//        if (mentors.isEmpty()) throw new BusinessException("Dự án chưa có Mentor");
//        return mentors.get(0).getUserId();
//    }

    private UUID getCompanyUserId(UUID companyId) {
        try {
            var stub = CompanyServiceGrpc.newBlockingStub(companyServiceChannel);
            var res = stub.getCompanyById(GetCompanyByIdRequest.newBuilder().setCompanyId(companyId.toString()).build());
            if (res.getUserId().isEmpty())
                throw new BusinessException("Công ty chưa có tài khoản User liên kết");
            return UUID.fromString(res.getUserId());
        } catch (Exception e) {
            throw new BusinessException("Lỗi kết nối Company Service: " + e.getMessage());
        }
    }

    private String getSystemRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !auth.getAuthorities().isEmpty()) {
            return auth.getAuthorities().iterator().next().getAuthority();
        }
        return "UNKNOWN";
    }

    private boolean isUserAdmin() {
        String role = getSystemRole();
        return Role.LAB_ADMIN.toString().equalsIgnoreCase(role) || Role.SYSTEM_ADMIN.toString().equalsIgnoreCase(role);
    }

    /**
     * TỐI ƯU: Gom danh sách ID để gọi gRPC 1 lần
     */
    private PaginatedResult<ReportResponse> mapPageToResponse(Page<Report> reportPage) {
        if (reportPage.isEmpty()) {
            return PaginatedResult.from(reportPage.map(r -> mapToResponse(r, Map.of())));
        }

        List<UUID> reporterIds = reportPage.getContent().stream()
                .map(Report::getReporterId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<String, UserInfo> userMap = fetchUserInfoBatch(reporterIds);

        return PaginatedResult.from(reportPage.map(report -> mapToResponse(report, userMap)));
    }

    private Map<String, UserInfo> fetchUserInfoBatch(List<UUID> userIds) {
        if (userIds.isEmpty()) return Map.of();
        try {
            List<String> stringIds = userIds.stream().map(UUID::toString).toList();
            var stub = UserServiceGrpc.newBlockingStub(userServiceChannel);

            GetUsersByIdsResponse response = stub.getUsersByIds(
                    GetUsersByIdsRequest.newBuilder().addAllUserId(stringIds).build()
            );

            return response.getUsersList().stream()
                    .collect(Collectors.toMap(UserInfo::getUserId, Function.identity(), (v1, v2) -> v1));
        } catch (Exception e) {
            log.error("gRPC Error fetching users: {}", e.getMessage());
            return Map.of();
        }
    }

    private ReportResponse mapToResponse(Report r, Map<String, UserInfo> userMap) {
        String reporterIdKey = Optional.ofNullable(r.getReporterId()).map(UUID::toString).orElse("");
        UserInfo userInfo = userMap.get(reporterIdKey);

        return ReportResponse.builder()
                .id(r.getId())
                .projectId(Optional.ofNullable(r.getProject()).map(Project::getId).orElse(null))
                .projectName(Optional.ofNullable(r.getProject()).map(Project::getTitle).orElse("Unknown Project"))
                .reporterId(r.getReporterId())
                .reporterName(Optional.ofNullable(userInfo).map(UserInfo::getFullName).orElse("Unknown"))
                .reporterEmail(Optional.ofNullable(userInfo).map(UserInfo::getEmail).orElse(""))
                .reporterAvatar(Optional.ofNullable(userInfo).map(UserInfo::getAvatarUrl).orElse(""))
                .recipientId(r.getRecipientId())
                .reportType(r.getReportType())
                .status(r.getStatus())
                .content(r.getContent())
                .attachmentsUrl(r.getAttachmentsUrl())
                .reportingDate(r.getReportingDate())
                .createdAt(r.getCreatedAt())
                .feedback(r.getFeedback())
                .milestoneId(r.getMilestone() != null ? r.getMilestone().getId() : null)
                .milestoneTitle(r.getMilestone() != null ? r.getMilestone().getTitle() : null)
                .build();
    }
}