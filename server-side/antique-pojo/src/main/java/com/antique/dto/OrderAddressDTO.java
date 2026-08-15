package com.antique.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 修改收货地址参数
 *
 * <p>接口：POST /api/order/address
 * <p>仅未发货订单（待付款/待发货）可修改。
 */
@Data
public class OrderAddressDTO {

    /** 订单 ID */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /** 收货人姓名 */
    @NotBlank(message = "收货人姓名不能为空")
    private String receiverName;

    /** 收货人手机号 */
    @NotBlank(message = "收货人手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "收货人手机号格式不正确")
    private String receiverPhone;

    /** 收货地址 */
    @NotBlank(message = "收货地址不能为空")
    private String receiverAddress;
}
