package com.antique.service;

import com.antique.vo.AiMessageVO;

import java.util.List;

/**
 * AI 助手服务接口
 *
 * <p>职责：调用 Python AI 服务（ai-side，FastAPI），
 * 将"前端请求 → Python 生成"的链路在后端完成转发与兜底。
 */
public interface AiService {

    /**
     * AI 问答：转发到 Python 服务生成回答
     *
     * @param userId   用户ID（来自 Token，Python 侧据此管理独立对话历史）
     * @param question 用户问题（1-500 字符）
     * @return AI 回答内容
     */
    String chat(Long userId, String question);

    /**
     * 获取用户最近的对话历史（按时间正序，最多 20 条）
     *
     * @param userId 用户ID
     * @return 消息列表 [{role, content}, ...]，无历史时返回空列表
     */
    List<AiMessageVO> history(Long userId);
}
