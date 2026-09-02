import { ref, computed, shallowRef } from 'vue'
import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import { getMessageList, getUnreadCount, readMessage } from '@/api/message'
import type { MessageItem, WsPushMessage } from '@/types/message'
import request from '@/utils/request'

/** 重连基础间隔（ms），指数退避：1s → 2s → 4s ... */
const RECONNECT_BASE_DELAY = 1000
/** 重连最大间隔（ms） */
const RECONNECT_MAX_DELAY = 30_000

/**
 * 拼接 WebSocket 地址：ws://{host}:{port}/api/ws
 * 与 axios baseURL（http://localhost:8080）保持一致，http 替换为 ws
 */
function buildWsUrl(): string {
  const base = (request.defaults.baseURL || 'http://localhost:8080').replace(/^http/, 'ws')
  return `${base}/api/ws`
}

/**
 * 消息模块 Pinia 仓库
 * 统一维护：消息数组、未读数量、ws 实例对象
 * 其他组件可直接从 messageStore 取 unreadCount 做全局角标
 */
export const useMessageStore = defineStore('message', () => {
  // ==================== 状态 ====================

  /** 消息列表（最新在最上方） */
  const messageList = ref<MessageItem[]>([])
  /** 消息总数 */
  const total = ref(0)
  /** 当前页码（加载下一页时使用） */
  const page = ref(1)
  /** 每页条数 */
  const size = ref(10)
  /** 未读消息总数（全局角标数据源） */
  const unreadCount = ref(0)
  /** 列表加载中标记（防止重复请求） */
  const loading = ref(false)
  /**
   * ws 实例对象
   * 使用 shallowRef 避免 Vue 深度代理 WebSocket 原生实例
   */
  const ws = shallowRef<WebSocket | null>(null)

  // ==================== 非响应式内部变量（不放入 state，避免无意义渲染） ====================

  /** 重连定时器句柄 */
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  /** 已重连次数（指数退避用） */
  let reconnectAttempts = 0
  /** 是否主动关闭（退出登录），主动关闭不触发重连 */
  let manuallyClosed = false

  // ==================== 计算属性 ====================

  /** 是否还有更多分页数据 */
  const hasMore = computed(() => messageList.value.length < total.value)

  // ==================== WebSocket ====================

  /** 建立 ws 连接（幂等：已连接/连接中直接返回） */
  function connect() {
    const token = localStorage.getItem('token')
    // 未登录不建立连接
    if (!token) return
    // 已连接或连接中，跳过重复连接
    const socket = ws.value
    if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
      return
    }

    manuallyClosed = false
    // 连接地址：ws://{host}:{port}/api/ws?token={token}，token 与请求头 Bearer token 一致
    const instance = new WebSocket(`${buildWsUrl()}?token=${encodeURIComponent(token)}`)
    ws.value = instance

    instance.onopen = () => {
      // 连接成功，重置重连次数
      reconnectAttempts = 0
    }

    instance.onmessage = (event: MessageEvent) => {
      try {
        const push = JSON.parse(event.data) as WsPushMessage
        // V1 只处理服务端推送 type: "newMessage"，其余推送忽略
        if (push?.type !== 'newMessage') return
        const msg = push?.data
        if (!msg?.messageId) return
        // type=3 AI助手消息全部忽略，不处理不渲染
        if (msg.type === 3) return
        handleNewMessage(msg)
      } catch {
        // 推送内容解析失败，忽略即可，不影响页面
      }
    }

    instance.onclose = () => {
      ws.value = null
      // 主动关闭（退出登录）不重连
      if (manuallyClosed) return
      scheduleReconnect()
    }
  }

  /** 断线重连：指数退避 1s → 2s → 4s ...，最大间隔 30s */
  function scheduleReconnect() {
    const delay = Math.min(RECONNECT_MAX_DELAY, RECONNECT_BASE_DELAY * 2 ** reconnectAttempts)
    reconnectAttempts += 1
    if (reconnectTimer) clearTimeout(reconnectTimer)
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      connect()
    }, delay)
  }

  /** 主动关闭 ws 连接（退出登录时调用），关闭后不再重连 */
  function disconnect() {
    manuallyClosed = true
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    reconnectAttempts = 0
    const socket = ws.value
    ws.value = null
    if (socket) {
      socket.close()
    }
  }

  /**
   * 处理 newMessage 推送：
   * ① 重新请求 unread-count 刷新未读角标
   * ② 新消息插入消息列表最顶部（按 messageId 去重）
   */
  function handleNewMessage(msg: MessageItem) {
    fetchUnreadCount()
    const exists = messageList.value.some((m) => m?.messageId === msg?.messageId)
    if (!exists) {
      messageList.value.unshift(msg)
      total.value += 1
    }
  }

  // ==================== HTTP 数据请求 ====================

  /** 拉取消息分页列表；isLoadMore=true 时追加到列表尾部，否则重置为第一页 */
  async function fetchMessageList(isLoadMore = false) {
    if (loading.value) return
    loading.value = true
    try {
      const res = await getMessageList({ page: page.value, size: size.value })
      if (res?.code === 1) {
        const data = res?.data
        // type=3 AI助手消息过滤，不进入渲染列表
        const list = (data?.list ?? []).filter((m) => m?.type !== 3)
        if (isLoadMore) {
          messageList.value.push(...list)
        } else {
          messageList.value = list
        }
        total.value = data?.total ?? 0
        // 记录服务端返回页码，下一页 = 当前页 + 1
        page.value = (data?.page ?? page.value) + 1
      } else {
        ElMessage.error(res?.msg || '获取消息列表失败')
      }
    } catch {
      ElMessage.error('获取消息列表失败')
    } finally {
      loading.value = false
    }
  }

  /** 拉取未读消息总数，更新全局角标 */
  async function fetchUnreadCount() {
    try {
      const res = await getUnreadCount()
      if (res?.code === 1) {
        unreadCount.value = res?.data?.count ?? 0
      }
    } catch {
      // 静默失败：未读角标暂不更新，不影响页面主流程
    }
  }

  /**
   * 单条消息标记已读（点击消息项调用，接口幂等）
   * 乐观更新本地 isRead 状态与未读数，接口失败回滚
   */
  async function markRead(messageId: number) {
    const target = messageList.value.find((m) => m?.messageId === messageId)
    // 消息不存在或已读，无需处理（幂等）
    if (!target || target.isRead === 1) return

    const prevIsRead = target.isRead
    // 乐观更新：先本地置已读
    target.isRead = 1
    if (unreadCount.value > 0) {
      unreadCount.value -= 1
    }

    try {
      const res = await readMessage(messageId)
      if (res?.code !== 1) {
        // 接口失败：回滚
        target.isRead = prevIsRead
        unreadCount.value += 1
        ElMessage.error(res?.msg || '标记已读失败')
      }
    } catch {
      // 请求异常：回滚
      target.isRead = prevIsRead
      unreadCount.value += 1
      ElMessage.error('标记已读失败')
    }
  }

  /** 退出登录时重置仓库：断开 ws、清空数据 */
  function reset() {
    disconnect()
    messageList.value = []
    total.value = 0
    page.value = 1
    unreadCount.value = 0
  }

  return {
    // state
    messageList,
    total,
    page,
    size,
    unreadCount,
    loading,
    ws,
    // getters
    hasMore,
    // actions
    connect,
    disconnect,
    fetchMessageList,
    fetchUnreadCount,
    markRead,
    reset,
  }
})
