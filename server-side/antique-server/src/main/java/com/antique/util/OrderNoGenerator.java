package com.antique.util;

import cn.hutool.core.util.RandomUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 订单号生成工具
 *
 * <p>格式：yyyyMMddHHmmss + 6 位随机数字（如 20260815204523403946），
 * 可读、按时间可回溯，配合 orders 表 uk_order_no 唯一索引兜底。
 */
public final class OrderNoGenerator {

    /** 订单号时间部分格式：yyyyMMddHHmmss */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 随机后缀长度 */
    private static final int RANDOM_LENGTH = 6;

    private OrderNoGenerator() {
    }

    /**
     * 生成订单号（20 位）
     */
    public static String generate() {
        return FORMATTER.format(LocalDateTime.now()) + RandomUtil.randomNumbers(RANDOM_LENGTH);
    }
}
