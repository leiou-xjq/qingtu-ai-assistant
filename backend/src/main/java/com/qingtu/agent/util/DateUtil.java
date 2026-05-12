package com.qingtu.agent.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 日期时间工具类
 * 
 * 功能说明：
 * - 日期时间格式化与解析
 * - 日期时间计算
 * - 常见日期时间操作封装
 * 
 * @author 青途智伴技术团队
 */
public class DateUtil {

    /**
     * 日期时间格式化器
     */
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 日期格式化器
     */
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * 时间格式化器
     */
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    /**
     * 时间戳格式化器
     */
    public static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 格式化日期时间
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DATETIME_FORMATTER);
    }

    /**
     * 格式化日期
     */
    public static String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DATE_FORMATTER);
    }

    /**
     * 格式化时间
     */
    public static String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(TIME_FORMATTER);
    }

    /**
     * 格式化时间戳
     */
    public static String formatTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(TIMESTAMP_FORMATTER);
    }

    /**
     * 解析日期时间
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER);
    }

    /**
     * 解析日期
     */
    public static LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateStr + " 00:00:00", DATETIME_FORMATTER);
    }

    /**
     * 计算两个日期时间之间的天数
     */
    public static long daysBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 计算两个日期时间之间的小时数
     */
    public static long hoursBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.HOURS.between(start, end);
    }

    /**
     * 计算两个日期时间之间的分钟数
     */
    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.MINUTES.between(start, end);
    }

    /**
     * 获取当前日期时间字符串
     */
    public static String nowDateTime() {
        return formatDateTime(LocalDateTime.now());
    }

    /**
     * 获取当前日期字符串
     */
    public static String nowDate() {
        return formatDate(LocalDateTime.now());
    }

    /**
     * 获取当前时间戳字符串
     */
    public static String nowTimestamp() {
        return formatTimestamp(LocalDateTime.now());
    }

    /**
     * 判断是否在指定时间之后
     */
    public static boolean isAfter(LocalDateTime dateTime, LocalDateTime target) {
        return dateTime != null && dateTime.isAfter(target);
    }

    /**
     * 判断是否在指定时间之前
     */
    public static boolean isBefore(LocalDateTime dateTime, LocalDateTime target) {
        return dateTime != null && dateTime.isBefore(target);
    }

    /**
     * 获取友好时间显示（如：刚刚、5分钟前、1小时前等）
     */
    public static String getFriendlyTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "未知";
        }
        LocalDateTime now = LocalDateTime.now();
        long minutes = minutesBetween(dateTime, now);
        
        if (minutes < 1) {
            return "刚刚";
        } else if (minutes < 60) {
            return minutes + "分钟前";
        } else if (minutes < 1440) {
            return (minutes / 60) + "小时前";
        } else if (minutes < 10080) {
            return (minutes / 1440) + "天前";
        } else {
            return formatDate(dateTime);
        }
    }
}