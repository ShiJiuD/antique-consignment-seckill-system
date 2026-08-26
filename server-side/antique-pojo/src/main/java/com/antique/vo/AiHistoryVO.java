package com.antique.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 对话历史 — 返回体
 *
 * <p>接口：GET /api/ai/history 的 data 字段
 * <p>消息按时间正序排列（最早的在前），最多 20 条（Python 服务侧截取）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiHistoryVO {

    /** 消息列表：{role: "user"/"assistant", content: "..."} */
    private List<AiMessageVO> list;
}
