package com.odc.projectservice.event.handler;

import com.odc.common.constant.Role;
import com.odc.commonlib.event.EventHandler;
import com.odc.commonlib.event.EventPublisher;
import com.odc.commonlib.util.ProtobufConverter;
import com.odc.notification.v1.Channel;
import com.odc.notification.v1.NotificationEvent;
import com.odc.notification.v1.Target;
import com.odc.notification.v1.UserTarget;
import com.odc.projectservice.entity.Project;
import com.odc.projectservice.entity.ProjectApplication;
import com.odc.projectservice.repository.ProjectApplicationRepository;
import com.odc.projectservice.repository.ProjectMemberRepository;
import com.odc.projectservice.repository.ProjectRepository;
import com.odc.projectservice.service.AiMatchingService;
import com.odc.projectservice.v1.CvAnalysisRequiredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class CvAnalysisHandler implements EventHandler {

    private final ProjectApplicationRepository applicationRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AiMatchingService aiMatchingService;
    private final EventPublisher eventPublisher;

    @Override
    public String getTopic() {
        return "project.cv.analysis";
    }

    @Override
    @Transactional
    public void handle(byte[] eventPayload) {
        try {
            CvAnalysisRequiredEvent event = ProtobufConverter.deserialize(eventPayload, CvAnalysisRequiredEvent.parser());
            log.info("Đang AI Scan CV cho đơn ứng tuyển: {}", event.getProjectApplicationId());

            ProjectApplication app = applicationRepository.findById(UUID.fromString(event.getProjectApplicationId()))
                    .orElseThrow(() -> new RuntimeException("Application not found"));

            Project project = projectRepository.findById(UUID.fromString(event.getProjectId()))
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            Map<String, Object> aiResult = aiMatchingService.analyzeCv(event.getCvUrl(), project.getDescription());

            app.setAiScanResult(aiResult);
            applicationRepository.save(app);

            List<String> mentorIds = projectMemberRepository.findByProjectIdAndRole(project.getId(), Role.MENTOR.toString())
                    .stream().map(m -> m.getUserId().toString()).toList();

            if (!mentorIds.isEmpty()) {
                boolean isCv = (boolean) aiResult.getOrDefault("is_cv", false);

                String notificationTitle;
                String notificationContent;

                if (isCv) {
                    int score = (int) aiResult.getOrDefault("match_score", 0);
                    notificationTitle = "AI Đã Chấm Điểm Hồ Sơ Mới";
                    notificationContent = String.format("Ứng viên cho dự án '%s' đạt %d/100 điểm phù hợp.", project.getTitle(), score);
                } else {
                    String reason = (String) aiResult.getOrDefault("reason", "Tệp tin không hợp lệ");
                    notificationTitle = "Cảnh Báo Hồ Sơ Không Hợp Lệ";
                    notificationContent = String.format("Ứng viên nộp hồ sơ cho dự án '%s' có vấn đề: %s", project.getTitle(), reason);
                }

                NotificationEvent noti = NotificationEvent.newBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setType(isCv ? "CV_ANALYSIS_SUCCESS" : "CV_ANALYSIS_INVALID")
                        .setTitle(notificationTitle)
                        .setContent(notificationContent)
                        .setDeepLink("/project/applications/" + app.getId())
                        .setTarget(Target.newBuilder()
                                .setUser(UserTarget.newBuilder().addAllUserIds(mentorIds).build())
                                .build())
                        .addChannels(Channel.WEB)
                        .setCreatedAt(System.currentTimeMillis())
                        .build();

                eventPublisher.publish("notifications", noti);
                log.info("Đã gửi thông báo AI scan cho Mentor: {}", notificationTitle);
            }

        } catch (Exception e) {
            log.error("Lỗi nghiêm trọng khi xử lý CvAnalysisEvent", e);
        }
    }
}