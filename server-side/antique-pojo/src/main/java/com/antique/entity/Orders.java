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
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表实体 — 映射数据库 orders 表
 *
 * <h3>字段说明</h3>
 * <ul>
 *   <li>status: 0-待付款，1-待发货，2-待收货，3-已完成，4-已取消</li>
 *   <li>cancelType: 取消类型（1-用户取消，2-超时取消），仅 status=4 时有值</li>
 *   <li>payDeadline: 支付截止时间（创建 + 16 分钟），落库为权威依据，
 *       超时取消由"延迟消息 + 定时任务 + 惰性检查"三层保障（见技术方案文档）</li>
 *   <li>sellerId: 卖家用户 ID 冗余快照（创建时自 antique.seller_id 复制），V1 无卖家接口</li>
 *   <li>receiver*: 收货信息冗余快照，发货前可通过接口修改</li>
 *   <li>deletedTime: 逻辑删除时间（NULL=未删除），MyBatis-Plus 全局逻辑删除自动过滤</li>
 * </ul>
 *
 * <h3>自动填充</h3>
 * <p>createTime 和 updateTime 由 {@code MyMetaObjectHandler} 自动填充，
 * 无需手动设置。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("orders")
public class Orders implements Serializable {

    /** 主键，数据库自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 订单号（yyyyMMddHHmmss + 6 位随机数字） */
    private String orderNo;

    /** 买家用户 ID（关联 user.id） */
    private Long userId;

    /** 卖家用户 ID（冗余自 antique.seller_id） */
    private Long sellerId;

    /** 订单总金额（元），= Σ 明细小计 */
    private BigDecimal totalAmount;

    /** 实付金额（V1 = 总金额，预留优惠/运费） */
    private BigDecimal payAmount;

    /** 状态：0-待付款，1-待发货，2-待收货，3-已完成，4-已取消 */
    private Integer status;

    /** 取消类型：1-用户取消，2-超时取消（仅 status=4 时有值） */
    private Integer cancelType;

    /** 支付截止时间（创建时间 + 16 分钟，以本字段为准） */
    private LocalDateTime payDeadline;

    /** 收货人姓名 */
    private String receiverName;

    /** 收货人手机号 */
    private String receiverPhone;

    /** 收货地址 */
    private String receiverAddress;

    /** 买家备注 */
    private String remark;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 发货时间（V1 由运营后台人工处理） */
    private LocalDateTime shipTime;

    /** 确认收货时间 */
    private LocalDateTime receiveTime;

    /** 取消时间 */
    private LocalDateTime cancelTime;

    /** 完成时间（当前与确认收货一致，预留售后期满自动完成） */
    private LocalDateTime finishTime;

    /** 创建时间（自动填充） */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（自动填充：新增和修改时均更新） */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除时间，NULL=未删除，非NULL=删除时间（MyBatis-Plus 自动过滤） */
    private LocalDateTime deletedTime;
}
