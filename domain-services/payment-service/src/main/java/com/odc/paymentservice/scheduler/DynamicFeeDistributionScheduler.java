package com.odc.paymentservice.scheduler;

import com.odc.common.constant.PaymentConstant;
import com.odc.common.exception.BusinessException;
import com.odc.paymentservice.entity.SystemConfig;
import com.odc.paymentservice.repository.SystemConfigRepository;
import com.odc.paymentservice.service.FeeDistributionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ScheduledFuture;

/**
 * Quản lý Cron Job động cho nghiệp vụ Phân bổ Phí (Fee Distribution)
 * Có thể reset ngay lập tức khi cấu hình DB thay đổi.
 */
@Component
@Slf4j
public class DynamicFeeDistributionScheduler implements DisposableBean {

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private FeeDistributionService feeDistributionService;

    private ScheduledFuture<?> scheduledTask;
    private final TaskScheduler taskScheduler;

    public DynamicFeeDistributionScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("fee-distribution-scheduler-");
        scheduler.initialize();
        this.taskScheduler = scheduler;
    }

    @PostConstruct
    public void startScheduler() {
        log.info("[Scheduler] Initializing Fee Distribution Scheduler");
        rescheduleTask();
    }

    public synchronized void rescheduleTask() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(true);
            log.info("[Scheduler] Old task cancelled");
        }

        SystemConfig config = systemConfigRepository.findByName(
                PaymentConstant.SYSTEM_CONFIG_FEE_DISTRIBUTION_NAME
        ).orElseThrow(() ->
                new BusinessException("Missing system config: fee-distribution"));

        String cron = null;
        if (config.getProperties() != null) {
            cron = (String) config.getProperties()
                    .get(PaymentConstant.SYSTEM_CONFIG_CRON_EXPRESSION_KEY);
        }

        // Default: 16:00 ngày 19 hằng tháng
        if (cron == null || cron.isBlank()) {
            cron = "0 35 21 26 1 ?";
            log.warn("[Scheduler] Cron not found, using default: {}", cron);
        }

        log.info("[Scheduler] Scheduling Fee Distribution with cron={}", cron);

        scheduledTask = taskScheduler.schedule(
                () -> {
                    try {
                        feeDistributionService.processFeeDistribution();
                    } catch (Exception e) {
                        log.error("[Scheduler] Job execution failed", e);
                    }
                },
                new CronTrigger(cron)
        );
    }

    @Override
    public void destroy() {
        if (scheduledTask != null) {
            scheduledTask.cancel(true);
        }
        ((ThreadPoolTaskScheduler) taskScheduler).shutdown();
        log.info("[Scheduler] Shutdown completed");
    }
}