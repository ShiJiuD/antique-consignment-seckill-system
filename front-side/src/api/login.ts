// import request from '@/utils/request'

// ========== 通用类型 ==========

/** 后端统一响应结构 */
export interface ApiResponse<T = unknown> {
  code: number
  msg: string
  data: T
}

// ========== 业务类型 ==========

/** 发送短信验证码返回 */
export interface SmsSendResult {
  expireSeconds?: number
}

/** 登录成功返回 */
export interface LoginResult {
  token: string
  refreshToken?: string
  /** 登录接口可能直接返回用户信息 */
  userInfo?: UserProfile
}

/** 用户资料 */
export interface UserProfile {
  id: number | string
  nickname: string
  avatar: string
  phone: string
  /** 角色标识，如 admin / user */
  role?: string
}

// ========== API 函数 ==========

/**
 * 发送短信验证码
 * POST /api/auth/sms/send
 */
export function sendSmsCode(phone: string): Promise<ApiResponse<SmsSendResult>> {
  // return request.post<ApiResponse<SmsSendResult>>('/api/auth/sms/send', { phone })
  console.log('sendSmsCode', phone)
  return Promise.resolve({ code: 0, msg: 'ok', data: {} })
}

/**
 * 密码登录
 * POST /api/auth/login/password
 */
export function loginByPassword(phone: string, password: string): Promise<ApiResponse<LoginResult>> {
  // return request.post<ApiResponse<LoginResult>>('/api/auth/login/password', { phone, password })
  console.log('loginByPassword', phone, password)
  return Promise.resolve({ code: 0, msg: 'ok', data: { token: '' } })
}

/**
 * 短信验证码登录
 * POST /api/auth/login/sms
 */
export function loginBySms(phone: string, code: string): Promise<ApiResponse<LoginResult>> {
  // return request.post<ApiResponse<LoginResult>>('/api/auth/login/sms', { phone, code })
  console.log('loginBySms', phone, code)
  return Promise.resolve({ code: 0, msg: 'ok', data: { token: '' } })
}

/**
 * 获取当前用户资料
 * GET /api/user/profile
 */
export function getUserProfile(): Promise<ApiResponse<UserProfile>> {
  // return request.get<ApiResponse<UserProfile>>('/api/user/profile')
  return Promise.resolve({ code: 0, msg: 'ok', data: {} as UserProfile })
}
