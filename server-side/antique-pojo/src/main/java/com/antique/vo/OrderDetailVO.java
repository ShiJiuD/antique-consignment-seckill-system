package com.antique.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 订单详情 VO — 订单完整信息（仅买家本人查看，手机号不脱敏）
 *
 * <p>生命周期时间字段（payTime/shipTime/...）未发生时返回 null；
 * sellerName 取自 antique.seller_name 快照。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailVO implements Serializable {

    /** 订单 ID */
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 订单状态：0-待付款，1-待发货，2-待收货，3-已完成，4-已取消 */
    private Integer status;

    /** 取消类型：1-用户取消，2-超时取消（仅已取消订单有值） */
    private Integer cancelType;

    /** 订单总金额（元） */
    private BigDecimal totalAmount;

    /** 实付金额（元，V1 = 总金额） */
    private BigDecimal payAmount;

    /** 支付截止时间（格式 yyyy-MM-dd HH:mm:ss） */
    private String payDeadline;

    /** 收货人姓名 */
    private String receiverName;

    /** 收货人手机号（本人查看不脱敏） */
    private String receiverPhone;

    /** 收货地址 */
    private String receiverAddress;

    /** 买家备注 */
    private String remark;

    /** 卖家用户 ID */
    private Long sellerId;

    /** 卖家名称（antique.seller_name 快照） */
    private String sellerName;

    /** 支付时间 */
    private String payTime;

    /** 发货时间（V1 由运营后台人工处理） */
    private String shipTime;

    /** 确认收货时间 */
    private String receiveTime;

    /** 取消时间 */
    private String cancelTime;

    /** 完成时间 */
    private String finishTime;

    /** 创建时间 */
    private String createdTime;

    /** 订单明细（含藏品快照与金额） */
    private List<OrderDetailItemVO> items;
}
