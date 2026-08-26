package com.antique.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 签到流水实体 — 映射数据库 user_sign_record 表
 *
 * <h3>字段说明</h3>
 * <ul>
 *   <li>id: 主键自增</li>
 *   <li>userId: 用户 ID（关联 user.id）</li>
 *   <li>signDate: 签到日期，一天一行</li>
 *   <li>pointsEarned: 本次签到获得的积分</li>
 * </ul>
 *
 * <h3>约束</h3>
 * <p>数据库唯一索引 {@code uk_user_date(user_id, sign_date)} 保证
 * 同一用户同一天只能签到一次（物理防重的最后防线，并发兜底）。
 *
 * <h3>设计说明</h3>
 * <ul>
 *   <li>本表是签到的物理留存与对账源，user 表的 points/sign_in_days/last_sign_date 均为冗余字段</li>
 *   <li>Redis BitMap（sign:{userId}:{yyyyMM}）是防重缓存，本表才是最终依据</li>
 *   <li>连续签到天数通过本表回推（最后一条 sign_date 与今天/昨天对比）</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_sign_record")
public class UserSignRecord implements Serializable {

    /** 主键，数据库自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户 ID（关联 user.id） */
    private Long userId;

    /** 签到日期（一天一行，格式：2026-08-13） */
    private LocalDate signDate;

    /** 本次签到获得的积分 */
    private Integer pointsEarned;

    /** 签到时间（自动填充） */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
