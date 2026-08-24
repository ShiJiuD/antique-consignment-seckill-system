<template>
  <div class="mine-page">
    <div class="header">
      <h2>古玩寄卖</h2>
    </div>
    <div class="mine-page__header">
      <div class="mine-page__avatar">👤</div>
      <div class="mine-page__header-info">
        <h3 class="mine-page__name">{{ username }}</h3>
        <p class="mine-page__phone">{{ phone }}</p>
        <p>积分：{{newPoints ?? 0}} | 已签到 {{ continueSignDays ?? 0 }} 天</p>
      </div>
    </div>

    <!-- 我的订单入口（位于签到卡片上方） -->
    <div class="order-entry" @click="goOrderList">
      <span class="order-entry__left">
        <span class="order-entry__icon">🧾</span>
        <span class="order-entry__label">我的订单</span>
      </span>
      <span class="order-entry__arrow">›</span>
    </div>

    <!-- ==================== 签到卡片（位于「我的收藏」上方） ==================== -->
    <div class="sign-card" @click="handleSign">
      <div class="sign-card__header">
        <div class="sign-card__title">
          <span class="sign-card__icon">🎁</span>
          <span>每日签到</span>
        </div>
        <!--
          已签到按钮：置灰 + 逻辑禁用（不使用原生 disabled），
          保证点击卡片任意位置（含按钮区域）都能触发「今日已签到」提示
        -->
        <button
          type="button"
          class="sign-card__btn"
          :class="{ 'sign-card__btn--signed': isSignedToday }"
          @click.stop="handleSign"
        >
          {{ isSignedToday ? '已签到' : '签到' }}
        </button>
      </div>
      <p class="sign-card__tip">签到领积分，连续签到奖励更多</p>
      <!-- 连续签到 7 天奖励进度：已达成的天数渲染 ✓，未达成展示奖励积分 -->
      <div class="sign-card__week">
        <div
          v-for="(reward, index) in weekRewards"
          :key="reward"
          class="sign-card__day"
        >
          <span class="sign-card__day-circle">
            {{ isSignedDay(index) ? '✓' : '+' + reward }}
          </span>
        </div>
      </div>
    </div>

    <div class="mine-page__menu">
      <div
        class="mine-page__item"
        v-for="item in menuList"
        :key="item.key"
        @click="handleMenu(item.key)"
      >
        <span class="mine-page__item-left">
          <span class="mine-page__item-icon">{{ item.icon }}</span>
          {{ item.label }}
        </span>
        <span class="mine-page__arrow">›</span>
      </div>
    </div>

    <button class="mine-page__logout" @click="handleLogout">退出登录</button>

    <!-- 签到成功弹窗（关闭后重新拉取签到状态） -->
    <transition name="popup-fade">
      <SignInPopup
        v-if="showSignPopup"
        :today-reward="signResult.todayReward"
        :new-points="signResult.newPoints"
        :continuous-days="popupContinuousDays"
        @close="handlePopupClose"
      />
    </transition>

    <!-- 手写轻提示（无 UI 组件库） -->
    <transition name="toast-fade">
      <div v-if="toastVisible" class="sign-toast">{{ toastText }}</div>
    </transition>

    <!-- 底部 Tab 栏 -->
    <div class="tab-bar">
      <div
        v-for="tab in tabList"
        :key="tab.key"
        class="tab-bar__item"
        :class="{ active: 'mine' === tab.key }"
        @click="handleTabChange(tab.key)"
      >
        <span class="tab-bar__icon">{{ tab.icon }}</span>
        <span class="tab-bar__label">{{ tab.label }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import SignInPopup from '@/components/SignInPopup/index.vue'

const continueSignDays = ref(0)
const newPoints = ref(0)

const router = useRouter()

const username = ref(localStorage.getItem('nickname') || '古玩爱好者')
const phone = ref(localStorage.getItem('phone') || '')

const menuList = [
  { key: 'favorites', label: '我的收藏', icon: '❤️' },
  { key: 'history', label: '浏览记录', icon: '🕐' },
  { key: 'sign', label: '签到有礼', icon: '📦' },
  { key: 'AI', label: 'AI智能问答', icon: 'AI' },
  { key: 'orders', label: '藏品对比', icon: '比' },
  { key: 'orders', label: '价格洞察', icon: '价' },
]

type TabKey = 'home' | 'discover' | 'message' | 'mine'
const tabList = [
  { key: 'home' as TabKey, label: '首页', icon: '🏠' },
  { key: 'discover' as TabKey, label: '发现', icon: '🔍' },
  { key: 'message' as TabKey, label: '消息', icon: '💬' },
  { key: 'mine' as TabKey, label: '我的', icon: '👤' },
]

const TAB_ROUTES: Record<TabKey, string> = {
  home: '/home',
  discover: '/discover',
  message: '/message',
  mine: '/mine',
}

function handleTabChange(key: TabKey) {
  router.push(TAB_ROUTES[key])
}

/** 菜单点击：收藏 / 浏览记录跳转对应列表页，其余菜单保留原逻辑不动 */
function handleMenu(key: string) {
  console.log('点击菜单:', key)
  if (key === 'favorites') {
    router.push('/favorite')
    return
  }
  if (key === 'history') {
    router.push('/history')
    return
  }
}

/** 我的订单入口：跳转订单列表页 */
function goOrderList() {
  router.push('/order/list')
}

function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('nickname')
  localStorage.removeItem('phone')
  router.push('/login')
}

// ==================== axios 实例（登录鉴权） ====================

/** 签到模块专用 axios 实例 */
const http = axios.create({
  baseURL: 'http://localhost:8080', // 后端服务地址
  timeout: 10000
})

// 请求拦截器：所有接口统一携带登录鉴权头 Authorization: Bearer ${token}
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ==================== TS 类型定义（严格约束，禁止 any） ====================

/** 统一响应结构：code = 1 业务成功，code = 0 业务失败 */
interface ApiResponse<T> {
  code: number
  msg: string
  data: T
}

/** 用户基本信息（后端 userInfo 字段） */
interface UserInfo {
  nickname: string
  avatar: string
  points: number
  signInDays: number
}

/** 今日签到信息（后端 signInfo 字段） */
interface SignInfo {
  /** 今日是否已签到 */
  isSignedToday: boolean
  /** 连续签到天数 */
  continuousDays: number
  /** 今日签到奖励积分 */
  todayReward: number
}

/** 个人中心信息：GET /api/user/center/info 返回的 data */
interface UserCenterInfo {
  userInfo: UserInfo
  signInfo: SignInfo
  /** 收藏总数 */
  collectionCount: number
  /** 浏览记录总数 */
  historyCount: number
}

/** 签到结果：POST /api/user/sign 返回的 data */
interface SignResult {
  /** 今日签到获得的积分 */
  todayReward: number
  /** 签到后的最新总积分 */
  newPoints: number
}

// ==================== 签到状态 ====================

/** 今日签到信息（来自 GET /api/user/center/info） */
const signInfo = ref<SignInfo | null>(null)

/** 是否已签到（由接口数据派生，非本地假数据） */
const isSignedToday = computed<boolean>(() => signInfo.value?.isSignedToday ?? false)

/** 是否正在提交签到请求（防止重复点击） */
const signing = ref(false)

/** 签到成功弹窗显隐 */
const showSignPopup = ref(false)

/** 签到成功弹窗展示数据 */
const signResult = ref<SignResult>({ todayReward: 0, newPoints: 0 })

/** 传入弹窗的连续签到天数（页面获取值 + 今日签到 1 天） */
const popupContinuousDays = ref(0)

/** 连续签到 7 天奖励积分表 */
const weekRewards: readonly number[] = [2, 4, 6, 8, 10, 12, 14]

/** 第 index + 1 天是否已签到（连续天数覆盖到即视为已签到） */
function isSignedDay(index: number): boolean {
  const days = signInfo.value?.continuousDays ?? 0
  return days >= index + 1
}

// ==================== 手写轻提示 ====================

const toastText = ref('')
const toastVisible = ref(false)
let toastTimer: number | undefined

/** 弹出轻提示，2 秒后自动消失 */
function showToast(msg: string): void {
  toastText.value = msg
  toastVisible.value = true
  window.clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => {
    toastVisible.value = false
  }, 2000)
}

// ==================== 接口请求 ====================

/**
 * 获取个人中心信息，刷新页面签到状态
 * GET /api/user/center/info（依赖请求头 Token 鉴权）
 */
async function fetchCenterInfo(): Promise<void> {
  try {
    const res = await http.get<ApiResponse<UserCenterInfo>>('/api/user/center/info')
    if (res.data.code === 1) {
      // 业务成功：写入签到信息
      signInfo.value = res.data.data.signInfo
      // 后端真实字段在 userInfo 下：points（文档误写为 newPoints）、signInDays
      newPoints.value = res.data.data.userInfo.points
      continueSignDays.value = res.data.data.userInfo.signInDays
    } else {
      // 业务失败：展示后端 msg
      showToast(res.data.msg || '获取签到信息失败')
    }
  } catch (err) {
    // 网络异常 / 超时 / 鉴权失败
    console.error('获取个人中心信息失败：', err)
    showToast('网络异常，请稍后重试')
  }
}

/**
 * 点击签到卡片
 * 已签到：仅提示，不发起请求；未签到：调用 POST /api/user/sign
 */
async function handleSign(): Promise<void> {
  // 签到数据尚未加载完成时不处理
  if (!signInfo.value) return

  // 今日已签到：只提示，不发请求
  if (signInfo.value.isSignedToday) {
    showToast('今日已签到')
    return
  }

  // 防止重复提交
  if (signing.value) return
  signing.value = true
  try {
    const res = await http.post<ApiResponse<SignResult>>('/api/user/sign')
    if (res.data.code === 1) {
      // 签到成功：组装弹窗数据并打开
      signResult.value = res.data.data
      // 页面获取的 continuousDays 是签到前的值，今日签到成功后 +1
      popupContinuousDays.value = signInfo.value.continuousDays + 1
      showSignPopup.value = true
    } else {
      // 业务失败：只展示后端 msg，不打开弹窗
      showToast(res.data.msg || '签到失败')
    }
  } catch (err) {
    // 网络异常：提示但不打开弹窗
    console.error('签到请求失败：', err)
    showToast('网络异常，请稍后重试')
  } finally {
    signing.value = false
  }
}

/** 签到成功弹窗关闭：重新拉取个人中心信息，刷新签到状态 */
function handlePopupClose(): void {
  showSignPopup.value = false
  void fetchCenterInfo()
}

// 页面加载完成即拉取签到状态
onMounted(() => {
  void fetchCenterInfo()
})
</script>

<style scoped>
.mine-page {
  min-height: 100vh;
  background: #f8f3eb; /* 页面背景色 */
  padding-bottom: 70px;
}

.header {
  padding: 15px;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(160deg, #4a1d0a, #5d2e0c, #7a3d16, #5d2e0c);
}

.header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #d4af37;
}

.mine-page__header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 24px 20px;
  background:  #b5471f;
}

.mine-page__avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  flex-shrink: 0;
}

.mine-page__header-info {
  color: #fff;
}

.mine-page__name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #d4af37;
}

.mine-page__phone {
  margin: 4px 0 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

/* ==================== 我的订单入口（签到卡片上方） ==================== */
.order-entry {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 16px 16px 0;
  padding: 14px 16px;
  background: #fff;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.15s;
}

.order-entry:active {
  background: #faf7f2;
}

.order-entry__left {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #91572c;
}

.order-entry__icon {
  font-size: 18px;
}

.order-entry__arrow {
  color: #ccc;
  font-size: 20px;
}

/* ==================== 签到卡片 ==================== */
.sign-card {
  margin: 16px; /* 卡片外边距 */
  padding: 16px;
  background: #fff; /* 白色背景 */
  border-radius: 12px; /* 圆角 */
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.sign-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 卡片头部左侧：🎁 + 每日签到（主棕色） */
.sign-card__title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 600;
  color: #91572c; /* 主棕色 */
}

.sign-card__icon {
  font-size: 20px;
  line-height: 1;
}

/* 右侧签到按钮（橙色） */
.sign-card__btn {
  padding: 6px 18px;
  border: none;
  border-radius: 16px;
  background: #c46b2b; /* 按钮橙色 */
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
}

.sign-card__btn:active {
  opacity: 0.85;
}

/* 已签到：置灰禁用（逻辑禁用，点击由卡片统一提示「今日已签到」） */
.sign-card__btn--signed {
  background: #c8c8c8;
  cursor: not-allowed;
}

/* 卡片下方灰色小字 */
.sign-card__tip {
  margin: 10px 0 0;
  font-size: 12px;
  color: #999;
}

/* 底部横向均分 7 个圆形 */
.sign-card__week {
  display: flex;
  justify-content: space-between;
  margin-top: 14px;
}

.sign-card__day {
  flex: 1;
  display: flex;
  justify-content: center;
}

.sign-card__day-circle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #f5efe6;
  color: #91572c; /* 主棕色 */
  font-size: 12px;
  font-weight: 600;
}

/* 已签到圆圈：棕色底 + 白色 ✓ */
.sign-card__day--signed .sign-card__day-circle {
  background: #91572c;
  color: #fff;
  font-size: 16px;
}

/* ==================== 手写轻提示 ==================== */
.sign-toast {
  position: fixed;
  left: 50%;
  top: 45%;
  transform: translate(-50%, -50%);
  max-width: 70vw;
  padding: 10px 20px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  font-size: 14px;
  text-align: center;
  z-index: 2000;
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: opacity 0.25s;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
}

/* 签到成功弹窗过渡动画（transition 包裹在组件外，类名作用于组件根元素） */
.popup-fade-enter-active,
.popup-fade-leave-active {
  transition: opacity 0.25s ease;
}

.popup-fade-enter-from,
.popup-fade-leave-to {
  opacity: 0;
}

.mine-page__menu {
  margin-top: 10px;
  background: #fff;
}

.mine-page__item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;
  font-size: 15px;
  color: #333;
  transition: background 0.15s;
}

.mine-page__item:active {
  background: #fafafa;
}

.mine-page__item-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.mine-page__item-icon {
  font-size: 18px;
}

.mine-page__arrow {
  color: #ccc;
  font-size: 20px;
}

.mine-page__logout {
  display: block;
  width: calc(100% - 32px);
  margin: 30px auto;
  padding: 14px 0;
  border: 1px solid #e0d0c0;
  border-radius: 10px;
  background: #fff;
  color: #c0392b;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}

.mine-page__logout:active {
  background: #fef5f5;
}

/* ========== Tab 栏 ========== */
.tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-around;
  align-items: center;
  height: 56px;
  background: #fff;
  border-top: 1px solid #eee;
  z-index: 100;
  padding-bottom: env(safe-area-inset-bottom);
}

.tab-bar__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  cursor: pointer;
  color: #999;
  transition: color 0.2s;
}

.tab-bar__item.active {
  color: #8b4513;
}

.tab-bar__icon {
  font-size: 22px;
  line-height: 1;
}

.tab-bar__label {
  font-size: 11px;
  font-weight: 500;
}
</style>
