package com.antique.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 登录响应 VO — 返回 Token + 用户信息
 *
 * <p>用于短信登录和密码登录两个接口的成功响应。
 *
 * <h3>数据结构</h3>
 * <pre>{@code
 * {
 *   "token": "a1b2c3d4...",     // 32 位随机 UUID Token
 *   "userInfo": {
 *     "id": 10001,
 *     "nickname": "藏友小明",
 *     "avatar": "https://...",
 *     "phone": "138****8000",    // 手机号已脱敏
 *     "points": 1280,
 *     "signInDays": 7
 *   }
 * }
 * }</pre>
 */
@Data
@Builder
public class LoginVO {

    /** 32 位随机 UUID Token */
    private String token;

    /** 用户基本信息 */
    private UserInfoVO userInfo;

    /**
     * 登录成功后回传的用户信息
     */
    @Data
    @Builder
    public static class UserInfoVO {
        /** 用户 ID */
        private Long id;
        /** 昵称 */
        private String nickname;
        /** 头像 URL */
        private String avatar;
        /** 脱敏手机号（138****8000） */
        private String phone;
        /** 积分 */
        private Integer points;
        /** 连续签到天数 */
        private Integer signInDays;
    }
}
