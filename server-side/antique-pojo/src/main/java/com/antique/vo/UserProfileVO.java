package com.antique.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户个人信息响应 VO
 *
 * <p>用于 GET /api/user/profile 接口，返回比 LoginVO 更完整的用户信息。
 *
 * <h3>与 LoginVO 的差异</h3>
 * <ul>
 *   <li>多了 status 字段（账号状态）</li>
 *   <li>多了 createdTime 字段（注册时间）</li>
 *   <li>没有 token 字段（仅查询信息，不重新生成 token）</li>
 * </ul>
 */
@Data
@Builder
public class UserProfileVO {

    /** 用户 ID */
    private Long id;
    /** 昵称 */
    private String nickname;
    /** 头像 URL */
    private String avatar;
    /** 脱敏手机号（138****8000） */
    private String phone;
    /** 账号状态：1=正常, 0=禁用 */
    private Integer status;
    /** 积分 */
    private Integer points;
    /** 连续签到天数 */
    private Integer signInDays;
    /** 注册时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
}
