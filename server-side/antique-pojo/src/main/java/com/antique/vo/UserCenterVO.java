package com.antique.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 个人中心初始化响应 VO
 *
 * <p>用于 GET /api/user/center/info 接口，返回用户信息 + 签到信息 + 统计信息。
 *
 * <h3>数据结构</h3>
 * <pre>{@code
 * {
 *   "userInfo": {
 *     "nickname": "藏友小明",
 *     "avatar": "http://example.com/avatar.jpg",
 *     "points": 1280,
 *     "signInDays": 7
 *   },
 *   "signInfo": {
 *     "isSignedToday": false,
 *     "continuousDays": 7,
 *     "todayReward": 14
 *   },
 *   "collectionCount": 12,
 *   "historyCount": 0
 * }
 * }</pre>
 */
@Data
@Builder
public class UserCenterVO {

    /** 用户基本信息 */
    private UserInfoVO userInfo;

    /** 签到信息 */
    private SignInfoVO signInfo;

    /** 我的收藏数（favorite 表统计） */
    private Integer collectionCount;

    /** 浏览历史数（预留字段，浏览记录表未实现，暂返回 0） */
    private Integer historyCount;

    /**
     * 用户基本信息
     */
    @Data
    @Builder
    public static class UserInfoVO {
        /** 昵称 */
        private String nickname;
        /** 头像 URL */
        private String avatar;
        /** 总积分（user.points） */
        private Integer points;
        /** 累计连续签到天数（user.sign_in_days，冗余字段） */
        private Integer signInDays;
    }

    /**
     * 签到信息
     */
    @Data
    @Builder
    public static class SignInfoVO {
        /** 今天是否已签到 */
        private Boolean isSignedToday;
        /** 回推计算后的当前连续天数 */
        private Integer continuousDays;
        /** 今日签到可获得积分（2 × 连续天数） */
        private Integer todayReward;
    }
}
