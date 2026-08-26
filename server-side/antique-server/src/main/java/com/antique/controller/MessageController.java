package com.antique.controller;

import com.antique.constant.MessageConstant;
import com.antique.context.UserContext;
import com.antique.dto.MessageReadDTO;
import com.antique.result.Result;
import com.antique.service.MessageService;
import com.antique.vo.MessageVO;
import com.antique.vo.PageResultVO;
import com.antique.vo.UnreadCountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息控制器 — 消息列表 / 未读数 / 标记已读
 *
 * <p>Controller 是"接口门卫"：只负责 收请求 → 取当前用户 → 转给 service → 包成统一格式返回，
 * 不写业务逻辑（业务都在 MessageServiceImpl 里）。
 *
 * <p>路径前缀 /api/message，全部需认证：Token 由 TokenInterceptor 校验，
 * 当前用户 ID 从 UserContext.getUserId() 拿（登录时放进去的，无法伪造）。
 */
@Slf4j
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
@Tag(name = "消息模块", description = "消息列表/未读数/标记已读，下单自动产生消息")
public class MessageController {

    private final MessageService messageService;

    /**
     * 接口 1：消息列表（分页，创建时间倒序）
     * GET /api/message/list?page=1&size=10
     */
    @Operation(summary = "消息列表")
    @GetMapping("/list")
    public Result<PageResultVO<MessageVO>> list(
            @RequestParam(defaultValue = "1") Integer page,   // URL 参数，没传默认第 1 页
            @RequestParam(defaultValue = "10") Integer size) {  // URL 参数，没传默认每页 10 条
        Long userId = UserContext.getUserId();  // 当前登录用户 ID
        log.info("消息列表: userId={}, page={}, size={}", userId, page, size);
        PageResultVO<MessageVO> data = messageService.pageList(userId, page, size);
        return Result.success(data, MessageConstant.QUERY_SUCCESS);  // {code:1, msg:"查询成功", data:{...}}
    }

    /**
     * 接口 2：未读消息数（前端角标）
     * GET /api/message/unread-count
     */
    @Operation(summary = "未读消息数")
    @GetMapping("/unread-count")
    public Result<UnreadCountVO> unreadCount() {
        Long userId = UserContext.getUserId();
        log.info("未读消息数: userId={}", userId);
        UnreadCountVO data = messageService.unreadCount(userId);
        return Result.success(data, MessageConstant.QUERY_SUCCESS);
    }

    /**
     * 接口 3：标记已读（单条，幂等）
     * POST /api/message/read   body: {"messageId": 10001}
     */
    @Operation(summary = "标记已读")
    @PostMapping("/read")
    public Result<?> read(@RequestBody @Valid MessageReadDTO dto) {
        // @RequestBody：请求体 JSON 自动填进 dto；@Valid：先过 @NotNull 校验，不过直接返回错误
        Long userId = UserContext.getUserId();
        log.info("标记已读: userId={}, messageId={}", userId, dto.getMessageId());
        messageService.markRead(userId, dto.getMessageId());
        return Result.success(null, MessageConstant.MESSAGE_READ_SUCCESS);  // 无数据返回，data 为 null
    }
}
