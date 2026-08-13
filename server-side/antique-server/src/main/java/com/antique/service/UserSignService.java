package com.antique.service;

import com.antique.vo.SignVO;
import com.antique.vo.UserCenterVO;

/**
 * 签到服务 — 个人中心初始化 + 执行签到
 *
 * <p>签到防重采用「Redis BitMap 极速防重 + 数据库唯一索引物理兜底」双保险，
 * 连续签到天数通过 user_sign_record 流水表回推（user 表冗余字段仅作展示）。
 */
public interface UserSignService {

    /**
     * 个人中心初始化（用户信息 + 签到信息 + 统计）
     *
     * @param userId 当前用户 ID
     * @return 个人中心响应 VO
     */
    UserCenterVO getCenterInfo(Long userId);

    /**
     * 执行签到（事务内：插入流水 + 更新积分与连续天数；事务外同步 Redis）
     *
     * @param userId 当前用户 ID
     * @return 签到响应 VO（最新积分 + 本次奖励）
     */
    SignVO sign(Long userId);
}
