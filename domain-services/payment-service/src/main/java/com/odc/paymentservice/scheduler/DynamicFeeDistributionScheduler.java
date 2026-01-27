package com.odc.paymentservice.scheduler;

import com.odc.common.constant.PaymentConstant;
import com.odc.paymentservice.repository.SystemConfigRepository;
import com.odc.paymentservice.service.FeeDistributionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.TimeZone;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class DynamicFeeDistributionScheduler implements DisposableBean {

    @Autowired
    private FeeDistributionService feeDistributionService;

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    private ScheduledFuture<?> scheduledTask;
    private final TaskScheduler taskScheduler;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${custom.config-cron-expression:0 35 21 26 1 ?}")
    private String cronExpression;

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

    /**
     * Reschedule cron job (KHÔNG chạy ngay)
     */
    public synchronized void rescheduleTask() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(true);
            log.info("[Scheduler] Old task cancelled");
        }

        try {
            systemConfigRepository
                    .findByName(PaymentConstant.SYSTEM_CONFIG_CRON_EXPRESSION_KEY)
                    .map(cfg -> cfg.getProperties().get("cronExpression"))
                    .map(Object::toString)
                    .ifPresentOrElse(
                            cron -> this.cronExpression = cron,
                            () -> log.warn("[Scheduler] Cron not found in DB, using default: {}", cronExpression)
                    );
        } catch (Exception e) {
            log.error("[Scheduler] Failed to load cron from DB, fallback to default", e);
        }

        if (!CronExpression.isValidExpression(cronExpression)) {
            log.error("[Scheduler] Invalid cron expression: {}", cronExpression);
            return;
        }

        log.info("[Scheduler] Scheduling Fee Distribution with cron={}", cronExpression);

        scheduledTask = taskScheduler.schedule(
                this::executeSafely,
                new CronTrigger(cronExpression, TimeZone.getTimeZone("Asia/Ho_Chi_Minh"))
        );
    }

    /**
     * Update cron + chạy NGAY LẬP TỨC
     */
    public synchronized void updateCronAndRunNow(String newCron) {

        if (!CronExpression.isValidExpression(newCron)) {
            throw new IllegalArgumentException("Invalid cron expression: " + newCron);
        }

        this.cronExpression = newCron;

        log.info("[Scheduler] Updating cron to {}", newCron);

        // schedule lại cron
        rescheduleTask();

        // chạy ngay 1 lần
        runOnceImmediately();
    }

    /**
     * Trigger chạy ngay (không ảnh hưởng cron)
     */
    public void runOnceImmediately() {
        log.info("[Scheduler] Manual immediate trigger");

        taskScheduler.schedule(
                this::executeSafely,
                Instant.now()
        );
    }

    /**
     * Chống chạy song song
     */
    private void executeSafely() {
        if (!running.compareAndSet(false, true)) {
            log.warn("[Scheduler] Job already running, skip");
            return;
        }

        try {
            feeDistributionService.processFeeDistribution();
        } catch (Exception e) {
            log.error("[Scheduler] Job execution failed", e);
        } finally {
            running.set(false);
        }
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
