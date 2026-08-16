package com.antique.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 标记已读请求体
 *
 * <p>前端调 POST /api/message/read 时，请求体 JSON 的字段自动填进这个类：
 * <pre>{ "messageId": 10001 }</pre>
 */
@Data
public class MessageReadDTO implements Serializable {

    // 校验注解：messageId 为空直接拒绝请求（由全局异常处理器统一返回 {code:0}），不用手写 if 判断
    @NotNull(message = "消息ID不能为空")
    private Long messageId;
}
