package com.odc.projectservice.event;

import com.odc.common.constant.ProjectMilestoneStatus;
import com.odc.commonlib.event.EventHandler;
import com.odc.commonlib.util.ProtobufConverter;
import com.odc.payment.v1.PaymentSuccessEvent;
import com.odc.projectservice.entity.ProjectMilestone;
import com.odc.projectservice.repository.ProjectMilestoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentSuccessHandler implements EventHandler {

    private final ProjectMilestoneRepository milestoneRepository;

    @Override
    public String getTopic() {
        return "payment.success"; // Topic này phải khớp với bên Payment Service gửi đi
    }

    @Override
    @Transactional
    public void handle(byte[] eventPayload) {
        try {
            // 1. Deserialize dữ liệu từ byte[] sang Object
            PaymentSuccessEvent event = ProtobufConverter.deserialize(eventPayload, PaymentSuccessEvent.parser());

            log.info("Received PaymentSuccessEvent for milestone: {}, OrderCode: {}",
                    event.getMilestoneId(), event.getOrderCode());

            UUID milestoneId = UUID.fromString(event.getMilestoneId());

            // 2. Tìm Milestone trong DB
            ProjectMilestone milestone = milestoneRepository.findById(milestoneId)
                    .orElseThrow(() -> new RuntimeException("Milestone not found with id: " + milestoneId));

            // 3. Kiểm tra trạng thái hiện tại (để tránh xử lý lại nếu đã PAID rồi)
            if (ProjectMilestoneStatus.PAID.toString().equals(milestone.getStatus())) {
                log.info("Milestone {} is already PAID. Skipping.", milestoneId);
                return;
            }

            // 4. Cập nhật trạng thái sang PAID
            milestone.setStatus(ProjectMilestoneStatus.PAID.toString());

            // (Optional) Bạn có thể thêm logic log lại lịch sử thay đổi trạng thái nếu cần

            milestoneRepository.save(milestone);

            log.info("Successfully updated milestone {} status to PAID", milestoneId);

            // 5. (Optional) Gửi thông báo cho Mentor/Admin biết tiền đã về
            // notificationService.send(...)

        } catch (Exception e) {
            log.error("Error handling PaymentSuccessEvent", e);
            // Lưu ý: Nếu ném Exception ở đây, Kafka Consumer sẽ retry lại message này.
            // Nếu lỗi do dữ liệu sai (VD: ID không tồn tại) thì không nên ném Exception để tránh vòng lặp vô tận.
        }
    }
}