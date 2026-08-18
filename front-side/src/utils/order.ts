// 订单模块通用工具：状态文案、倒计时、手机号脱敏（订单列表 / 详情 / 支付页复用）

/** 订单状态文案 */
export const ORDER_STATUS_TEXT: Record<number, string> = {
  0: '待付款',
  1: '待发货',
  2: '待收货',
  3: '已完成',
  4: '已取消',
  5: '退款/售后',
}

/** 取消类型文案（订单详情展示，如有） */
export const CANCEL_TYPE_TEXT: Record<number, string> = {
  1: '用户取消',
  2: '超时未支付自动取消',
}

/**
 * 解析后端时间字符串为毫秒时间戳
 * 兼容 "2026-08-19 12:00:00" 与 "2026-08-19T12:00:00"
 * （iOS Safari 不支持带 - 的日期字符串，统一替换为 /）
 */
export function parseTime(time: string): number {
  if (!time) return NaN
  return new Date(time.replace('T', ' ').replace(/-/g, '/')).getTime()
}

/**
 * 待付款倒计时文案
 * 剩余时间 > 0：mm:ss（超过 1 小时为 hh:mm:ss）；已超时 / 解析失败：00:00
 */
export function formatCountdown(deadline: string, now: number): string {
  const diff = parseTime(deadline) - now
  if (Number.isNaN(diff) || diff <= 0) return '00:00'
  const totalSec = Math.floor(diff / 1000)
  const h = Math.floor(totalSec / 3600)
  const m = Math.floor((totalSec % 3600) / 60)
  const s = totalSec % 60
  const mm = String(m).padStart(2, '0')
  const ss = String(s).padStart(2, '0')
  return h > 0 ? `${String(h).padStart(2, '0')}:${mm}:${ss}` : `${mm}:${ss}`
}

/** 手机号脱敏：13812345678 → 138****5678 */
export function maskPhone(phone: string): string {
  if (!phone || phone.length < 7) return phone
  return phone.slice(0, 3) + '****' + phone.slice(-4)
}

/** Date → "yyyy-MM-dd HH:mm:ss"（支付页倒计时兜底值用） */
export function formatDateTime(d: Date): string {
  const pad = (n: number): string => String(n).padStart(2, '0')
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ` +
    `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  )
}
