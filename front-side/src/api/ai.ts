/** 后端统一响应结构。 */
export interface ApiResponse<T> {
  code: number
  msg: string
  data: T | null
}

/** AI 对话中的单条消息。 */
export interface AiMessage {
  role: 'user' | 'assistant'
  content: string
}

/** GET /api/ai/history 的 data。 */
export interface AiHistoryData {
  list: AiMessage[]
}

/** POST /api/ai/chat 的 data。 */
export interface AiChatData {
  answer: string
}

export type AiHistoryResponse = ApiResponse<AiHistoryData>
export type AiChatResponse = ApiResponse<AiChatData>
