package com.antique.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 订单 ID 请求参数 — 支付/取消/确认收货/催发货共用
 *
 * <p>接口：POST /api/order/{pay|cancel|receive|urge}
 */
@Data
public class OrderIdDTO {

    /** 订单 ID */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;
}
