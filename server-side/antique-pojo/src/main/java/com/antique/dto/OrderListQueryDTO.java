package com.antique.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 我的订单列表查询参数
 *
 * <p>接口：GET /api/order/list
 *
 * <h3>参数说明</h3>
 * <ul>
 *   <li>status: 0-待付款，1-待发货，2-待收货，3-已完成；不传=全部（含已取消）；传 5=退款/售后（V1 预留，恒返回空列表）</li>
 *   <li>page/size 缺省时由 Service 层归一化（1/10），越界由 @Min/@Max 拦截</li>
 * </ul>
 */
@Data
public class OrderListQueryDTO {

    /** 页码，缺省默认 1，不能小于 1 */
    @Min(value = 1, message = "页码不能小于1")
    private Integer page;

    /** 每页数量，缺省默认 10，范围 1-50 */
    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 50, message = "每页数量不能大于50")
    private Integer size;

    /** 订单状态筛选（不传=全部；5=退款/售后预留） */
    private Integer status;
}
