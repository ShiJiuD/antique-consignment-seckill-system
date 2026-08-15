package com.antique.util;

/**
 * 脱敏工具 — 敏感信息展示脱敏
 */
public final class MaskUtil {

    private MaskUtil() {
    }

    /**
     * 手机号脱敏：13800138000 → 138****8000
     *
     * <p>非 11 位手机号原样返回（不做长度假设，防御脏数据）。
     *
     * @param phone 原始手机号，可为 null
     * @return 脱敏后的手机号，null 输入返回 null
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
