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
 * 用户实体 — 映射数据库 user 表
 *
 * <h3>字段说明</h3>
 * <ul>
 *   <li>id: 主键自增</li>
 *   <li>phone: 手机号（唯一标识，登录凭证）</li>
 *   <li>password: BCrypt 加密密码，短信登录用户可为空</li>
 *   <li>nickname: 昵称，自动注册默认为"藏友" + 6 位随机数字</li>
 *   <li>status: 1=正常, 0=禁用</li>
 *   <li>points: 积分</li>
 *   <li>signInDays: 连续签到天数</li>
 *   <li>deletedTime: 逻辑删除时间（NULL=未删除）</li>
 * </ul>
 *
 * <h3>自动填充</h3>
 * <p>createTime 和 updateTime 由 {@code MyMetaObjectHandler} 自动填充，
 * 无需手动设置。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user")
public class User implements Serializable {

    /** 主键，数据库自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 手机号，11 位数字，格式 ^1[3-9]\d{9}$ */
    private String phone;

    /** BCrypt 加密密码，短信登录用户可为 null */
    private String password;

    /** 用户昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatar;

    /** 账号状态：1=正常，0=禁用 */
    private Integer status;

    /** 积分 */
    private Integer points;

    /** 连续签到天数 */
    private Integer signInDays;

    /** 创建时间（自动填充） */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（自动填充：新增和修改时均更新） */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除时间，NULL=未删除，非NULL=删除时间 */
    private LocalDateTime deletedTime;
}
