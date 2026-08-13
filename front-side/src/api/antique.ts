import request from '@/utils/request'
import type { ApiResponse } from './login'

// ==================== 类型定义 ====================

/** 藏品列表项 */
export interface AntiqueItem {
  id: number
  title: string
  dynasty: string
  price: number
  coverImage: string
  /** 是否热门：1-是 0-否 */
  isHot: number
}

/** 列表分页响应数据体 */
export interface AntiqueListResponse {
  list: AntiqueItem[]
  total: number
  page: number
  size: number
}

/**
 * 藏品详情（与后端 GET /api/antique/{id} 返回的 data 字段一一对应）
 * 全部字段小驼峰，isHot/status 为数字枚举
 */
export interface AntiqueDetail {
  id: number
  title: string
  categoryId: number
  subCategory: string
  dynasty: string
  material: string
  price: number
  coverImage: string
  /** 详情图片地址数组，可能为空数组 */
  images: string[]
  description: string
  sellerId: number
  sellerName: string
  viewCount: number
  likeCount: number
  /** 是否热门：1-是 0-否 */
  isHot: number
  /** 藏品状态：1-在售 0-下架 */
  status: number
  createdTime: string
  isFavorited: boolean
}

// ==================== 请求参数类型 ====================

/** 藏品列表查询参数 */
export interface AntiqueListParams {
  categoryId?: number
  isHot?: number
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
// 注意：响应拦截器已解包 axios response，
// 实际返回为后端统一结构 { code, msg, data }，code === 1 表示成功

/**
 * 获取藏品列表
 */
export function getAntiqueList(params: AntiqueListParams): Promise<ApiResponse<AntiqueListResponse>> {
  return request.get('/api/antique/list', { params })
}

/**
 * 获取藏品详情
 * GET /api/antique/{id}
 */
export function getAntiqueDetail(id: number): Promise<ApiResponse<AntiqueDetail>> {
  return request.get(`/api/antique/${id}`)
}

/**
 * 搜索藏品
 */
export function searchAntique(params: SearchAntiqueParams): Promise<ApiResponse<AntiqueListResponse>> {
  return request.get('/api/antique/search', { params })
}
