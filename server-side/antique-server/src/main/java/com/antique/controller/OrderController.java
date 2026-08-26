package com.antique.controller;

import com.antique.constant.MessageConstant;
import com.antique.context.UserContext;
import com.antique.dto.OrderAddressDTO;
import com.antique.dto.OrderCreateDTO;
import com.antique.dto.OrderIdDTO;
import com.antique.dto.OrderListQueryDTO;
import com.antique.result.Result;
import com.antique.service.OrderService;
import com.antique.vo.OrderCreateVO;
import com.antique.vo.OrderDetailVO;
import com.antique.vo.OrderListItemVO;
import com.antique.vo.OrderPayVO;
import com.antique.vo.PageResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单控制器 — 买家订单全生命周期
 *
 * <p>路径前缀: /api/order（需认证，Token 校验由 TokenInterceptor 完成）
 * <p>当前用户 ID 从 {@code UserContext.getUserId()} 获取，无法伪造。
 *
 * <h3>接口列表</h3>
 * <table>
 *   <tr><th>接口</th><th>路径</th><th>认证</th></tr>
 *   <tr><td>创建订单</td><td>POST /api/order/create</td><td>🔒</td></tr>
 *   <tr><td>订单支付</td><td>POST /api/order/pay</td><td>🔒</td></tr>
 *   <tr><td>取消订单</td><td>POST /api/order/cancel</td><td>🔒</td></tr>
 *   <tr><td>我的订单列表</td><td>GET /api/order/list</td><td>🔒</td></tr>
 *   <tr><td>订单详情</td><td>GET /api/order/{id}</td><td>🔒</td></tr>
 *   <tr><td>修改收货地址</td><td>POST /api/order/address</td><td>🔒</td></tr>
 *   <tr><td>确认收货</td><td>POST /api/order/receive</td><td>🔒</td></tr>
 *   <tr><td>催发货</td><td>POST /api/order/urge</td><td>🔒</td></tr>
 * </table>
 *
 * <p>超时自动取消（16 分钟）为后台能力，不提供接口，见技术方案文档。
 */
@Slf4j
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Tag(name = "订单模块", description = "创建/支付/取消/列表/详情/地址/收货/催发货")
public class OrderController {

    private final OrderService orderService;

    /**
     * 接口 1：创建订单
     *
     * <p>POST /api/order/create
     * <p>买家购买藏品，创建订单并锁定藏品；16 分钟未支付自动取消。
     * <p>收货信息三项必填（前端暂为写死的假数据上传）。
     *
     * @param dto 藏品ID + 收货信息
     * @return 订单ID/订单号/待支付金额/支付截止时间（前端倒计时）
     */
    @Operation(summary = "创建订单")
    @PostMapping("/create")
    public Result<OrderCreateVO> create(@RequestBody @Valid OrderCreateDTO dto) {
        Long userId = UserContext.getUserId();
        log.info("创建订单: userId={}, antiqueId={}", userId, dto.getAntiqueId());
        OrderCreateVO vo = orderService.createOrder(userId, dto);
        return Result.success(vo, MessageConstant.ORDER_CREATE_SUCCESS);
    }

    /**
     * 接口 2：订单支付
     *
     * <p>POST /api/order/pay
     * <p>模拟支付（无真实支付渠道），仅待付款订单可支付。
     *
     * @param dto 订单ID
     * @return 支付后状态与支付时间
     */
    @Operation(summary = "订单支付")
    @PostMapping("/pay")
    public Result<OrderPayVO> pay(@RequestBody @Valid OrderIdDTO dto) {
        Long userId = UserContext.getUserId();
        log.info("订单支付: userId={}, orderId={}", userId, dto.getOrderId());
        OrderPayVO vo = orderService.pay(userId, dto.getOrderId());
        return Result.success(vo, MessageConstant.ORDER_PAY_SUCCESS);
    }

    /**
     * 接口 3：取消订单
     *
     * <p>POST /api/order/cancel
     * <p>仅待付款订单可取消，回滚藏品为在售。
     */
    @Operation(summary = "取消订单")
    @PostMapping("/cancel")
    public Result<?> cancel(@RequestBody @Valid OrderIdDTO dto) {
        Long userId = UserContext.getUserId();
        log.info("取消订单: userId={}, orderId={}", userId, dto.getOrderId());
        orderService.cancel(userId, dto.getOrderId());
        return Result.success(null, MessageConstant.ORDER_CANCEL_SUCCESS);
    }

    /**
     * 接口 4：我的订单列表
     *
     * <p>GET /api/order/list
     * <p>按状态筛选（不传=全部）；待付款订单附带支付倒计时；查询前惰性处理超时订单。
     */
    @Operation(summary = "我的订单列表")
    @GetMapping("/list")
    public Result<PageResultVO<OrderListItemVO>> list(@Valid OrderListQueryDTO dto) {
        Long userId = UserContext.getUserId();
        log.info("我的订单列表: userId={}, status={}", userId, dto.getStatus());
        PageResultVO<OrderListItemVO> data = orderService.pageList(
                userId, dto.getStatus(), dto.getPage(), dto.getSize());
        return Result.success(data, MessageConstant.QUERY_SUCCESS);
    }

    /**
     * 接口 5：订单详情
     *
     * <p>GET /api/order/{id}
     * <p>仅买家本人可查看，手机号不脱敏。
     */
    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        log.info("订单详情: userId={}, orderId={}", userId, id);
        OrderDetailVO vo = orderService.getDetail(userId, id);
        return Result.success(vo, MessageConstant.QUERY_SUCCESS);
    }

    /**
     * 接口 6：修改收货地址
     *
     * <p>POST /api/order/address
     * <p>仅未发货订单（待付款/待发货）可修改，对应"修改地址"按钮。
     */
    @Operation(summary = "修改收货地址")
    @PostMapping("/address")
    public Result<?> updateAddress(@RequestBody @Valid OrderAddressDTO dto) {
        Long userId = UserContext.getUserId();
        log.info("修改收货地址: userId={}, orderId={}", userId, dto.getOrderId());
        orderService.updateAddress(userId, dto);
        return Result.success(null, MessageConstant.ORDER_ADDRESS_UPDATE_SUCCESS);
    }

    /**
     * 接口 7：确认收货
     *
     * <p>POST /api/order/receive
     * <p>待收货 → 已完成。
     */
    @Operation(summary = "确认收货")
    @PostMapping("/receive")
    public Result<?> receive(@RequestBody @Valid OrderIdDTO dto) {
        Long userId = UserContext.getUserId();
        log.info("确认收货: userId={}, orderId={}", userId, dto.getOrderId());
        orderService.receive(userId, dto.getOrderId());
        return Result.success(null, MessageConstant.ORDER_RECEIVE_SUCCESS);
    }

    /**
     * 接口 8：催发货
     *
     * <p>POST /api/order/urge
     * <p>模拟通知卖家，后续接入消息模块。
     */
    @Operation(summary = "催发货")
    @PostMapping("/urge")
    public Result<?> urge(@RequestBody @Valid OrderIdDTO dto) {
        Long userId = UserContext.getUserId();
        log.info("催发货: userId={}, orderId={}", userId, dto.getOrderId());
        orderService.urge(userId, dto.getOrderId());
        return Result.success(null, MessageConstant.ORDER_URGE_SUCCESS);
    }
}
