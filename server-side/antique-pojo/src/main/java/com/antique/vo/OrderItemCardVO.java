package com.antique.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单明细卡片 VO — 订单列表/详情中藏品快照的展示字段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemCardVO implements Serializable {

    /** 藏品 ID */
    private Long antiqueId;

    /** 藏品名称（下单时快照） */
    private String title;

    /** 藏品封面图 URL（下单时快照） */
    private String coverImage;

    /** 年代（下单时快照） */
    private String dynasty;

    /** 成交单价（元，下单时快照） */
    private BigDecimal price;

    /** 购买数量（当前恒为 1） */
    private Integer quantity;
}
