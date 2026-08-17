package com.antique.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 问答 — 请求体
 *
 * <p>接口：POST /api/ai/chat（需认证）
 *
 * <h3>校验规则</h3>
 * <ul>
 *   <li>question 不能为空，1 - 500 字符</li>
 * </ul>
 */
@Data
public class AiChatDTO {

    /** 用户问题，必填，1 - 500 字符 */
    @NotBlank(message = "问题不能为空")
    @Size(max = 500, message = "问题不能超过500字符")
    private String question;
}
