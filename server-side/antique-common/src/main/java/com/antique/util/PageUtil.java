package com.antique.util;

/**
 * 分页参数工具 — 页码/每页数量归一化
 *
 * <p>所有分页接口共用同一套默认值与上限（页面默认 1、每页默认 10、上限 50），
 * 避免各 Service 各自实现一份相同的归一化逻辑。
 */
public final class PageUtil {

    /** 默认页码 */
    public static final int DEFAULT_PAGE = 1;

    /** 默认每页数量 */
    public static final int DEFAULT_SIZE = 10;

    /** 每页数量上限（防止一次性拉取过多数据） */
    public static final int MAX_SIZE = 50;

    private PageUtil() {
    }

    /**
     * 页码归一化：null 或小于 1 时取默认值 1
     */
    public static int normalizePage(Integer page) {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    /**
     * 每页数量归一化：null 或小于 1 取默认值 10，超过上限 50 时截断
     */
    public static int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
