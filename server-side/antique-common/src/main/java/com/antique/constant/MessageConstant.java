package com.antique.constant;

/**
 * 消息常量 — 统一管理所有接口返回的提示信息
 *
 * <p>所有 Controller 中使用的硬编码字符串均从此处引用，
 * 方便后续统一修改或多语言扩展。
 */
public class MessageConstant {

    /** 通用操作成功 */
    public static final String OP_SUCCESS = "操作成功";

    /** 系统内部异常（兜底） */
    public static final String SYSTEM_ERROR = "系统异常";

    // ==================== 认证相关 ====================

    /** 手机号格式校验失败 */
    public static final String PHONE_INVALID = "手机号格式不正确";

    /** 验证码发送成功 */
    public static final String CODE_SEND_SUCCESS = "验证码发送成功";

    /** 验证码错误或已过期 */
    public static final String CODE_ERROR_OR_EXPIRED = "验证码错误或已过期";

    /** 登录成功 */
    public static final String LOGIN_SUCCESS = "登录成功";

    /** 账号或密码错误（统一提示，不区分用户不存在/密码错误，防撞库） */
    public static final String ACCOUNT_OR_PASSWORD_ERROR = "账号或密码错误";

    /** 手机号未注册（仅密码登录时：用户存在但未设置密码） */
    public static final String PHONE_NOT_REGISTERED = "手机号未注册";

    /** 账号已被禁用 */
    public static final String ACCOUNT_DISABLED = "账号已被禁用";

    /** 未登录或 Token 已过期 */
    public static final String NOT_LOGIN = "未登录，请先登录";

    // ==================== 用户操作 ====================

    /** 退出登录成功 */
    public static final String LOGOUT_SUCCESS = "退出成功";

    /** 个人信息修改成功 */
    public static final String PROFILE_UPDATE_SUCCESS = "修改成功";
}
