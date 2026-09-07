package com.antique.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 短信验证码登录 — 请求体
 *
 * <p>接口：POST /api/auth/login/sms
 * <p>用户不存在时自动注册，无需单独调用注册接口。
 *
 * <h3>校验规则</h3>
 * <ul>
 *   <li>phone 不能为空，格式 ^1[3-9]\d{9}$</li>
 *   <li>code 不能为空（6 位数字验证码）</li>
 * </ul>
 */
@Data
public class SmsLoginDTO {

    /** 手机号，必填 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 6 位短信验证码，必填 */
    @NotBlank(message = "验证码不能为空")
    private String code;
}
