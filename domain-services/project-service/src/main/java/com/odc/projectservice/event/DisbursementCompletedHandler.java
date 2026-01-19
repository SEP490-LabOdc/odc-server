package com.odc.projectservice.event;

import com.odc.common.constant.ProjectMilestoneStatus;
import com.odc.commonlib.event.EventHandler;
import com.odc.commonlib.util.ProtobufConverter;
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
public class DisbursementCompletedHandler implements EventHandler {

    private final ProjectMilestoneRepository milestoneRepository;

    @Override
    public String getTopic() {
        return "payment.disbursement.completed";
    }

    @Override
    @Transactional
    public void handle(byte[] eventPayload) {
        try {
            com.odc.paymentservice.v1.DisbursementCompletedEvent event =
                    ProtobufConverter.deserialize(eventPayload, com.odc.paymentservice.v1.DisbursementCompletedEvent.parser());

            log.info("Received DisbursementCompletedEvent for milestone: {}, disbursementId: {}",
                    event.getMilestoneId(), event.getDisbursementId());

            UUID milestoneId = UUID.fromString(event.getMilestoneId());

            ProjectMilestone milestone = milestoneRepository.findById(milestoneId)
                    .orElseThrow(() ->
                            new RuntimeException("Milestone not found with id: " + milestoneId));

            // Idempotent check
            if (ProjectMilestoneStatus.DISTRIBUTED.toString().equals(milestone.getStatus())) {
                log.info("Milestone {} already DISTRIBUTED. Skipping.", milestoneId);
                return;
            }

            if (!ProjectMilestoneStatus.PAID.toString().equals(milestone.getStatus())) {
                log.warn("Milestone {} status is {}, expected PAID before DISTRIBUTED",
                        milestoneId, milestone.getStatus());
            }

            milestone.setStatus(ProjectMilestoneStatus.DISTRIBUTED.toString());
            milestoneRepository.save(milestone);

            log.info("Milestone {} status updated to DISTRIBUTED", milestoneId);

        } catch (Exception e) {
            log.error("Error handling DisbursementCompletedEvent", e);
        }
    }
}
