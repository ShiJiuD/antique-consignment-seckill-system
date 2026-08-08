import request from '@/utils/request'
import type { AntiqueItem } from './antique'

// ==================== 类型定义 ====================

/** 收藏列表分页响应 */
export interface FavoriteListResponse {
  list: AntiqueItem[]
  total: number
  page: number
  size: number
}

/** 收藏列表查询参数 */
export interface FavoriteListParams {
  page?: number
  size?: number
}

// ==================== API 函数 ====================

/**
 * 添加收藏
 * @param antiqueId 藏品ID
 */
export function addFavorite(antiqueId: number): Promise<void> {
  return request.post('/api/favorite/add', { antiqueId })
}

/**
 * 取消收藏
 * @param antiqueId 藏品ID
 */
export function removeFavorite(antiqueId: number): Promise<void> {
  return request.post('/api/favorite/remove', { antiqueId })
}

/**
 * 获取收藏列表
 * @param params 分页参数
 */
export function getFavoriteList(params: FavoriteListParams): Promise<FavoriteListResponse> {
  return request.get('/api/favorite/list', { params })
}
