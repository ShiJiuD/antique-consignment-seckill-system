package com.antique.util;

import com.antique.entity.OrderItem;
import com.antique.entity.Orders;
import com.antique.vo.OrderDetailItemVO;
import com.antique.vo.OrderDetailVO;
import com.antique.vo.OrderItemCardVO;
import com.antique.vo.OrderListItemVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单 VO 装配器 — 实体 → 展示 VO 的转换
 *
 * <p>与业务逻辑解耦：Service 只负责编排，转换规则集中在装配器，
 * 前端字段调整只改这里。
 */
public final class OrderVoAssembler {

    private OrderVoAssembler() {
    }

    /**
     * 订单 → 列表项 VO（手机号脱敏，item 取第一条明细快照）
     *
     * <p>payDeadline 仅待付款订单返回（前端倒计时），其他状态返回 null。
     *
     * @param item 第一条明细（当前一单一件），可能为 null（数据异常时防御）
     */
    public static OrderListItemVO toListItem(Orders order, OrderItem item) {
        return OrderListItemVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .payDeadline(order.getStatus() == 0 ? TimeUtil.format(order.getPayDeadline()) : null)
                .createdTime(TimeUtil.format(order.getCreateTime()))
                .receiverPhone(MaskUtil.maskPhone(order.getReceiverPhone()))
                .item(item == null ? null : toCard(item))
                .build();
    }

    /**
     * 订单 + 明细 → 详情 VO（生命周期时间未发生时返回 null）
     *
     * @param items      订单明细（可能为空列表）
     * @param sellerName 卖家名称（antique.seller_name 快照，可能为 null）
     */
    public static OrderDetailVO toDetail(Orders order, List<OrderItem> items, String sellerName) {
        return OrderDetailVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .status(order.getStatus())
                .cancelType(order.getCancelType())
                .totalAmount(order.getTotalAmount())
                .payAmount(order.getPayAmount())
                .payDeadline(TimeUtil.format(order.getPayDeadline()))
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .receiverAddress(order.getReceiverAddress())
                .remark(order.getRemark())
                .sellerId(order.getSellerId())
                .sellerName(sellerName)
                .payTime(TimeUtil.format(order.getPayTime()))
                .shipTime(TimeUtil.format(order.getShipTime()))
                .receiveTime(TimeUtil.format(order.getReceiveTime()))
                .cancelTime(TimeUtil.format(order.getCancelTime()))
                .finishTime(TimeUtil.format(order.getFinishTime()))
                .createdTime(TimeUtil.format(order.getCreateTime()))
                .items(items.stream().map(OrderVoAssembler::toDetailItem).collect(Collectors.toList()))
                .build();
    }

    /**
     * 明细实体 → 卡片 VO
     */
    public static OrderItemCardVO toCard(OrderItem item) {
        return OrderItemCardVO.builder()
                .antiqueId(item.getAntiqueId())
                .title(item.getAntiqueTitle())
                .coverImage(item.getAntiqueCover())
                .dynasty(item.getDynasty())
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .build();
    }

    /**
     * 明细实体 → 详情项 VO
     */
    public static OrderDetailItemVO toDetailItem(OrderItem item) {
        return OrderDetailItemVO.builder()
                .antiqueId(item.getAntiqueId())
                .title(item.getAntiqueTitle())
                .coverImage(item.getAntiqueCover())
                .dynasty(item.getDynasty())
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .totalPrice(item.getTotalPrice())
                .build();
    }
}
