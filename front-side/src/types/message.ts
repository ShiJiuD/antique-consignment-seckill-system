/**
 * 消息模块 TS 类型定义
 * 对应接口文档：05消息接口文档
 */

/** 消息类型：1-系统通知（预留） 2-订单消息 3-AI助手（前端全部忽略不渲染） */
export type MessageType = 1 | 2 | 3

/** 已读状态：0-未读 1-已读 */
export type IsRead = 0 | 1

/** 单条消息 */
export interface MessageItem {
  /** 消息ID */
  messageId: number
  /** 消息类型：1系统通知 2订单消息 3AI助手 */
  type: MessageType
  /** 消息标题 */
  title: string
  /** 消息内容摘要 */
  content: string
  /** 创建时间 */
  createdTime: string
  /** 已读状态：0未读 1已读 */
  isRead: IsRead
}

/** 消息分页列表响应 data（GET /api/message/list） */
export interface MessageListResult {
  /** 消息列表（按创建时间倒序，最新在最上方） */
  list: MessageItem[]
  /** 总条数 */
  total: number
  /** 当前页码 */
  page: number
  /** 每页条数 */
  size: number
}

/** 消息分页查询参数（GET /api/message/list） */
export interface MessageListParams {
  /** 页码，从 1 开始，默认 1 */
  page: number
  /** 每页条数，默认 10 */
  size: number
}

/** 未读消息总数响应 data（GET /api/message/unread-count） */
export interface UnreadCountResult {
  /** 未读消息总数 */
  count: number
}

/** WebSocket 服务端推送消息 */
export interface WsPushMessage {
  /** 推送类型：目前仅处理 newMessage */
  type: string
  /** 推送数据：新消息内容 */
  data?: MessageItem | null
}
