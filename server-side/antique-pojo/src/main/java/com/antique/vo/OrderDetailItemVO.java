package com.antique.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单详情明细 VO — 含金额的完整明细信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailItemVO implements Serializable {

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

    /** 小计金额（= price × quantity） */
    private BigDecimal totalPrice;
}
