import request from '@/utils/request'

// ==================== 类型定义 ====================

/** 藏品列表项 */
export interface AntiqueItem {
  id: number
  title: string
  dynasty: string
  price: number
  coverImage: string
  isHot: boolean
}

/** 列表分页响应 */
export interface AntiqueListResponse {
  list: AntiqueItem[]
  total: number
  page: number
  size: number
}

/** 藏品详情 */
export interface AntiqueDetail {
  id: number
  title: string
  categoryId: number
  subCategory: string
  dynasty: string
  material: string
  price: number
  coverImage: string
  images: string[]
  description: string
  sellerId: number
  sellerName: string
  viewCount: number
  likeCount: number
  isHot: boolean
  status: number
  createdTime: string
  isFavorited: boolean
}

// ==================== 请求参数类型 ====================

/** 藏品列表查询参数 */
export interface AntiqueListParams {
  categoryId?: number
  isHot?: boolean
  page?: number
  size?: number
}

/** 藏品搜索参数 */
export interface SearchAntiqueParams {
  keyword?: string
  categoryId?: number
  page?: number
  size?: number
}

// ==================== API 函数 ====================

/**
 * 获取藏品列表
 */
export function getAntiqueList(params: AntiqueListParams): Promise<AntiqueListResponse> {
  return request.get('/api/antique/list', { params })
}

/**
 * 获取藏品详情
 */
export function getAntiqueDetail(id: number): Promise<AntiqueDetail> {
  return request.get(`/api/antique/${id}`)
}

/**
 * 搜索藏品
 */
export function searchAntique(params: SearchAntiqueParams): Promise<AntiqueListResponse> {
  return request.get('/api/antique/search', { params })
}
