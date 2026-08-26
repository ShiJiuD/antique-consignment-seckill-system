package com.antique.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 对话消息 — 单条消息（历史列表元素）
 *
 * <p>接口：GET /api/ai/history 的 list 元素
 * <p>由 Python 服务返回的历史记录转换而来。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiMessageVO {

    /** 消息角色：user=用户提问，assistant=AI 回答 */
    private String role;

    /** 消息内容 */
    private String content;
}
