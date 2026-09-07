package com.antique.exception;

/**
 * 认证异常 — 登录、Token 校验等场景的业务异常
 *
 * <p>使用示例：
 * <pre>{@code
 * // 验证码错误
 * throw new AuthException(MessageConstant.CODE_ERROR_OR_EXPIRED);
 *
 * // 账号被禁用
 * throw new AuthException(MessageConstant.ACCOUNT_DISABLED);
 * }</pre>
 */
public class AuthException extends BaseException {

    public AuthException() {
    }

    public AuthException(String msg) {
        super(msg);
    }
}
