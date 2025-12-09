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

    // 1. Khởi tạo ThreadPoolTaskScheduler riêng
    public DynamicFeeDistributionScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("fee-distribution-scheduler-");
        scheduler.initialize();
        this.taskScheduler = scheduler;
    }

    @PostConstruct
    public void startScheduler() {
        rescheduleTask();
    }

    /**
     * Dừng tác vụ hiện tại, đọc cấu hình mới từ DB, và lên lịch lại tác vụ.
     * Phương thức này được gọi khi Admin cập nhật config.
     */
    public void rescheduleTask() {
        // 1. Dừng tác vụ hiện tại nếu đang chạy
        if (this.scheduledTask != null && !this.scheduledTask.isCancelled()) {
            this.scheduledTask.cancel(true);
            log.info("Old Fee Distribution task cancelled.");
        }

        // 2. Đọc cấu hình Cron Expression từ DB bằng NAME="fee-distribution"
        SystemConfig config = systemConfigRepository.findByName(
                        PaymentConstant.SYSTEM_CONFIG_FEE_DISTRIBUTION_NAME)
                .orElseThrow(() -> new BusinessException("Không tìm thấy cấu hình: " + PaymentConstant.SYSTEM_CONFIG_FEE_DISTRIBUTION_NAME));
        ;

        String cronExpression = null;
        if (config.getProperties() != null) {
            cronExpression = (String) config.getProperties().get(PaymentConstant.SYSTEM_CONFIG_CRON_EXPRESSION_KEY);
        }

        if (cronExpression == null || cronExpression.isBlank()) {
            // Cung cấp Cron Expression mặc định nếu không tìm thấy trong DB
            cronExpression = "0 0 3 1 * ?"; // Mặc định: 3:00 AM ngày 1 hàng tháng
            log.warn("Cron expression not found in properties. Using default: {}", cronExpression);
        }

        log.info("Configuring Fee Distribution with new cron expression: {}", cronExpression);

        // 3. Lên lịch tác vụ mới
        try {
            this.scheduledTask = taskScheduler.schedule(
                    this.feeDistributionService::processFeeDistribution,
                    new CronTrigger(cronExpression)
            );
            log.info("New Fee Distribution task successfully scheduled.");
        } catch (Exception e) {
            log.error("Failed to schedule Fee Distribution task with cron expression: {}", cronExpression, e);
            // Có thể đặt lịch mặc định an toàn nếu cấu hình mới bị lỗi
        }
    }

    @Override
    public void destroy() {
        if (this.scheduledTask != null) {
            this.scheduledTask.cancel(true);
        }
        if (this.taskScheduler instanceof ThreadPoolTaskScheduler) {
            ((ThreadPoolTaskScheduler) this.taskScheduler).shutdown();
        }
        log.info("Fee Distribution Scheduler shut down.");
    }
}