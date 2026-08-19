import type { AiChatResponse, AiHistoryResponse } from '@/api/ai'
import request from '@/utils/request'

/** 获取当前用户最近 20 条 AI 对话历史。 */
export function getAiHistory() {
  return request.get<unknown, AiHistoryResponse>('/api/ai/history')
}

/** 发送问题并等待 AI 返回完整回答。 */
export function sendAiQuestion(question: string) {
  return request.post<unknown, AiChatResponse>(
    '/api/ai/chat',
    { question },
    { timeout: 65_000 },
  )
}
