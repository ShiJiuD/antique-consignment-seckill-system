package com.antique.exception;

/**
 * 业务异常基类（抽象）
 *
 * <p>所有业务异常均继承此类，由 {@code GlobalExceptionHandler}
 * 统一捕获并转换为 {@code Result.error(msg)}。
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * throw new AuthException("验证码错误或已过期");
 * }</pre>
 *
 * <h3>与 RuntimeException 的关系</h3>
 * <p>继承 RuntimeException 使得业务异常无需显式 try-catch，
 * 自动向上抛出直至被 GlobalExceptionHandler 拦截。
 */
public abstract class BaseException extends RuntimeException {

    public BaseException() {
    }

    public BaseException(String msg) {
        super(msg);
    }
}
