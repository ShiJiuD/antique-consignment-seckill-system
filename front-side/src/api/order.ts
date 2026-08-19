import request from '@/utils/request'
import type { ApiResponse } from './login'

// ==================== 类型定义 ====================

/** 订单藏品快照（下单时的藏品信息快照） */
export interface OrderItem {
  /** 快照ID */
  id: number
  /** 藏品ID */
  antiqueId: number
  /** 藏品名称 */
  title: string
  /** 封面图 */
  coverImage: string
  /** 年代 */
  dynasty: string
  /** 单价 */
  price: number
  /** 数量 */
  quantity: number
  /** 小计（单价 × 数量） */
  subtotal: number
}

/**
 * 订单状态：
 * 0-待付款 1-待发货 2-待收货 3-已完成 4-已取消 5-退款/售后
 */
export type OrderStatus = 0 | 1 | 2 | 3 | 4 | 5

/** 订单完整信息（GET /api/order/{id} 返回的 data） */
export interface Order {
  /** 订单ID */
  id: number
  /** 订单号 */
  orderNo: string
  /** 订单状态 */
  status: OrderStatus
  /** 取消类型（1-用户取消 2-超时未支付自动取消），无取消时后端可能不返回 */
  cancelType?: number
  /** 收货人姓名 */
  receiverName: string
  /** 收货人手机号 */
  receiverPhone: string
  /** 收货地址 */
  receiverAddress: string
  /** 买家备注 */
  buyerRemark?: string
  /** 藏品快照列表 */
  items: OrderItem[]
  /** 订单总金额 */
  totalAmount: number
  /** 实付金额 */
  payAmount: number
  /** 创建时间 */
  createTime: string
  /** 支付时间 */
  payTime?: string
  /** 发货时间 */
  shipTime?: string
  /** 确认收货时间 */
  receiveTime?: string
  /** 取消时间 */
  cancelTime?: string
  /** 支付截止时间（待付款倒计时依据） */
  payDeadline: string
}

/** 订单列表分页响应数据体 */
export interface OrderListResponse {
  list: Order[]
  total: number
  page: number
  size: number
}

/** 订单列表查询参数 */
export interface OrderListParams {
  /** 订单状态：不传=全部，0待付款 1待发货 2待收货 3已完成 5退款/售后 */
  status?: number
  /** 页码（从 1 开始） */
  page: number
  /** 每页条数 */
  size: number
}

/** 创建订单入参（POST /api/order/create） */
export interface CreateOrderParams {
  /** 藏品ID */
  antiqueId: number
  /** 收货人姓名 */
  receiverName: string
  /** 收货人手机号 */
  receiverPhone: string
  /** 收货地址 */
  receiverAddress: string
  /** 买家备注（选填） */
  buyerRemark?: string
}

/** 创建订单返回（data） */
export interface CreateOrderResult {
  /** 新订单ID（跳转模拟支付页携带） */
  orderId: number
  /** 支付截止时间（跳转模拟支付页携带，用于倒计时） */
  payDeadline: string
}

/** 模拟支付返回（data） */
export interface PayOrderResult {
  orderId: number
}

/** 修改收货地址入参（POST /api/order/address） */
export interface UpdateAddressParams {
  orderId: number
  receiverName: string
  receiverPhone: string
  receiverAddress: string
}

// ==================== API 函数 ====================
// 说明：响应拦截器已解包 axios response，实际返回后端统一结构 { code, msg, data }，code === 1 表示成功；
// 请求拦截器统一携带 Authorization: Bearer ${token} 鉴权头

/** 创建订单 POST /api/order/create */
export function createOrder(params: CreateOrderParams): Promise<ApiResponse<CreateOrderResult>> {
  return request.post('/api/order/create', params)
}

/** 模拟支付 POST /api/order/pay */
export function payOrder(orderId: number): Promise<ApiResponse<PayOrderResult>> {
  return request.post('/api/order/pay', { orderId })
}

/** 取消订单 POST /api/order/cancel */
export function cancelOrder(orderId: number): Promise<ApiResponse<null>> {
  return request.post('/api/order/cancel', { orderId })
}

/** 获取我的订单列表（分页） GET /api/order/list */
export function getOrderList(params: OrderListParams): Promise<ApiResponse<OrderListResponse>> {
  return request.get('/api/order/list', { params })
}

/** 获取订单详情 GET /api/order/{id} */
export function getOrderDetail(id: number): Promise<ApiResponse<Order>> {
  return request.get(`/api/order/${id}`)
}

/** 修改收货地址 POST /api/order/address */
export function updateOrderAddress(params: UpdateAddressParams): Promise<ApiResponse<null>> {
  return request.post('/api/order/address', params)
}

/** 确认收货 POST /api/order/receive */
export function confirmReceive(orderId: number): Promise<ApiResponse<null>> {
  return request.post('/api/order/receive', { orderId })
}

/** 催发货 POST /api/order/urge */
export function urgeShip(orderId: number): Promise<ApiResponse<null>> {
  return request.post('/api/order/urge', { orderId })
}
