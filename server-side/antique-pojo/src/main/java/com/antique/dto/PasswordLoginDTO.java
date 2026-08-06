package com.antique.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 密码登录 — 请求体
 *
 * <p>接口：POST /api/auth/login/password
 * <p>密码明文传输（TLS 加密），后端 BCrypt 校验。
 * <p>要求用户必须已注册且设置了密码。
 *
 * <h3>校验规则</h3>
 * <ul>
 *   <li>phone 不能为空，格式 ^1[3-9]\d{9}$</li>
 *   <li>password 不能为空</li>
 * </ul>
 */
@Data
public class PasswordLoginDTO {

    /** 手机号，必填 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 登录密码（明文），必填 */
    @NotBlank(message = "密码不能为空")
    private String password;
}
