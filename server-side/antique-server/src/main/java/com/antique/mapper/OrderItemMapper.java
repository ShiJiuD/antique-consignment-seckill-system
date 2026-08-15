package com.antique.mapper;

import com.antique.entity.OrderItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 订单明细表 Mapper 接口
 *
 * <p>继承 BaseMapper 自动获得单表 CRUD 方法；明细按 orderId 查询使用
 * LambdaQueryWrapper（in/eq）即可，无需自定义 SQL。
 */
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
