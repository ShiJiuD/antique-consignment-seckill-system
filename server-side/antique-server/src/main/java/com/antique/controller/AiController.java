package com.antique.controller;

import com.antique.constant.MessageConstant;
import com.antique.context.UserContext;
import com.antique.dto.AiChatDTO;
import com.antique.result.Result;
import com.antique.service.AiService;
import com.antique.vo.AiChatVO;
import com.antique.vo.AiHistoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 助手控制器 — 智能知识库问答（需认证）
 *
 * <p>路径前缀: /api/ai（需认证，Token 校验由 TokenInterceptor 完成，
 * 未登录返回 401；当前用户 ID 从 {@code UserContext.getUserId()} 获取）
 *
 * <p>架构：前端 → 本控制器 → Python AI 服务（ai-side，内部 HTTP 调用）→ 通义千问
 *
 * <h3>接口列表</h3>
 * <table>
 *   <tr><th>接口</th><th>路径</th><th>认证</th></tr>
 *   <tr><td>AI 问答</td><td>POST /api/ai/chat</td><td>🔒</td></tr>
 *   <tr><td>获取对话历史</td><td>GET /api/ai/history</td><td>🔒</td></tr>
 * </table>
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI助手模块", description = "智能知识库问答")
public class AiController {

    private final AiService aiService;

    /**
     * 接口 1：AI 问答（知识库检索 + 通义千问生成，自动保存对话历史）
     *
     * @param dto 用户问题（1-500 字符，不能为空）
     */
    @Operation(summary = "AI问答")
    @PostMapping("/chat")
    public Result<AiChatVO> chat(@Valid @RequestBody AiChatDTO dto) {
        Long userId = UserContext.getUserId();
        log.info("AI问答请求: userId={}, question={}", userId, dto.getQuestion());
        String answer = aiService.chat(userId, dto.getQuestion());
        return Result.success(new AiChatVO(answer), MessageConstant.QUERY_SUCCESS);
    }

    /**
     * 接口 2：获取对话历史（最近 20 条，时间正序，进页面时加载）
     */
    @Operation(summary = "获取对话历史")
    @GetMapping("/history")
    public Result<AiHistoryVO> history() {
        Long userId = UserContext.getUserId();
        log.info("AI历史请求: userId={}", userId);
        AiHistoryVO data = new AiHistoryVO(aiService.history(userId));
        return Result.success(data, MessageConstant.QUERY_SUCCESS);
    }
}
