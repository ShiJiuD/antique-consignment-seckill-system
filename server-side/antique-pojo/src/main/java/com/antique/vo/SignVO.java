package com.antique.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 签到响应 VO
 *
 * <p>用于 POST /api/user/sign 接口的成功响应。
 *
 * <h3>数据结构</h3>
 * <pre>{@code
 * {
 *   "newPoints": 1294,
 *   "todayReward": 14
 * }
 * }</pre>
 */
@Data
@Builder
public class SignVO {

    /** 签到后的最新总积分 */
    private Integer newPoints;

    /** 本次签到获得的积分（2 × 连续天数） */
    private Integer todayReward;
}
