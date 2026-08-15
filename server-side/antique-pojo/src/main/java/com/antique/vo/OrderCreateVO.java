package com.antique.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 创建订单响应 VO
 *
 * <p>返回订单 ID、订单号、待支付金额与支付截止时间，
 * 前端以 payDeadline 做 16 分钟倒计时。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateVO implements Serializable {

    /** 订单 ID */
    private Long orderId;

    /** 订单号 */
    private String orderNo;

    /** 待支付金额（元） */
    private BigDecimal totalAmount;

    /** 支付截止时间（创建 + 16 分钟，格式 yyyy-MM-dd HH:mm:ss），前端倒计时 */
    private String payDeadline;
}
