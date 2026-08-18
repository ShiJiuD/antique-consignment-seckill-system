<template>
  <div class="pay-page">
    <!-- 顶部导航栏 -->
    <div class="pay-page__bar">
      <span class="pay-page__back" @click="goBack">←</span>
      <span class="pay-page__title">模拟支付</span>
      <span class="pay-page__placeholder"></span>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="state-box">
      <span class="state-box__spinner"></span>
      <p class="state-box__text">加载中...</p>
    </div>

    <!-- 加载异常 -->
    <div v-else-if="loadError" class="state-box">
      <span class="state-box__icon">😥</span>
      <p class="state-box__text">加载失败，请检查网络后重试</p>
      <button type="button" class="state-box__btn" @click="fetchOrder">
        重新加载
      </button>
    </div>

    <!-- 支付主体 -->
    <template v-else-if="order">
      <!-- 订单基础信息 -->
      <div class="pay-card">
        <h4 class="pay-card__title">订单信息</h4>
        <p class="pay-card__row">订单号：{{ order.orderNo }}</p>
        <p class="pay-card__row">藏品名称：{{ firstItem?.title ?? '—' }}</p>
        <p class="pay-card__row">
          订单状态：{{ ORDER_STATUS_TEXT[order.status] ?? '未知状态' }}
        </p>
        <p class="pay-card__row">
          支付金额：<span class="pay-card__amount">¥{{ order.totalAmount.toLocaleString() }}</span>
        </p>
      </div>

      <!-- 支付倒计时（payDeadline，红色醒目；仅待付款展示） -->
      <div v-if="order.status === 0" class="pay-countdown">
        <p class="pay-countdown__label">
          {{ timeout ? '订单已超时关闭' : '请在截止时间前完成支付' }}
        </p>
        <p class="pay-countdown__time">{{ remainText }}</p>
      </div>

      <!-- 立即模拟支付（倒计时结束置灰不可点击） -->
      <button
        type="button"
        class="pay-page__pay-btn"
        :class="{
          'pay-page__pay-btn--disabled': timeout || paying || order.status !== 0,
        }"
        @click="handlePay"
      >
        {{
          paying ? '支付中...' : order.status === 0 ? '立即模拟支付' : '订单已支付'
        }}
      </button>
    </template>

    <!-- 支付成功弹窗：确认后跳转订单详情页 -->
    <transition name="pay-success-fade">
      <div v-if="paySuccessVisible" class="pay-success-mask">
        <div class="pay-success">
          <span class="pay-success__icon">🎉</span>
          <h4 class="pay-success__title">支付成功</h4>
          <p class="pay-success__text">恭喜您，订单支付成功！</p>
          <button type="button" class="pay-success__btn" @click="handlePaySuccessConfirm">
            查看订单
          </button>
        </div>
      </div>
    </transition>

    <!-- 手写轻提示 -->
    <transition name="toast-fade">
      <div v-if="toastVisible" class="page-toast">{{ toastText }}</div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail, payOrder, type Order } from '@/api/order'
import { ORDER_STATUS_TEXT, formatCountdown, formatDateTime, parseTime } from '@/utils/order'

const route = useRoute()
const router = useRouter()

// ==================== 路由参数 ====================

/** 订单ID（创建订单成功后跳转携带） */
const orderId = Number(route.query.orderId) || 0
/** 支付截止时间（创建订单成功后跳转携带，倒计时依据） */
const payDeadline = ref(typeof route.query.payDeadline === 'string' ? route.query.payDeadline : '')

// ==================== 状态 ====================

/** 订单信息（GET /api/order/{id} 获取基础信息展示） */
const order = ref<Order | null>(null)
/** 加载中 */
const loading = ref(false)
/** 加载异常 */
const loadError = ref(false)
/** 支付中（防止重复点击） */
const paying = ref(false)
/** 倒计时是否结束（结束置灰支付按钮） */
const timeout = ref(false)
/** 倒计时文案 */
const remainText = ref('--:--')
/** 支付成功弹窗显隐 */
const paySuccessVisible = ref(false)

/** 订单首个藏品 */
const firstItem = computed(() => order.value?.items[0] ?? null)

// ==================== 倒计时 ====================

let tickTimer: number | undefined

/** 每秒刷新倒计时，剩余时间为 0 时标记超时并停止 */
function tick(): void {
  if (!payDeadline.value) return
  remainText.value = formatCountdown(payDeadline.value, Date.now())
  if (parseTime(payDeadline.value) - Date.now() <= 0) {
    timeout.value = true
    stopTick()
    showToast('订单已超时关闭')
  }
}

function startTick(): void {
  stopTick()
  tick() // 先立即执行一次，避免首屏空白
  tickTimer = window.setInterval(tick, 1000)
}

function stopTick(): void {
  window.clearInterval(tickTimer)
  tickTimer = undefined
}

// ==================== 手写轻提示 ====================

const toastText = ref('')
const toastVisible = ref(false)
let toastTimer: number | undefined

function showToast(msg: string): void {
  toastText.value = msg
  toastVisible.value = true
  window.clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => {
    toastVisible.value = false
  }, 2000)
}

// ==================== 接口请求 ====================

/** 获取订单基础信息 */
async function fetchOrder(): Promise<void> {
  if (!orderId) {
    loadError.value = true
    return
  }
  loading.value = true
  loadError.value = false
  try {
    const res = await getOrderDetail(orderId)
    if (res.code === 1) {
      order.value = res.data
      // 路由未携带截止时间时，取订单详情里的 payDeadline
      if (!payDeadline.value && res.data.payDeadline) {
        payDeadline.value = res.data.payDeadline
      }
    } else {
      showToast(res.msg || '获取订单信息失败')
      loadError.value = true
    }
  } catch (err) {
    console.error('获取订单信息失败：', err)
    loadError.value = true
  } finally {
    loading.value = false
    // 截止时间兜底：缺失或解析失败时按 16 分钟倒计时
    if (!payDeadline.value || Number.isNaN(parseTime(payDeadline.value))) {
      payDeadline.value = formatDateTime(new Date(Date.now() + 16 * 60 * 1000))
    }
    // 仅待付款订单启动倒计时
    if (order.value && order.value.status === 0) {
      startTick()
    }
  }
}

// ==================== 交互 ====================

/**
 * 立即模拟支付
 * 成功：弹出支付成功弹窗，确认后跳转订单详情页
 * 失败：展示后端 msg
 */
async function handlePay(): Promise<void> {
  if (paying.value || timeout.value) return
  // 非待付款订单不可支付
  if (order.value && order.value.status !== 0) return
  paying.value = true
  try {
    const res = await payOrder(orderId)
    if (res.code === 1) {
      stopTick() // 支付成功，停止倒计时
      paySuccessVisible.value = true
    } else {
      showToast(res.msg || '支付失败')
    }
  } catch (err) {
    console.error('支付失败：', err)
    showToast('网络异常，请稍后重试')
  } finally {
    paying.value = false
  }
}

/** 支付成功弹窗确认：跳转订单详情页 */
function handlePaySuccessConfirm(): void {
  paySuccessVisible.value = false
  router.replace(`/order/detail/${orderId}`)
}

/** 返回上一页 */
function goBack() {
  router.back()
}

// ==================== 生命周期 ====================

onMounted(() => {
  void fetchOrder()
})

onBeforeUnmount(() => {
  stopTick() // 离开页面停止定时器
})
</script>

<style scoped>
/* ==================== 页面容器 ==================== */
.pay-page {
  min-height: 100vh;
  background: #f8f3eb; /* 米白底色 */
}

/* ==================== 顶部导航栏 ==================== */
.pay-page__bar {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.pay-page__back {
  font-size: 20px;
  cursor: pointer;
  color: #333;
}

.pay-page__title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.pay-page__placeholder {
  width: 20px;
}

/* ==================== 订单信息卡片 ==================== */
.pay-card {
  margin: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.pay-card__title {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 700;
  color: #91572c; /* 主棕色 */
}

.pay-card__row {
  margin: 0 0 8px;
  font-size: 14px;
  color: #666;
}

.pay-card__row:last-child {
  margin-bottom: 0;
}

.pay-card__amount {
  font-size: 18px;
  font-weight: 700;
  color: #c0392b; /* 金额红色 */
}

/* ==================== 支付倒计时（红色醒目） ==================== */
.pay-countdown {
  margin: 0 16px;
  padding: 18px 16px;
  background: #fff;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.pay-countdown__label {
  margin: 0 0 8px;
  font-size: 13px;
  color: #999;
}

.pay-countdown__time {
  margin: 0;
  font-size: 30px;
  font-weight: 700;
  color: #e02020; /* 倒计时红色 */
  letter-spacing: 2px;
  font-variant-numeric: tabular-nums; /* 数字等宽，避免跳动 */
}

/* ==================== 支付按钮 ==================== */
.pay-page__pay-btn {
  display: block;
  width: calc(100% - 32px);
  margin: 20px 16px;
  padding: 14px 0;
  border: none;
  border-radius: 24px;
  background: #c46b2b; /* 按钮橙色（主按钮实色） */
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.pay-page__pay-btn:active {
  opacity: 0.85;
}

/* 倒计时结束 / 非待付款：置灰不可点击 */
.pay-page__pay-btn--disabled {
  background: #c8c8c8;
  cursor: not-allowed;
}

/* ==================== 支付成功弹窗 ==================== */
.pay-success-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6); /* 半透明遮罩 */
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.pay-success {
  width: 78vw;
  max-width: 320px;
  box-sizing: border-box;
  padding: 28px 24px 22px;
  background: #fff;
  border-radius: 16px;
  text-align: center;
}

.pay-success__icon {
  font-size: 44px;
  line-height: 1;
}

.pay-success__title {
  margin: 10px 0 6px;
  font-size: 20px;
  font-weight: 700;
  color: #91572c; /* 主棕色 */
}

.pay-success__text {
  margin: 0 0 20px;
  font-size: 14px;
  color: #666;
}

.pay-success__btn {
  width: 100%;
  padding: 12px 0;
  border: none;
  border-radius: 22px;
  background: #c46b2b; /* 按钮橙色 */
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.pay-success__btn:active {
  opacity: 0.85;
}

.pay-success-fade-enter-active,
.pay-success-fade-leave-active {
  transition: opacity 0.25s ease;
}

.pay-success-fade-enter-from,
.pay-success-fade-leave-to {
  opacity: 0;
}

/* ==================== 加载中 / 异常 ==================== */
.state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 80px 16px;
}

.state-box__icon {
  font-size: 44px;
}

.state-box__text {
  margin: 0;
  font-size: 14px;
  color: #999;
}

.state-box__spinner {
  width: 28px;
  height: 28px;
  border: 3px solid #f0e6d3;
  border-top-color: #c46b2b;
  border-radius: 50%;
  animation: pay-spin 0.8s linear infinite;
}

@keyframes pay-spin {
  to {
    transform: rotate(360deg);
  }
}

.state-box__btn {
  padding: 8px 28px;
  border: none;
  border-radius: 18px;
  background: #c46b2b;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.state-box__btn:active {
  opacity: 0.85;
}

/* ==================== 轻提示 ==================== */
.page-toast {
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
</style>
