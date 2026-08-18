import request from '@/utils/request'
import type { ApiResponse } from './login'

// ==================== 类型定义 ====================

/** 浏览记录列表项 */
export interface HistoryItem {
  /** 浏览记录ID（删除单条时使用） */
  id: number
  /** 藏品ID（点击跳转藏品详情） */
  antiqueId: number
  /** 藏品名称 */
  title: string
  /** 藏品封面图 */
  coverImage: string
  /** 浏览时间（后端按此字段倒序返回，如 2026-08-18T12:30:00） */
  browseTime: string
}

/** 浏览记录列表响应数据体 */
export interface HistoryListResponse {
  list: HistoryItem[]
  total: number
}

// ==================== API 函数 ====================
// 说明：响应拦截器已解包 axios response，实际返回后端统一结构 { code, msg, data }，code === 1 表示成功；
// 请求拦截器统一携带 Authorization: Bearer ${token} 鉴权头

/**
 * 获取当前登录用户的浏览记录列表（后端按浏览时间倒序）
 * GET /api/history/list
 */
export function getHistoryList(): Promise<ApiResponse<HistoryListResponse>> {
  return request.get('/api/history/list')
}

/**
 * 删除单条浏览记录
 * POST /api/history/delete
 * @param id 浏览记录ID
 */
export function deleteHistory(id: number): Promise<ApiResponse<null>> {
  return request.post('/api/history/delete', { id })
}

/**
 * 一键清空全部浏览记录
 * POST /api/history/clear
 */
export function clearHistory(): Promise<ApiResponse<null>> {
  return request.post('/api/history/clear')
}
