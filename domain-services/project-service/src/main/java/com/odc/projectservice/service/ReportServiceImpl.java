package com.odc.projectservice.service;

import com.odc.common.constant.*;
import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.common.exception.BusinessException;
import com.odc.common.exception.ResourceNotFoundException;
import com.odc.common.util.EnumUtil;
import com.odc.commonlib.event.EventPublisher;
import com.odc.companyservice.v1.*;
import com.odc.notification.v1.Channel;
import com.odc.notification.v1.NotificationEvent;
import com.odc.notification.v1.Target;
import com.odc.notification.v1.UserTarget;
import com.odc.projectservice.dto.request.CreateReportLabAdminRequest;
import com.odc.projectservice.dto.request.CreateReportRequest;
import com.odc.projectservice.dto.request.UpdateReportRequest;
import com.odc.projectservice.dto.request.UpdateReportStatusRequest;
import com.odc.projectservice.dto.response.GetReportToLabAdminResponse;
import com.odc.projectservice.dto.response.ReportResponse;
import com.odc.projectservice.dto.response.UserParticipantResponse;
import com.odc.projectservice.entity.*;
import com.odc.projectservice.repository.*;
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
    private final MilestoneMemberRepository milestoneMemberRepository;
    @Qualifier("userServiceChannel1")
    private final ManagedChannel userServiceChannel;
    @Qualifier("companyServiceChannel")
    private final ManagedChannel companyServiceChannel;
    private final EventPublisher eventPublisher;

    public ReportServiceImpl(ReportRepository reportRepository,
                             ProjectRepository projectRepository,
                             ProjectMemberRepository projectMemberRepository,
                             ProjectMilestoneRepository projectMilestoneRepository,
                             MilestoneMemberRepository milestoneMemberRepository,
                             ManagedChannel userServiceChannel,
                             ManagedChannel companyServiceChannel,
                             EventPublisher eventPublisher) {
        this.reportRepository = reportRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectMilestoneRepository = projectMilestoneRepository;
        this.milestoneMemberRepository = milestoneMemberRepository;
        this.userServiceChannel = userServiceChannel;
        this.companyServiceChannel = companyServiceChannel;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public ApiResponse<ReportResponse> createReport(UUID userId, CreateReportRequest request) {
        // 1. Validate Project & Status
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new BusinessException("Dự án không tồn tại"));

        // Kiểm tra trạng thái dự án có cho phép tạo report không
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
            // Các loại report khác (Weekly, Milestone...) thường cần file đính kèm
            if (request.getAttachmentsUrl() == null || request.getAttachmentsUrl().isEmpty()) {
                throw new BusinessException("Báo cáo này yêu cầu đính kèm file");
            }
        }

        // 3. Xử lý Logic Milestone
        ProjectMilestone milestone = null;

        // Nếu milestoneId != null -> Report cho Milestone cụ thể
        if (request.getMilestoneId() != null) {
            milestone = projectMilestoneRepository.findById(request.getMilestoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Milestone không tồn tại"));

            // Kiểm tra milestone có thuộc dự án này không
            if (!milestone.getProject().getId().equals(project.getId())) {
                throw new BusinessException("Milestone không thuộc về dự án này");
            }

            // Kiểm tra trạng thái Milestone phải là ON_GOING
            if (!ProjectMilestoneStatus.ON_GOING.toString().equalsIgnoreCase(milestone.getStatus())) {
                throw new BusinessException("Chỉ có thể báo cáo cho Milestone đang diễn ra (ON_GOING). Trạng thái hiện tại: " + milestone.getStatus());
            }

            milestone.setStatus(ProjectMilestoneStatus.PENDING_COMPLETED.toString());
            projectMilestoneRepository.save(milestone);
        }
        // Nếu milestoneId == null -> Report cho cả dự án (Logic giữ nguyên, không cần xử lý milestone)

        // 4. Validate Recipient (Người nhận)
        if (request.getRecipientId() == null) {
            throw new BusinessException("Người nhận báo cáo không được để trống");
        }

        // Kiểm tra người nhận có phải là thành viên dự án không
        // Sử dụng existsByUserIdAndProjectId để tối ưu thay vì lấy list về lọc
        boolean isRecipientInProject = projectMemberRepository.existsByUserIdAndProjectId(request.getRecipientId(), project.getId());

        // Nếu không phải thành viên dự án, kiểm tra xem có phải là Client (Company Owner) không
        if (!isRecipientInProject) {
            boolean isCompanyOwner = false;
            try {
                // Logic kiểm tra company owner qua gRPC
                CompanyServiceGrpc.CompanyServiceBlockingStub companyStub =
                        CompanyServiceGrpc.newBlockingStub(companyServiceChannel);

                GetCompanyByIdRequest companyRequest = GetCompanyByIdRequest.newBuilder()
                        .setCompanyId(project.getCompanyId().toString())
                        .build();

                GetCompanyByIdResponse companyResponse = companyStub.getCompanyById(companyRequest);

                // So sánh recipientId với userId của chủ công ty
                if (companyResponse.getUserId() != null &&
                        companyResponse.getUserId().equalsIgnoreCase(request.getRecipientId().toString())) {
                    isCompanyOwner = true;
                }
            } catch (Exception e) {
                log.error("Lỗi khi kiểm tra Company Owner qua gRPC: {}", e.getMessage());
            }

            if (!isCompanyOwner) {
                throw new BusinessException("Người nhận không phải là thành viên của dự án hoặc chủ sở hữu công ty");
            }
        }

        if (request.getRecipientId().equals(userId)) {
            throw new BusinessException("Bạn không thể tự gửi báo cáo cho chính mình.");
        }

        Report report = Report.builder()
                .project(project)
                .reporterId(userId)
                .recipientId(request.getRecipientId())
                .reportType(request.getReportType())
                .content(request.getContent())
                .attachmentsUrl(request.getAttachmentsUrl())
                .reportingDate(LocalDate.now())
                .status(ReportStatus.SUBMITTED.toString())
                .milestone(milestone)
                .build();

        Report savedReport = reportRepository.save(report);

        Map<String, UserInfo> userMap = fetchUserInfoBatch(List.of(userId));

        return ApiResponse.success("Tạo báo cáo thành công", mapToResponse(savedReport, userMap));
    }

    @Override
    @Transactional
    public ApiResponse<ReportResponse> updateReport(UUID userId, UUID reportId, UpdateReportRequest request) {
        // 1. Tìm báo cáo
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Báo cáo không tồn tại"));

        // 2. Validate quyền sở hữu (Người sửa phải là người tạo)
        if (!report.getReporterId().equals(userId)) {
            throw new BusinessException("Không có quyền sửa báo cáo này");
        }

        // 3. Validate trạng thái Report
        // Lưu ý: Nếu luồng nghiệp vụ là [Bị từ chối -> Sửa lại], bạn có thể cần cho phép sửa cả khi status là REJECTED
        // Hiện tại code giữ nguyên logic chỉ cho sửa khi SUBMITTED như yêu cầu cũ
        if (!ReportStatus.PENDING_ADMIN_CHECK.toString().equals(report.getStatus()) &&
                !ReportStatus.REJECTED.toString().equals(report.getStatus())) {
            throw new BusinessException("Chỉ có thể chỉnh sửa báo cáo khi ở trạng thái SUBMITTED hoặc REJECTED");
        }

        if (request.getContent() != null) report.setContent(request.getContent());
        if (request.getAttachmentsUrl() != null) report.setAttachmentsUrl(request.getAttachmentsUrl());

        Report updatedReport = reportRepository.save(report);

        ProjectMilestone milestone = report.getMilestone();
        if (milestone != null) {
            milestone.setStatus(ProjectMilestoneStatus.PENDING_COMPLETED.toString());
            projectMilestoneRepository.save(milestone);

            log.info("Updated Milestone {} status to PENDING_COMPLETE after report update", milestone.getId());
        }

        // 6. Map response
        Map<String, UserInfo> userMap = fetchUserInfoBatch(List.of(userId));

        return ApiResponse.success("Cập nhật báo cáo thành công", mapToResponse(updatedReport, userMap));
    }

    @Override
    @Transactional // Đảm bảo tính toàn vẹn dữ liệu (Rollback nếu lỗi)
    public ApiResponse<Void> reviewReport(UUID userId, UUID reportId, UpdateReportStatusRequest request) {
        // 1. Tìm báo cáo
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Báo cáo không tồn tại"));

        // 2. Validate quyền (Chỉ người nhận mới được duyệt)
        // Lưu ý: recipientId trong DB là nullable, cần check null trước khi equals
        boolean isRecipient = report.getRecipientId() != null && report.getRecipientId().equals(userId);

        // Mở rộng: Nếu muốn Admin cũng duyệt được thì thêm check role ở đây
        if (!isRecipient) {
            throw new BusinessException("Bạn không có quyền duyệt báo cáo này");
        }

        // 3. Validate Status hợp lệ (APPROVED / REJECTED)
        // Helper EnumUtil đã có trong project common
        if (!EnumUtil.isEnumValueExist(request.getStatus(), ReportStatus.class)) {
            throw new BusinessException("Trạng thái duyệt không hợp lệ: " + request.getStatus());
        }

        // 4. Cập nhật Report
        report.setStatus(request.getStatus());
        report.setFeedback(request.getFeedback());
        // report.setReviewedAt(LocalDateTime.now()); // Nên thêm field này vào Entity để tracking
        reportRepository.save(report);

        // 5. Cập nhật trạng thái Milestone (Side effect)
        // Chỉ thực hiện nếu báo cáo này gắn với một Milestone
        ProjectMilestone milestone = report.getMilestone();
        if (milestone != null) {
            updateMilestoneStatus(milestone, request.getStatus());
        }

        // 6. (Optional) Gửi thông báo lại cho người tạo báo cáo (Mentor)
        // notifyReporter(report);

        return ApiResponse.success("Đã cập nhật trạng thái báo cáo", null);
    }

    @Override
    public ApiResponse<Void> reviewReportByLabAdmin(UUID userId, UUID reportId, UpdateReportStatusRequest request) {
        // 1. Tìm báo cáo
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Báo cáo không tồn tại"));

        if (!report.getStatus().equals(ReportStatus.PENDING_ADMIN_CHECK.toString())) {
            throw new BusinessException("Báo cáo đã được duyệt.");
        }

        // 3. Validate Status hợp lệ (APPROVED / REJECTED)
        // Helper EnumUtil đã có trong project common
        if (!EnumUtil.isEnumValueExist(request.getStatus(), ReportStatus.class)) {
            throw new BusinessException("Trạng thái duyệt không hợp lệ: " + request.getStatus());
        }

        // 4. Cập nhật Report
        report.setStatus(request.getStatus());
        report.setFeedback(request.getFeedback());
        reportRepository.save(report);

        UUID createdBy = UUID.fromString(report.getCreatedBy());

        boolean isApproved = ReportStatus.APPROVED.toString().equals(report.getStatus());

        if (!isApproved) {
            ProjectMilestone projectMilestone = report.getMilestone();
            projectMilestone.setStatus(ProjectStatus.ON_GOING.toString());
            projectMilestoneRepository.save(projectMilestone);
        }

        NotificationEvent notificationEvent = NotificationEvent.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setType("REPORT_REVIEW")
                .setTitle(isApproved
                        ? "Báo cáo đã được duyệt"
                        : "Báo cáo bị từ chối")
                .setContent(isApproved
                        ? "Báo cáo của bạn đã được Lab Admin duyệt thành công."
                        : "Báo cáo của bạn đã bị từ chối. Vui lòng xem phản hồi và chỉnh sửa lại.")
                .putAllData(Map.of(
                        "reportId", report.getId().toString(),
                        "status", report.getStatus(),
                        "feedback", Optional.ofNullable(report.getFeedback()).orElse("")
                ))
                .setDeepLink("/milestones/" + report.getMilestone().getId().toString())
                .setPriority("HIGH")
                .setTarget(Target.newBuilder()
                        .setUser(UserTarget.newBuilder()
                                .addUserIds(createdBy.toString())
                                .build())
                        .build())
                .addAllChannels(List.of(Channel.WEB))
                .setCategory("REPORT")
                .setCreatedAt(System.currentTimeMillis())
                .build();

        eventPublisher.publish("notifications", notificationEvent);

        return ApiResponse.success("Đã cập nhật trạng thái báo cáo", null);
    }

    @Override
    public ApiResponse<PaginatedResult<GetReportToLabAdminResponse>> getReportToLabAdmin(
            Integer page, Integer pageSize) {

        Pageable pageable = PageRequest.of(
                page - 1,
                pageSize,
                Sort.by("reportingDate").descending()
        );

        Page<Report> reportPage =
                reportRepository.findByStatus(
                        ReportStatus.PENDING_ADMIN_CHECK.name(),
                        pageable
                );

        if (reportPage.isEmpty()) {
            return ApiResponse.success(PaginatedResult.from(Page.empty(pageable)));
        }

    /* =======================
       1. Collect companyIds
       ======================= */
        List<UUID> companyIds = reportPage.getContent().stream()
                .map(r -> r.getProject().getCompanyId())
                .distinct()
                .toList();

        Map<String, CompanyInfo> companyInfoMap =
                fetchCompanyInfoBatch(companyIds);

    /* =======================
       2. Collect userIds
       ======================= */
        Set<UUID> userIds = new HashSet<>();

        // reporter
        reportPage.getContent()
                .forEach(r -> userIds.add(r.getReporterId()));

        // user đại diện công ty
        companyInfoMap.values().forEach(c ->
                userIds.add(UUID.fromString(c.getUserId()))
        );

        Map<String, UserInfo> userInfoMap =
                fetchUserInfoBatch(new ArrayList<>(userIds));

    /* =======================
       3. Mapping response
       ======================= */
        List<GetReportToLabAdminResponse> responses =
                reportPage.getContent().stream()
                        .map(report -> mapToResponse(
                                report,
                                companyInfoMap,
                                userInfoMap
                        ))
                        .toList();

        PaginatedResult<GetReportToLabAdminResponse> result =
                PaginatedResult.<GetReportToLabAdminResponse>builder()
                        .data(responses)
                        .totalElements(reportPage.getTotalElements())
                        .totalPages(reportPage.getTotalPages())
                        .currentPage(reportPage.getNumber() + 1)
                        .hasNext(reportPage.hasNext())
                        .hasPrevious(reportPage.hasPrevious())
                        .build();

        return ApiResponse.success(result);
    }

    @Override
    public ApiResponse<Void> publishToCompany(UUID reportId, UUID userCompanyId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Báo cáo không tồn tại"));

        if (!ReportStatus.PENDING_ADMIN_CHECK.name().equals(report.getStatus())) {
            throw new BusinessException("Report phải được admin chấp nhận trước khi gửi cho công ty");
        }

        report.setRecipientId(userCompanyId);
        report.setStatus(ReportStatus.PENDING_COMPANY_REVIEW.toString());
        reportRepository.save(report);

        return ApiResponse.success("Gửi báo cáo tới công ty thành công", null);
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
    public ApiResponse<List<UserParticipantResponse>> getReportRecipients(UUID projectId, UUID milestoneId) {
        Project project;
        Map<UUID, String> memberRoles = new HashMap<>(); // Map để lưu UserId -> Role trong dự án

        // 1. Xác định danh sách thành viên dựa trên input (Milestone hoặc Project)
        if (milestoneId != null) {
            // -- LẤY THEO MILESTONE --
            ProjectMilestone milestone = projectMilestoneRepository.findById(milestoneId)
                    .orElseThrow(() -> new ResourceNotFoundException("Milestone không tồn tại"));

            project = milestone.getProject(); // Lấy project từ milestone để dùng sau này

            // Lấy danh sách thành viên ĐANG ACTIVE trong milestone này
            List<MilestoneMember> milestoneMembers = milestoneMemberRepository.findByProjectMilestone_Id(milestoneId);

            milestoneMembers.stream()
                    .filter(MilestoneMember::isActive) // Chỉ lấy active
                    .forEach(mm -> memberRoles.put(
                            mm.getProjectMember().getUserId(),
                            mm.getProjectMember().getRoleInProject()
                    ));

        } else if (projectId != null) {
            // -- LẤY THEO PROJECT --
            project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Dự án không tồn tại"));

            // Lấy toàn bộ thành viên dự án
            List<ProjectMember> projectMembers = projectMemberRepository.findByProjectId(projectId);

            projectMembers.forEach(pm -> memberRoles.put(
                    pm.getUserId(),
                    pm.getRoleInProject()
            ));

        } else {
            throw new BusinessException("Vui lòng cung cấp projectId hoặc milestoneId để lấy danh sách người nhận.");
        }

        // 2. Luôn lấy thêm thông tin Company Owner (Client)
        Set<UUID> allUserIdsToFetch = new HashSet<>(memberRoles.keySet());
        UUID companyOwnerId = null;

        try {
            CompanyServiceGrpc.CompanyServiceBlockingStub companyStub =
                    CompanyServiceGrpc.newBlockingStub(companyServiceChannel);

            // Gọi gRPC lấy thông tin công ty
            GetCompanyByIdRequest companyRequest = GetCompanyByIdRequest.newBuilder()
                    .setCompanyId(project.getCompanyId().toString())
                    .build();

            GetCompanyByIdResponse companyResponse = companyStub.getCompanyById(companyRequest);

            if (companyResponse.getUserId() != null && !companyResponse.getUserId().isEmpty()) {
                companyOwnerId = UUID.fromString(companyResponse.getUserId());
                allUserIdsToFetch.add(companyOwnerId); // Thêm vào danh sách cần fetch info
            }
        } catch (Exception e) {
            log.error("Lỗi khi lấy thông tin Company Owner từ Company Service: {}", e.getMessage());
        }

        // 3. Gọi User Service (gRPC) để lấy thông tin chi tiết (Tên, Avatar) cho tất cả ID
        // Hàm fetchUserInfoBatch đã được tối ưu ở phần trước để gọi gRPC 1 lần
        Map<String, UserInfo> userMap = fetchUserInfoBatch(new ArrayList<>(allUserIdsToFetch));

        // 4. Map dữ liệu sang Response DTO
        List<UserParticipantResponse> responses = new ArrayList<>();

        // 4.1 Thêm Company Owner vào danh sách (Ưu tiên hiển thị nếu có)
        if (companyOwnerId != null) {
            UserInfo info = userMap.get(companyOwnerId.toString());
            if (info != null) {
                responses.add(UserParticipantResponse.builder()
                        .id(companyOwnerId)
                        .name(info.getFullName() + " (Khách hàng)") // Thêm hậu tố để FE dễ hiển thị
                        .avatar(info.getAvatarUrl())
                        .roleName(Role.COMPANY.toString()) // Role đặc biệt để FE nhận biết đây là Client
                        .build());
            }
        }

        // 4.2 Thêm các thành viên khác
        for (Map.Entry<UUID, String> entry : memberRoles.entrySet()) {
            UUID uid = entry.getKey();
            String role = entry.getValue();

            // Nếu Company Owner cũng nằm trong danh sách thành viên thì bỏ qua để tránh trùng (đã add ở trên)
            if (uid.equals(companyOwnerId)) continue;

            UserInfo info = userMap.get(uid.toString());
            if (info != null) {
                responses.add(UserParticipantResponse.builder()
                        .id(uid)
                        .name(info.getFullName())
                        .avatar(info.getAvatarUrl())
                        .roleName(role) // MENTOR, TALENT, LEADER...
                        .build());
            }
        }

        return ApiResponse.success("Lấy danh sách người nhận báo cáo thành công", responses);
    }

    @Override
    public ApiResponse<Void> createReportToLabAdmin(UUID userId, CreateReportLabAdminRequest request) {
        // 1. Validate Project & Status
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new BusinessException("Dự án không tồn tại"));

        // Kiểm tra trạng thái dự án có cho phép tạo report không
        if (!ALLOWED_REPORT_STATUSES.contains(project.getStatus())) {
            throw new BusinessException(
                    "Không thể tạo báo cáo khi dự án đang ở trạng thái: " + project.getStatus()
            );
        }

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new BusinessException("Daily Report yêu cầu nội dung text");
        }

        // Các loại report khác (Weekly, Milestone...) thường cần file đính kèm
        if (request.getAttachmentsUrl() == null || request.getAttachmentsUrl().isEmpty()) {
            throw new BusinessException("Báo cáo này yêu cầu đính kèm file");
        }

        // 3. Xử lý Logic Milestone
        ProjectMilestone milestone = null;

        // Nếu milestoneId != null -> Report cho Milestone cụ thể
        if (request.getMilestoneId() != null) {
            milestone = projectMilestoneRepository.findById(request.getMilestoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Milestone không tồn tại"));

            // Kiểm tra milestone có thuộc dự án này không
            if (!milestone.getProject().getId().equals(project.getId())) {
                throw new BusinessException("Milestone không thuộc về dự án này");
            }

            // Kiểm tra trạng thái Milestone phải là ON_GOING
            if (!ProjectMilestoneStatus.ON_GOING.toString().equalsIgnoreCase(milestone.getStatus())) {
                throw new BusinessException("Chỉ có thể báo cáo cho Milestone đang diễn ra (ON_GOING). Trạng thái hiện tại: " + milestone.getStatus());
            }

            milestone.setStatus(ProjectMilestoneStatus.PENDING_COMPLETED.toString());
            projectMilestoneRepository.save(milestone);
        }

        Report report = Report.builder()
                .project(project)
                .reporterId(userId)
                .reportType(request.getReportType())
                .content(request.getContent())
                .attachmentsUrl(request.getAttachmentsUrl())
                .reportingDate(LocalDate.now())
                .status(ReportStatus.PENDING_ADMIN_CHECK.toString())
                .milestone(milestone)
                .build();

        reportRepository.save(report);
        return ApiResponse.success("Tạo báo cáo thành công", null);
    }

    @Override
    public ApiResponse<ReportResponse> getReportDetail(UUID reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Báo cáo không tồn tại"));

        Map<String, UserInfo> userMap = fetchUserInfoBatch(List.of(report.getReporterId()));
        return ApiResponse.success(mapToResponse(report, userMap));
    }

    private void updateMilestoneStatus(ProjectMilestone milestone, String reportStatus) {
        if (ReportStatus.COMPANY_APPROVED.toString().equalsIgnoreCase(reportStatus)) {
            // Nếu Client duyệt -> Milestone hoàn thành
            milestone.setStatus(ProjectMilestoneStatus.COMPLETED.toString());
        } else if (ReportStatus.COMPANY_REJECTED.toString().equalsIgnoreCase(reportStatus) ||
                ReportStatus.ADMIN_REJECTED.toString().equalsIgnoreCase(reportStatus)) {
            // Nếu Client/Lab Admin từ chối -> Yêu cầu Mentor tạo lại -> Trạng thái ON_GOING
            milestone.setStatus(ProjectMilestoneStatus.ON_GOING.toString());
        }
        projectMilestoneRepository.save(milestone);
    }

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

    private Map<String, CompanyInfo> fetchCompanyInfoBatch(List<UUID> companyIds) {
        if (companyIds.isEmpty()) return Map.of();
        try {
            List<String> stringIds = companyIds.stream().map(UUID::toString).toList();
            var stub = CompanyServiceGrpc.newBlockingStub(companyServiceChannel);

            GetInfoCompanyByCompanyIdsResponse response = stub.getCompaniesInfoByIds(
                    GetInfoCompanyByCompanyIdsRequest.newBuilder().addAllIds(stringIds).build()
            );

            return response.getDataMap();
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

    private GetReportToLabAdminResponse mapToResponse(
            Report report,
            Map<String, CompanyInfo> companyInfoMap,
            Map<String, UserInfo> userInfoMap) {

        UserInfo reporter =
                userInfoMap.get(report.getReporterId().toString());

        CompanyInfo company =
                companyInfoMap.get(report.getProject().getCompanyId().toString());

        UserInfo companyUser =
                userInfoMap.get(company.getUserId());

        return GetReportToLabAdminResponse.builder()
                .id(report.getId())
                .projectId(report.getProject().getId())
                .projectName(report.getProject().getTitle())

                .reporterId(report.getReporterId())
                .reporterName(reporter != null ? reporter.getFullName() : null)
                .reporterEmail(reporter != null ? reporter.getEmail() : null)
                .reporterAvatar(reporter != null ? reporter.getAvatarUrl() : null)

                .reportType(report.getReportType())
                .status(report.getStatus())

                .content(report.getContent())
                .attachmentsUrl(report.getAttachmentsUrl())

                .reportingDate(report.getReportingDate())
                .createdAt(report.getCreatedAt())
                .feedback(report.getFeedback())

                .milestoneId(report.getMilestone() != null ? report.getMilestone().getId() : null)
                .milestoneTitle(report.getMilestone() != null ? report.getMilestone().getTitle() : null)

                .companyId(report.getProject().getCompanyId())
                .companyName(company.getCompanyName())
                .companyLogo(company.getCompanyLogo())
                .companyEmail(company.getCompanyEmail())

                .userCompanyId(company.getUserId())
                .userCompanyEmail(companyUser.getEmail())
                .userCompanyAvatar(companyUser.getAvatarUrl())

                .build();
    }

}