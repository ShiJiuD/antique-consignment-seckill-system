import request from '@/utils/request'

// ==================== TS 类型定义 ====================
// 说明：全局接口统一返回 { code, msg, data }，code === 1 代表成功
// 以下类型均与后端返回的 data 字段严格匹配，全部字段小驼峰

/** 用户基本信息（后端 userInfo 字段） */
export interface UserInfo {
  nickname: string
  avatar: string
  /** 当前总积分 */
  points: number
  /** 累计签到天数 */
  signInDays: number
}

/** 今日签到信息（后端 signInfo 字段） */
export interface SignInfo {
  /** 今日是否已签到 */
  isSignedToday: boolean
  /** 连续签到天数 */
  continuousDays: number
  /** 今日签到奖励积分 */
  todayReward: number
}

/** 个人中心信息（GET /api/user/center/info 返回的 data） */
export interface UserCenterInfo {
  userInfo: UserInfo
  signInfo: SignInfo
  /** 收藏总数 */
  collectionCount: number
}

/**
 * 签到结果（POST /api/user/sign 返回的 data）
 * 用于签到成功弹窗展示
 */
export interface SignResult {
  /** 本次签到获得的积分 */
  rewardPoints: number
  /** 签到后的连续签到天数 */
  continuousDays: number
  /** 距离额外奖励还需要的天数（0 表示已获得额外奖励） */
  daysToExtraReward: number
  /** 签到后的总积分 */
  totalPoints: number
}

/** 业务接口错误：后端返回 code === 0 时抛出，message 为后端 msg 提示文本 */
export class ApiError extends Error {
  constructor(msg: string) {
    super(msg)
    this.name = 'ApiError'
  }
}

// ==================== API 函数 ====================

/**
 * 获取个人中心信息（含签到数据）
 * GET /api/user/center/info
 * 无请求参数，依靠请求头 Token 识别登录用户
 *
 * @returns data 业务数据；code !== 1 时抛出 ApiError（携带后端 msg）
 */
export async function getUserCenterInfo(): Promise<UserCenterInfo> {
  const res = await request.get('/api/user/center/info')
  // 判断 code === 1 再接收数据
  if (res.code !== 1) {
    throw new ApiError(res.msg || '获取个人信息失败')
  }
  return res.data
}

/**
 * 执行每日签到
 * POST /api/user/sign
 * 无需传参；当日重复签到后端返回 code === 0 并携带错误提示
 *
 * @returns data 签到结果；code !== 1 时抛出 ApiError（携带后端 msg）
 */
export async function userSign(): Promise<SignResult> {
  const res = await request.post('/api/user/sign')
  // 判断 code === 1 再接收数据
  if (res.code !== 1) {
    throw new ApiError(res.msg || '签到失败')
  }
  return res.data
}
