package com.antique.service;

import com.antique.dto.OrderAddressDTO;
import com.antique.dto.OrderCreateDTO;
import com.antique.vo.OrderCreateVO;
import com.antique.vo.OrderDetailVO;
import com.antique.vo.OrderListItemVO;
import com.antique.vo.OrderPayVO;
import com.antique.vo.PageResultVO;

/**
 * 订单服务接口 — 买家订单全生命周期
 *
 * <p>接口定义见《接口文档/04订单接口文档.md》，超时关单见《技术方案文档/01订单超时自动取消技术方案.md》。
 */
public interface OrderService {

    /**
     * 创建订单（接口 1）
     *
     * <p>锁定藏品 + 写入订单主表/明细，16 分钟未支付自动取消（延迟消息 + 定时任务兜底）。
     */
    OrderCreateVO createOrder(Long userId, OrderCreateDTO dto);

    /**
     * 订单支付（接口 2）— 模拟支付，仅待付款订单可支付
     */
    OrderPayVO pay(Long userId, Long orderId);

    /**
     * 取消订单（接口 3）— 仅待付款订单可取消，回滚藏品
     */
    void cancel(Long userId, Long orderId);

    /**
     * 我的订单列表（接口 4）— 按状态筛选，查询前惰性处理超时订单
     */
    PageResultVO<OrderListItemVO> pageList(Long userId, Integer status, Integer page, Integer size);

    /**
     * 订单详情（接口 5）— 仅买家本人查看
     */
    OrderDetailVO getDetail(Long userId, Long orderId);

    /**
     * 修改收货地址（接口 6）— 仅未发货订单（待付款/待发货）可修改
     */
    void updateAddress(Long userId, OrderAddressDTO dto);

    /**
     * 确认收货（接口 7）— 待收货 → 已完成
     */
    void receive(Long userId, Long orderId);

    /**
     * 催发货（接口 8）— 模拟通知卖家，后续接入消息模块
     */
    void urge(Long userId, Long orderId);

    /**
     * 超时关单（技术方案：延迟消息消费者 / 定时任务兜底 / 惰性检查共用）
     *
     * <p>仅待付款且已超时（DB 时间 NOW() 为准）的订单流转为已取消（cancelType=2），
     * 并幂等回滚藏品。条件更新保证与支付/其他关单方并发时只生效一次。
     */
    void closeOrderByTimeout(Long orderId);
}
