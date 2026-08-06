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
import java.time.LocalDateTime;

/**
 * 收藏实体 — 映射数据库 favorite 表
 *
 * <h3>字段说明</h3>
 * <ul>
 *   <li>id: 主键自增</li>
 *   <li>userId: 用户 ID（关联 user.id）</li>
 *   <li>antiqueId: 藏品 ID（关联 antique.id）</li>
 * </ul>
 *
 * <h3>约束</h3>
 * <p>数据库唯一索引 {@code uk_user_antique(user_id, antique_id)} 保证
 * 同一用户同一藏品只能收藏一次（防重复收藏的最后防线）。
 *
 * <h3>设计说明</h3>
 * <p>收藏数不实时 COUNT 本表，而是同步维护 antique.like_count 冗余字段，
 * 避免高频查询下 COUNT 开销过大。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("favorite")
public class Favorite implements Serializable {

    /** 主键，数据库自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户 ID（关联 user.id） */
    private Long userId;

    /** 藏品 ID（关联 antique.id） */
    private Long antiqueId;

    /** 收藏时间（自动填充） */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
