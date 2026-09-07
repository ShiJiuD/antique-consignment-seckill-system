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

    /** 系统繁忙（Redis 等基础设施不可用时） */
    public static final String SYSTEM_BUSY = "系统繁忙";

    // ==================== 通用参数校验 ====================

    /** 请求体格式错误（JSON 解析失败或字段类型不匹配） */
    public static final String PARAM_FORMAT_ERROR = "请求参数格式错误";

    /** 参数校验兜底提示（无具体字段错误信息时使用） */
    public static final String PARAM_ERROR = "参数错误";

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

    // ==================== 藏品相关 ====================

    /** 查询成功（列表/搜索/详情/收藏列表） */
    public static final String QUERY_SUCCESS = "查询成功";

    /** 藏品不存在 */
    public static final String ANTIQUE_NOT_EXIST = "藏品不存在";

    /** 藏品不存在或已下架 */
    public static final String ANTIQUE_NOT_ON_SALE = "藏品不存在或已下架";

    /** 搜索关键词不能为空 */
    public static final String SEARCH_KEYWORD_EMPTY = "搜索关键词不能为空";

    // ==================== 收藏相关 ====================

    /** 收藏成功 */
    public static final String FAVORITE_ADD_SUCCESS = "收藏成功";

    /** 取消收藏成功 */
    public static final String FAVORITE_REMOVE_SUCCESS = "取消收藏成功";

    /** 已收藏过该藏品（重复收藏） */
    public static final String FAVORITE_ALREADY = "已收藏过该藏品";

    /** 未收藏该藏品（取消收藏时） */
    public static final String FAVORITE_NOT_EXIST = "未收藏该藏品";
}
