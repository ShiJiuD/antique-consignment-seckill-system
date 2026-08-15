package com.antique.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 创建订单参数
 *
 * <p>接口：POST /api/order/create
 *
 * <h3>参数说明</h3>
 * <ul>
 *   <li>antiqueId 必填；quantity 缺省默认 1，当前仅支持 1（多件购买预留）</li>
 *   <li>收货信息三项必填（前端暂为写死的假数据上传）</li>
 *   <li>receiverPhone 与登录手机号同格式校验（^1[3-9]\d{9}$）</li>
 * </ul>
 */
@Data
public class OrderCreateDTO {

    /** 藏品 ID */
    @NotNull(message = "藏品ID不能为空")
    private Long antiqueId;

    /** 数量，缺省默认 1，当前仅支持 1 */
    @Min(value = 1, message = "购买数量不能小于1")
    private Integer quantity;

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

    /** 买家备注（可选） */
    private String remark;
}
