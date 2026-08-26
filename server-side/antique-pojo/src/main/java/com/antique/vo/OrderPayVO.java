package com.antique.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单支付响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPayVO implements Serializable {

    /** 订单 ID */
    private Long orderId;

    /** 支付后订单状态（恒为 1-待发货） */
    private Integer status;

    /** 支付时间（格式 yyyy-MM-dd HH:mm:ss） */
    private String payTime;
}
