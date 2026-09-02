import request from '@/utils/request'
import type { ApiResponse } from './login'
import type { MessageListParams, MessageListResult, UnreadCountResult } from '@/types/message'

// ==================== 消息模块 API ====================
// 说明：响应拦截器已解包 axios response，实际返回后端统一结构 { code, msg, data }，code === 1 表示成功；
// 请求拦截器统一携带 Authorization: Bearer ${token} 鉴权头

/**
 * 消息分页列表
 * GET /api/message/list
 * page 默认 1，size 默认 10；按创建时间倒序，新消息在最上方
 */
export function getMessageList(params: MessageListParams): Promise<ApiResponse<MessageListResult>> {
  return request.get('/api/message/list', { params })
}

/**
 * 获取未读消息总数（用于全局角标）
 * GET /api/message/unread-count
 * WebSocket 收到 newMessage 推送时重新调用该接口刷新未读数
 */
export function getUnreadCount(): Promise<ApiResponse<UnreadCountResult>> {
  return request.get('/api/message/unread-count')
}

/**
 * 单条消息标记已读（接口幂等）
 * POST /api/message/read
 * 点击单条消息项时调用，入参 messageId
 */
export function readMessage(messageId: number): Promise<ApiResponse<null>> {
  return request.post('/api/message/read', { messageId })
}
