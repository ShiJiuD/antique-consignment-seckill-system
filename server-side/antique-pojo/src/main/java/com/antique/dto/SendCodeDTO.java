package com.antique.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 发送短信验证码 — 请求体
 *
 * <p>接口：POST /api/auth/sms/send
 *
 * <h3>校验规则</h3>
 * <ul>
 *   <li>phone 不能为空</li>
 *   <li>phone 必须匹配正则 ^1[3-9]\d{9}$（中国大陆手机号）</li>
 * </ul>
 */
@Data
public class SendCodeDTO {

    /** 手机号，必填，格式 ^1[3-9]\d{9}$ */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
