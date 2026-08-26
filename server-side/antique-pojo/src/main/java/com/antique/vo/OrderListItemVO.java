package com.antique.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 我的订单列表项 VO — 订单卡片展示字段
 *
 * <p>receiverPhone 为脱敏展示（138****8000）；item 为第一条明细快照（当前一单一件）；
 * payDeadline 仅待付款订单有值，前端倒计时。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderListItemVO implements Serializable {

    /** 订单 ID */
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 订单状态：0-待付款，1-待发货，2-待收货，3-已完成，4-已取消 */
    private Integer status;

    /** 订单总金额（元） */
    private BigDecimal totalAmount;

    /** 支付截止时间（仅待付款订单有值，格式 yyyy-MM-dd HH:mm:ss），前端倒计时 */
    private String payDeadline;

    /** 创建时间（格式 yyyy-MM-dd HH:mm:ss） */
    private String createdTime;

    /** 收货人手机号（脱敏展示） */
    private String receiverPhone;

    /** 第一条明细快照（藏品卡片展示用） */
    private OrderItemCardVO item;
}
