package com.antique.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 问答 — 返回体
 *
 * <p>接口：POST /api/ai/chat 的 data 字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatVO {

    /** AI 生成的回答内容 */
    private String answer;
}
