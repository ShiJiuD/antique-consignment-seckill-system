package com.antique.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时间格式化工具 — 统一的响应时间格式
 *
 * <p>所有接口响应中的时间字段统一为 {@code yyyy-MM-dd HH:mm:ss}。
 */
public final class TimeUtil {

    /** 响应时间格式：yyyy-MM-dd HH:mm:ss */
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TimeUtil() {
    }

    /**
     * 时间格式化（null 安全）：null 输入返回 null，避免调用方重复判空
     */
    public static String format(LocalDateTime time) {
        return time == null ? null : time.format(FORMATTER);
    }
}
