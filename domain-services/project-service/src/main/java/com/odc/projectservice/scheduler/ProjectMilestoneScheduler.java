package com.odc.projectservice.scheduler;

import com.odc.common.constant.ProjectMilestoneStatus;
import com.odc.projectservice.entity.ProjectMilestone;
import com.odc.projectservice.repository.ProjectMilestoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectMilestoneScheduler {

    private final ProjectMilestoneRepository milestoneRepository;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void updateMilestonesStatus() {
        LocalDate today = LocalDate.now();

        List<ProjectMilestone> milestones =
                milestoneRepository.findMilestonesToStart(today);

        milestones.forEach(m -> m.setStatus(ProjectMilestoneStatus.ON_GOING.toString()));

        milestoneRepository.saveAll(milestones);

        log.info("Updated {} milestones to ON_GOING", milestones.size());
    }
}

