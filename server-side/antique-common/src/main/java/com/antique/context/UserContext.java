package com.antique.context;

/**
 * 用户上下文 — ThreadLocal 实现请求级用户信息传递
 *
 * <h3>工作流程</h3>
 * <ol>
 *   <li><b>设置</b>：{@code TokenInterceptor.preHandle()} 从 Redis 验证 Token 后，调用 {@code UserContext.set(userId, phone)}</li>
 *   <li><b>使用</b>：Controller / Service 中通过 {@code UserContext.getUserId()} 获取当前用户，无需从请求参数传递</li>
 *   <li><b>清理</b>：{@code TokenInterceptor.afterCompletion()} 中调用 {@code clear()} 防止内存泄漏</li>
 * </ol>
 *
 * <h3>设计说明</h3>
 * <p>使用 ThreadLocal 而非 RequestAttribute，因为：
 * <ul>
 *   <li>Service 层无法直接获取 HttpServletRequest</li>
 *   <li>ThreadLocal 对业务代码零侵入，不污染方法签名</li>
 * </ul>
 *
 * <h3>注意</h3>
 * <p>ThreadLocal 必须在使用后清理（clear），否则在 Tomcat 线程池复用场景下会造成数据串套。
 * 清理操作已在拦截器的 afterCompletion 中统一完成。
 */
public class UserContext {

    /** 当前请求的用户 ID */
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    /** 当前请求的用户手机号（脱敏后） */
    private static final ThreadLocal<String> USER_PHONE = new ThreadLocal<>();

    /**
     * 设置当前用户上下文
     *
     * @param userId 用户 ID
     * @param phone  用户手机号（脱敏格式）
     */
    public static void set(Long userId, String phone) {
        USER_ID.set(userId);
        USER_PHONE.set(phone);
    }

    /**
     * 获取当前用户 ID
     *
     * @return 用户 ID，未登录时返回 null
     */
    public static Long getUserId() {
        return USER_ID.get();
    }

    /**
     * 获取当前用户手机号（脱敏）
     *
     * @return 脱敏手机号，未登录时返回 null
     */
    public static String getUserPhone() {
        return USER_PHONE.get();
    }

    /**
     * 清理当前线程的上下文（防止内存泄漏）
     * <p>由拦截器 afterCompletion 自动调用
     */
    public static void clear() {
        USER_ID.remove();
        USER_PHONE.remove();
    }
}
