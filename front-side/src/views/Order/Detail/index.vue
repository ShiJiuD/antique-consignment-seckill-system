<template>
  <div class="order-detail-page">
    <!-- 顶部导航栏 -->
    <div class="order-detail-page__bar">
      <span class="order-detail-page__back" @click="goBack">←</span>
      <span class="order-detail-page__title">订单详情</span>
      <span class="order-detail-page__placeholder"></span>
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
      <button type="button" class="state-box__btn" @click="fetchDetail">
        重新加载
      </button>
    </div>

    <!-- 空状态（订单不存在） -->
    <div v-else-if="!order" class="state-box">
      <span class="state-box__icon">🧾</span>
      <p class="state-box__text">订单不存在</p>
    </div>

    <!-- 订单信息 -->
    <template v-else>
      <!-- 状态卡片：订单状态、取消类型、订单号、待付款倒计时 -->
      <div class="od-card od-card--status">
        <div class="od-card__status-head">
          <span class="od-card__status" :class="`od-card__status--${order.status}`">
            {{ ORDER_STATUS_TEXT[order.status] ?? '未知状态' }}
          </span>
          <!-- 取消类型（如有） -->
          <span v-if="order.cancelType" class="od-card__cancel-type">
            （{{ CANCEL_TYPE_TEXT[order.cancelType] ?? '已取消' }}）
          </span>
        </div>
        <p class="od-card__row">订单号：{{ order.orderNo }}</p>
        <!-- 待付款：红色倒计时 -->
        <p v-if="order.status === 0" class="od-card__countdown">
          支付剩余 <span>{{ formatCountdown(order.payDeadline, now) }}</span>
        </p>
      </div>

      <!-- 收货信息 -->
      <div class="od-card">
        <h4 class="od-card__title">收货信息</h4>
        <p class="od-card__row">收货人：{{ order.receiverName }}</p>
        <p class="od-card__row">手机号：{{ order.receiverPhone }}</p>
        <p class="od-card__row">收货地址：{{ order.receiverAddress }}</p>
        <p v-if="order.buyerRemark" class="od-card__row">
          买家备注：{{ order.buyerRemark }}
        </p>
      </div>

      <!-- 藏品列表快照：封面、名称、年代、单价、小计 -->
      <div class="od-card">
        <h4 class="od-card__title">藏品信息</h4>
        <div v-for="goods in order.items" :key="goods.id" class="od-goods">
          <img
            class="od-goods__cover"
            :src="goods.coverImage || defaultCover"
            :alt="goods.title"
            @error="handleImgError"
          />
          <div class="od-goods__info">
            <h5 class="od-goods__name">{{ goods.title }}</h5>
            <p class="od-goods__dynasty">年代：{{ goods.dynasty || '—' }}</p>
            <p class="od-goods__price">
              单价 ¥{{ goods.price.toLocaleString() }} × {{ goods.quantity }}
            </p>
          </div>
          <span class="od-goods__subtotal">小计 ¥{{ goods.subtotal.toLocaleString() }}</span>
        </div>
      </div>

      <!-- 金额信息 -->
      <div class="od-card">
        <h4 class="od-card__title">金额信息</h4>
        <p class="od-card__row">
          商品总额：¥{{ order.totalAmount.toLocaleString() }}
        </p>
        <p class="od-card__row od-card__row--pay">
          实付金额：
          <span class="od-card__pay-amount">¥{{ order.payAmount.toLocaleString() }}</span>
        </p>
      </div>

      <!-- 时间节点（有值才展示） -->
      <div class="od-card">
        <h4 class="od-card__title">时间节点</h4>
        <p v-if="order.createTime" class="od-card__row">创建时间：{{ order.createTime }}</p>
        <p v-if="order.payTime" class="od-card__row">支付时间：{{ order.payTime }}</p>
        <p v-if="order.shipTime" class="od-card__row">发货时间：{{ order.shipTime }}</p>
        <p v-if="order.receiveTime" class="od-card__row">
          确认收货时间：{{ order.receiveTime }}
        </p>
        <p v-if="order.cancelTime" class="od-card__row">取消时间：{{ order.cancelTime }}</p>
      </div>

      <!-- 底部操作栏占位 -->
      <div class="order-detail-page__padding"></div>
    </template>

    <!-- 底部操作按钮（与订单列表页逻辑保持一致） -->
    <div v-if="order && hasActions(order.status)" class="order-detail-page__actions">
      <!-- 0 待付款：取消订单 / 去支付 -->
      <template v-if="order.status === 0">
        <button type="button" class="obtn obtn--danger" @click="handleCancelClick">
          取消订单
        </button>
        <button type="button" class="obtn obtn--primary" @click="goPay">去支付</button>
      </template>
      <!-- 1 待发货：修改地址 / 催发货 -->
      <template v-else-if="order.status === 1">
        <button type="button" class="obtn obtn--plain" @click="handleAddressClick">
          修改地址
        </button>
        <button type="button" class="obtn obtn--plain" @click="handleUrge">催发货</button>
      </template>
      <!-- 2 待收货：确认收货 -->
      <template v-else-if="order.status === 2">
        <button type="button" class="obtn obtn--primary" @click="handleReceiveClick">
          确认收货
        </button>
      </template>
    </div>

    <!-- 取消订单 / 确认收货 二次确认弹窗（复用一个，按模式区分） -->
    <ConfirmDialog
      v-model:visible="confirmVisible"
      :title="confirmMode === 'cancel' ? '取消订单' : '确认收货'"
      :content="confirmContent"
      :confirm-text="confirmMode === 'cancel' ? '取消订单' : '确认收货'"
      @confirm="handleConfirmAction"
    />

    <!-- 修改收货地址弹窗 -->
    <AddressEditPopup
      v-model:visible="addressVisible"
      :order-id="order?.id ?? 0"
      :receiver-name="order?.receiverName ?? ''"
      :receiver-phone="order?.receiverPhone ?? ''"
      :receiver-address="order?.receiverAddress ?? ''"
      @success="handleAddressSuccess"
    />

    <!-- 手写轻提示 -->
    <transition name="toast-fade">
      <div v-if="toastVisible" class="page-toast">{{ toastText }}</div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ConfirmDialog from '@/components/ConfirmDialog/index.vue'
import AddressEditPopup from '@/components/AddressEditPopup/index.vue'
import {
  cancelOrder,
  confirmReceive,
  getOrderDetail,
  urgeShip,
  type Order,
} from '@/api/order'
import { CANCEL_TYPE_TEXT, ORDER_STATUS_TEXT, formatCountdown } from '@/utils/order'

const route = useRoute()
const router = useRouter()

// ==================== 状态 ====================

/** 路由订单ID */
const orderId = Number(route.params.id) || 0
/** 订单完整信息 */
const order = ref<Order | null>(null)
/** 加载中 */
const loading = ref(false)
/** 加载异常 */
const loadError = ref(false)

/** 当前时间戳（每秒刷新，驱动待付款倒计时） */
const now = ref(Date.now())
let tickTimer: number | undefined

/** 二次确认弹窗模式：取消订单 / 确认收货 */
type ConfirmMode = 'cancel' | 'receive'
const confirmMode = ref<ConfirmMode>('cancel')
const confirmVisible = ref(false)
/** 修改地址弹窗显隐 */
const addressVisible = ref(false)

const defaultCover = 'https://via.placeholder.com/300x300/f5f0eb/8b4513?text=古玩藏品'

/** 二次确认弹窗文案 */
const confirmContent = computed<string>(() => {
  if (confirmMode.value === 'cancel') {
    return `确定取消订单「${order.value?.orderNo ?? ''}」吗？`
  }
  return '确认已收到藏品？确认后订单将完成。'
})

// ==================== 工具函数 ====================

/** 该状态是否渲染操作按钮（3/4/5 无操作按钮） */
function hasActions(status: number): boolean {
  return status === 0 || status === 1 || status === 2
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

/** 获取订单详情 GET /api/order/{id} */
async function fetchDetail(): Promise<void> {
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
    } else {
      showToast(res.msg || '获取订单详情失败')
      loadError.value = true
    }
  } catch (err) {
    console.error('获取订单详情失败：', err)
    loadError.value = true
  } finally {
    loading.value = false
  }
}

// ==================== 交互 ====================

function goBack() {
  router.back()
}

/** 去支付：跳转模拟支付页（携带 orderId、payDeadline） */
function goPay() {
  router.push({
    path: '/order/pay',
    query: {
      orderId: String(orderId),
      payDeadline: order.value?.payDeadline ?? '',
    },
  })
}

/** 取消订单：打开二次确认弹窗 */
function handleCancelClick() {
  confirmMode.value = 'cancel'
  confirmVisible.value = true
}

/** 确认收货：打开二次确认弹窗 */
function handleReceiveClick() {
  confirmMode.value = 'receive'
  confirmVisible.value = true
}

/** 二次确认弹窗回调：按模式执行取消 / 确认收货 */
async function handleConfirmAction(): Promise<void> {
  if (confirmMode.value === 'cancel') {
    await doCancel()
  } else {
    await doReceive()
  }
}

/** 取消订单：成功后重新拉取详情 */
async function doCancel(): Promise<void> {
  try {
    const res = await cancelOrder(orderId)
    if (res.code === 1) {
      showToast('订单已取消')
      void fetchDetail()
    } else {
      showToast(res.msg || '取消失败，请稍后重试')
    }
  } catch (err) {
    console.error('取消订单失败：', err)
    showToast('网络异常，请稍后重试')
  }
}

/** 确认收货：成功后重新拉取详情 */
async function doReceive(): Promise<void> {
  try {
    const res = await confirmReceive(orderId)
    if (res.code === 1) {
      showToast('确认收货成功')
      void fetchDetail()
    } else {
      showToast(res.msg || '操作失败，请稍后重试')
    }
  } catch (err) {
    console.error('确认收货失败：', err)
    showToast('网络异常，请稍后重试')
  }
}

/** 催发货：直接调用接口，成功提示 */
async function handleUrge(): Promise<void> {
  try {
    const res = await urgeShip(orderId)
    if (res.code === 1) {
      showToast('已通知卖家发货')
    } else {
      showToast(res.msg || '操作失败，请稍后重试')
    }
  } catch (err) {
    console.error('催发货失败：', err)
    showToast('网络异常，请稍后重试')
  }
}

/** 修改地址：打开弹窗（预填原收货信息） */
function handleAddressClick() {
  addressVisible.value = true
}

/** 地址修改成功：重新拉取详情 */
function handleAddressSuccess(): void {
  void fetchDetail()
}

/** 图片加载失败时替换为占位图 */
function handleImgError(e: Event) {
  const img = e.target as HTMLImageElement
  img.src = defaultCover
}

// ==================== 生命周期 ====================

onMounted(() => {
  void fetchDetail()
  // 每秒刷新 now，驱动待付款倒计时
  tickTimer = window.setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onBeforeUnmount(() => {
  window.clearInterval(tickTimer)
})
</script>

<style scoped>
/* ==================== 页面容器 ==================== */
.order-detail-page {
  min-height: 100vh;
  background: #f8f3eb; /* 米白底色 */
}

/* ==================== 顶部导航栏 ==================== */
.order-detail-page__bar {
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

.order-detail-page__back {
  font-size: 20px;
  cursor: pointer;
  color: #333;
}

.order-detail-page__title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.order-detail-page__placeholder {
  width: 20px;
}

/* ==================== 信息卡片 ==================== */
.od-card {
  margin: 12px 16px;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.od-card__title {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 700;
  color: #91572c; /* 主棕色 */
}

.od-card__row {
  margin: 0 0 8px;
  font-size: 14px;
  line-height: 1.5;
  color: #666;
  word-break: break-all;
}

.od-card__row:last-child {
  margin-bottom: 0;
}

.od-card__row--pay {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.od-card__pay-amount {
  font-size: 18px;
  font-weight: 700;
  color: #c0392b; /* 实付金额红色 */
}

/* 状态卡片 */
.od-card__status-head {
  margin-bottom: 10px;
}

.od-card__status {
  font-size: 16px;
  font-weight: 700;
}

.od-card__status--0 {
  color: #c46b2b;
}

.od-card__status--1,
.od-card__status--2 {
  color: #8b4513;
}

.od-card__status--3,
.od-card__status--4 {
  color: #999;
}

.od-card__status--5 {
  color: #c0392b;
}

.od-card__cancel-type {
  font-size: 13px;
  color: #999;
}

/* 待付款红色倒计时 */
.od-card__countdown {
  margin: 0;
  font-size: 13px;
  color: #666;
}

.od-card__countdown span {
  font-weight: 700;
  color: #e02020; /* 倒计时红色醒目 */
  font-variant-numeric: tabular-nums;
}

/* 藏品快照行 */
.od-goods {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f5efe6;
}

.od-goods:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.od-goods__cover {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  object-fit: cover;
  background: #f5f0eb;
  flex-shrink: 0;
}

.od-goods__info {
  flex: 1;
  min-width: 0;
}

.od-goods__name {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.od-goods__dynasty {
  margin: 0 0 4px;
  font-size: 12px;
  color: #999;
}

.od-goods__price {
  margin: 0;
  font-size: 12px;
  color: #666;
}

.od-goods__subtotal {
  font-size: 13px;
  font-weight: 600;
  color: #c0392b;
  flex-shrink: 0;
}

/* ==================== 底部操作栏 ==================== */
.order-detail-page__padding {
  height: 70px; /* 底部操作栏占位，防止遮挡内容 */
}

.order-detail-page__actions {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 10px 16px;
  padding-bottom: calc(10px + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1px solid #eee;
  z-index: 100;
}

/* 主按钮：实色 */
.obtn--primary {
  background: #c46b2b; /* 按钮橙色 */
  color: #fff;
  border: 1px solid #c46b2b;
}

/* 次要按钮：边框 */
.obtn--plain {
  background: #fff;
  color: #91572c; /* 主棕色 */
  border: 1px solid #c9b294;
}

/* 危险操作：红色文字/边框 */
.obtn--danger {
  background: #fff;
  color: #c0392b;
  border: 1px solid #e3b8b8;
}

.obtn {
  padding: 8px 20px;
  border-radius: 18px;
  font-size: 14px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.obtn:active {
  opacity: 0.8;
}

/* ==================== 加载中 / 空状态 / 异常 ==================== */
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
  animation: od-spin 0.8s linear infinite;
}

@keyframes od-spin {
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
