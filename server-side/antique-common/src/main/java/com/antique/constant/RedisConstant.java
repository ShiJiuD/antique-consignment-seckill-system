package com.antique.constant;

/**
 * Redis 常量 — 定义所有 Redis Key 前缀和过期时间
 *
 * <h3>Key 设计规范</h3>
 * <ul>
 *   <li>Token Key: {@code antique:token:{uuid32}} → JSON{"userId":1,"phone":"138****8000"}</li>
 *   <li>验证码 Key: {@code sms:code:{phone}} → 6位数字验证码</li>
 * </ul>
 *
 * <h3>TTL 设计</h3>
 * <ul>
 *   <li>Token 7 天 — 每次请求自动续期，活跃用户无需频繁登录</li>
 *   <li>验证码 5 分钟 — 防暴力破解，过期需重新发送</li>
 * </ul>
 */
public class RedisConstant {

    // ==================== Key 前缀 ====================

    /** Token 存储 Key 前缀，完整 Key = antique:token:{token} */
    public static final String KEY_TOKEN = "antique:token:";

    /** 短信验证码 Key 前缀，完整 Key = sms:code:{phone} */
    public static final String KEY_SMS_CODE = "sms:code:";

    // ==================== 过期时间（秒） ====================

    /** Token 过期时间：7 天 = 60×60×24×7 */
    public static final long TOKEN_TTL = 604800L;

    /** 验证码过期时间：5 分钟 = 60×5 */
    public static final long CODE_TTL = 300L;
}
