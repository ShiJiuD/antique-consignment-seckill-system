import request from '@/utils/request'
import type { ApiResponse } from './login'
import type { AntiqueItem } from './antique'

// ==================== 类型定义 ====================

/**
 * 收藏列表项
 * 在藏品列表项基础上补充「简介」字段，供收藏卡片展示封面、名称、简介
 */
export interface FavoriteItem extends AntiqueItem {
  /** 藏品简介 */
  description?: string
}

/** 收藏列表分页响应数据体 */
export interface FavoriteListResponse {
  list: FavoriteItem[]
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
// 说明：响应拦截器已解包 axios response，实际返回后端统一结构 { code, msg, data }，code === 1 表示成功；
// 请求拦截器统一携带 Authorization: Bearer ${token} 鉴权头

/**
 * 添加收藏
 * @param antiqueId 藏品ID
 */
export function addFavorite(antiqueId: number): Promise<ApiResponse<null>> {
  return request.post('/api/favorite/add', { antiqueId })
}

/**
 * 取消收藏
 * @param antiqueId 藏品ID
 */
export function removeFavorite(antiqueId: number): Promise<ApiResponse<null>> {
  return request.post('/api/favorite/remove', { antiqueId })
}

/**
 * 获取当前登录用户收藏的藏品列表
 * @param params 分页参数
 */
export function getFavoriteList(
  params: FavoriteListParams
): Promise<ApiResponse<FavoriteListResponse>> {
  return request.get('/api/favorite/list', { params })
}
