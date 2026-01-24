package com.odc.common.util;

import com.odc.common.exception.BusinessException;
import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {

    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT));
    }

    public static String formatDateTime(LocalDateTime dateTime, String pattern) {
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    public static LocalDateTime parseDateTime(String dateTimeStr) {
        return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT));
    }

    public static LocalDateTime parseDateTime(String dateTimeStr, String pattern) {
        return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(pattern));
    }

    public static LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now(DEFAULT_ZONE_ID);
    }

    public static ZonedDateTime getCurrentZonedDateTime() {
        return ZonedDateTime.now(DEFAULT_ZONE_ID);
    }

    public static LocalDate calculateNextScheduledDate(String cronExpression) {
        CronExpression cron = CronExpression.parse(cronExpression);

        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime next = cron.next(now);

        if (next == null) {
            throw new BusinessException("Invalid cron expression: " + cronExpression);
        }

        return next.toLocalDate();
    }
}
