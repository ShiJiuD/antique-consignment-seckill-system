<template>
  <div class="order-list-page">
    <!-- 顶部导航栏 -->
    <div class="order-list-page__bar">
      <span class="order-list-page__back" @click="goBack">←</span>
      <span class="order-list-page__title">我的订单</span>
      <span class="order-list-page__placeholder"></span>
    </div>

    <!-- 状态 Tab：全部 / 待付款 / 待发货 / 待收货 / 已完成 / 退款售后 -->
    <div class="order-tabs">
      <div
        v-for="tab in TABS"
        :key="String(tab.status)"
        class="order-tabs__item"
        :class="{ 'order-tabs__item--active': currentStatus === tab.status }"
        @click="handleTabChange(tab.status)"
      >
        {{ tab.label }}
      </div>
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
      <button type="button" class="state-box__btn" @click="refreshList">
        重新加载
      </button>
    </div>

    <!-- 空状态 -->
    <div v-else-if="orderList?.length === 0" class="state-box">
      <span class="state-box__icon">🧾</span>
      <p class="state-box__text">暂无订单记录</p>
    </div>

    <!-- 订单列表 -->
    <template v-else>
      <div class="order-list">
        <div
          v-for="item in orderList"
          :key="item.id"
          class="order-card"
          @click="goDetail(item)"
        >
          <!-- 订单号 + 状态标签 -->
          <div class="order-card__head">
            <span class="order-card__no">订单号：{{ item.orderNo }}</span>
            <span class="order-card__status" :class="`order-card__status--${item.status}`">
              {{ ORDER_STATUS_TEXT[item.status] ?? '未知状态' }}
            </span>
          </div>

          <!-- 藏品快照：封面、名称、年代、单价 -->
          <div v-if="firstItem(item)" class="order-card__goods">
            <img
              class="order-card__cover"
              :src="firstItem(item)?.coverImage || defaultCover"
              :alt="firstItem(item)?.title"
              @error="handleImgError"
            />
            <div class="order-card__goods-info">
              <h4 class="order-card__name">{{ firstItem(item)?.title }}</h4>
              <p class="order-card__dynasty">年代：{{ firstItem(item)?.dynasty || '—' }}</p>
              <p class="order-card__price">
                单价：¥{{ (firstItem(item)?.price ?? 0).toLocaleString() }}
              </p>
            </div>
          </div>

          <!-- 手机号脱敏 / 总金额 / 待付款倒计时 -->
          <div class="order-card__meta">
            <p class="order-card__receiver">收件人：{{ maskPhone(item.receiverPhone) }}</p>
            <p class="order-card__total">
              共 {{ item.items?.length }} 件，合计
              <span class="order-card__amount">¥{{ item.totalAmount.toLocaleString() }}</span>
            </p>
            <!-- 待付款：红色倒计时 -->
            <p v-if="item.status === 0" class="order-card__countdown">
              支付剩余 <span>{{ formatCountdown(item.payDeadline, now) }}</span>
            </p>
          </div>

          <!-- 底部操作按钮：按订单状态渲染 -->
          <div v-if="hasActions(item.status)" class="order-card__actions">
            <!-- 0 待付款：取消订单 / 去支付 -->
            <template v-if="item.status === 0">
              <button type="button" class="obtn obtn--danger" @click.stop="handleCancelClick(item)">
                取消订单
              </button>
              <button type="button" class="obtn obtn--primary" @click.stop="goPay(item)">
                去支付
              </button>
            </template>
            <!-- 1 待发货：修改地址 / 催发货 -->
            <template v-else-if="item.status === 1">
              <button type="button" class="obtn obtn--plain" @click.stop="handleAddressClick(item)">
                修改地址
              </button>
              <button type="button" class="obtn obtn--plain" @click.stop="handleUrge(item)">
                催发货
              </button>
            </template>
            <!-- 2 待收货：确认收货 -->
            <template v-else-if="item.status === 2">
              <button type="button" class="obtn obtn--primary" @click.stop="handleReceiveClick(item)">
                确认收货
              </button>
            </template>
          </div>
        </div>
      </div>

      <!-- 分页：加载更多 -->
      <div v-if="hasMore" class="order-list-page__more">
        <button
          type="button"
          class="order-list-page__more-btn"
          @click="loadMore"
        >
          {{ loadingMore ? '加载中...' : '加载更多' }}
        </button>
      </div>
    </template>

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
      :order-id="pendingOrder?.id ?? 0"
      :receiver-name="pendingOrder?.receiverName ?? ''"
      :receiver-phone="pendingOrder?.receiverPhone ?? ''"
      :receiver-address="pendingOrder?.receiverAddress ?? ''"
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
import { useRouter } from 'vue-router'
import ConfirmDialog from '@/components/ConfirmDialog/index.vue'
import AddressEditPopup from '@/components/AddressEditPopup/index.vue'
import {
  cancelOrder,
  confirmReceive,
  getOrderList,
  urgeShip,
  type Order,
  type OrderListParams,
} from '@/api/order'
import { ORDER_STATUS_TEXT, formatCountdown, maskPhone } from '@/utils/order'

const router = useRouter()

// ==================== Tab 定义 ====================
// status 参数：不传=全部，0待付款 1待发货 2待收货 3已完成 5退款/售后

interface OrderTab {
  label: string
  /** '' 表示全部（不传 status 参数） */
  status: number | ''
}

const TABS: OrderTab[] = [
  { label: '全部', status: '' },
  { label: '待付款', status: 0 },
  { label: '待发货', status: 1 },
  { label: '待收货', status: 2 },
  { label: '已完成', status: 3 },
  { label: '退款/售后', status: 5 },
]

// ==================== 状态 ====================

/** 当前选中 Tab 的 status 值（'' 表示全部） */
const currentStatus = ref<number | ''>('')
/** 订单列表 */
const orderList = ref<Order[]>([])
/** 总条数（分页判断） */
const total = ref(0)
/** 当前页码 */
const page = ref(1)
/** 每页条数 */
const PAGE_SIZE = 10
/** 初始加载中 */
const loading = ref(false)
/** 加载更多中 */
const loadingMore = ref(false)
/** 加载异常 */
const loadError = ref(false)

/** 当前时间戳（每秒刷新，驱动待付款倒计时显示） */
const now = ref(Date.now())
let tickTimer: number | undefined

/** 二次确认弹窗模式：取消订单 / 确认收货 */
type ConfirmMode = 'cancel' | 'receive'
const confirmMode = ref<ConfirmMode>('cancel')
/** 当前操作的订单（二次确认 / 修改地址共用） */
const pendingOrder = ref<Order | null>(null)
const confirmVisible = ref(false)
/** 修改地址弹窗显隐 */
const addressVisible = ref(false)

const defaultCover = 'https://via.placeholder.com/300x300/f5f0eb/8b4513?text=古玩藏品'

/** 二次确认弹窗文案 */
const confirmContent = computed<string>(() => {
  if (confirmMode.value === 'cancel') {
    return `确定取消订单「${pendingOrder.value?.orderNo ?? ''}」吗？`
  }
  return '确认已收到藏品？确认后订单将完成。'
})

/** 是否还有更多数据 */
const hasMore = computed<boolean>(() => orderList.value?.length < total.value)

// ==================== 工具函数 ====================

/** 该状态是否渲染操作按钮（3/4/5 无操作按钮） */
function hasActions(status: number): boolean {
  return status === 0 || status === 1 || status === 2
}

/** 订单首个藏品快照 */
function firstItem(order: Order) {
  return order.items?.length > 0 ? order.items[0] : null
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

/**
 * 获取订单列表（分页）
 * @param append 是否追加到现有列表（加载更多）
 */
async function fetchOrders(append: boolean): Promise<void> {
  if (append) {
    loadingMore.value = true
  } else {
    loading.value = true
  }
  loadError.value = false
  try {
    const params: OrderListParams = { page: page.value, size: PAGE_SIZE }
    // 全部 Tab 不传 status 参数
    if (currentStatus.value !== '') {
      params.status = currentStatus.value
    }
    const res = await getOrderList(params)
    if (res.code === 1) {
      orderList.value = append ? orderList.value.concat(res.data.list) : res.data.list
      total.value = res.data.total
    } else {
      showToast(res.msg || '获取订单列表失败')
      if (!append) loadError.value = true
    }
  } catch (err) {
    console.error('获取订单列表失败：', err)
    if (append) {
      showToast('网络异常，请稍后重试')
    } else {
      loadError.value = true
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

/** 切换 Tab：重置分页并重新拉取 */
function handleTabChange(status: number | ''): void {
  if (currentStatus.value === status) return
  currentStatus.value = status
  refreshList()
}

/** 刷新当前列表（回到第一页） */
function refreshList(): void {
  page.value = 1
  void fetchOrders(false)
}

/** 加载更多：页码 +1 追加数据 */
function loadMore(): void {
  if (loadingMore.value || !hasMore.value) return
  page.value += 1
  void fetchOrders(true)
}

// ==================== 交互 ====================

function goBack() {
  router.back()
}

/** 点击整张订单卡片跳转订单详情页 */
function goDetail(item: Order) {
  router.push(`/order/detail/${item.id}`)
}

/** 去支付：跳转模拟支付页（携带 orderId、payDeadline） */
function goPay(item: Order) {
  router.push({
    path: '/order/pay',
    query: {
      orderId: String(item.id),
      payDeadline: item.payDeadline,
    },
  })
}

/** 取消订单：打开二次确认弹窗 */
function handleCancelClick(item: Order) {
  confirmMode.value = 'cancel'
  pendingOrder.value = item
  confirmVisible.value = true
}

/** 确认收货：打开二次确认弹窗 */
function handleReceiveClick(item: Order) {
  confirmMode.value = 'receive'
  pendingOrder.value = item
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

/** 取消订单：成功后刷新列表 */
async function doCancel(): Promise<void> {
  const item = pendingOrder.value
  if (!item) return
  try {
    const res = await cancelOrder(item.id)
    if (res.code === 1) {
      showToast('订单已取消')
      refreshList()
    } else {
      showToast(res.msg || '取消失败，请稍后重试')
    }
  } catch (err) {
    console.error('取消订单失败：', err)
    showToast('网络异常，请稍后重试')
  } finally {
    pendingOrder.value = null
  }
}

/** 确认收货：成功后刷新列表 */
async function doReceive(): Promise<void> {
  const item = pendingOrder.value
  if (!item) return
  try {
    const res = await confirmReceive(item.id)
    if (res.code === 1) {
      showToast('确认收货成功')
      refreshList()
    } else {
      showToast(res.msg || '操作失败，请稍后重试')
    }
  } catch (err) {
    console.error('确认收货失败：', err)
    showToast('网络异常，请稍后重试')
  } finally {
    pendingOrder.value = null
  }
}

/** 催发货：直接调用接口，成功提示 */
async function handleUrge(item: Order): Promise<void> {
  try {
    const res = await urgeShip(item.id)
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
function handleAddressClick(item: Order) {
  pendingOrder.value = item
  addressVisible.value = true
}

/** 地址修改成功：刷新列表 */
function handleAddressSuccess(): void {
  refreshList()
}

/** 图片加载失败时替换为占位图 */
function handleImgError(e: Event) {
  const img = e.target as HTMLImageElement
  img.src = defaultCover
}

// ==================== 生命周期 ====================

onMounted(() => {
  void fetchOrders(false)
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
.order-list-page {
  min-height: 100vh;
  background: #f8f3eb; /* 米白底色 */
}

/* ==================== 顶部导航栏 ==================== */
.order-list-page__bar {
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

.order-list-page__back {
  font-size: 20px;
  cursor: pointer;
  color: #333;
}

.order-list-page__title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.order-list-page__placeholder {
  width: 20px;
}

/* ==================== 状态 Tab ==================== */
.order-tabs {
  position: sticky;
  top: 45px;
  z-index: 99;
  display: flex;
  background: #fff;
  border-bottom: 1px solid #f0e6d3;
}

.order-tabs__item {
  flex: 1;
  padding: 12px 0;
  text-align: center;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
}

.order-tabs__item--active {
  color: #c46b2b; /* 按钮橙色 */
  font-weight: 600;
}

.order-tabs__item--active::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 0;
  transform: translateX(-50%);
  width: 24px;
  height: 3px;
  border-radius: 2px;
  background: #c46b2b;
}

/* ==================== 订单卡片 ==================== */
.order-list {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-card {
  background: #fff;
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: transform 0.15s;
}

.order-card:active {
  transform: scale(0.99);
}

/* 订单号 + 状态标签 */
.order-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px solid #f5efe6;
}

.order-card__no {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 60%;
}

.order-card__status {
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.order-card__status--0 {
  color: #c46b2b;
}

.order-card__status--1,
.order-card__status--2 {
  color: #8b4513;
}

.order-card__status--3,
.order-card__status--4 {
  color: #999;
}

.order-card__status--5 {
  color: #c0392b;
}

/* 藏品快照 */
.order-card__goods {
  display: flex;
  gap: 12px;
  padding: 12px 0;
}

.order-card__cover {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  object-fit: cover;
  background: #f5f0eb;
  flex-shrink: 0;
}

.order-card__goods-info {
  flex: 1;
  min-width: 0;
}

.order-card__name {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-card__dynasty {
  margin: 0 0 4px;
  font-size: 12px;
  color: #999;
}

.order-card__price {
  margin: 0;
  font-size: 13px;
  color: #666;
}

/* 元信息：手机号脱敏 / 总金额 / 待付款倒计时 */
.order-card__meta {
  padding-top: 10px;
  border-top: 1px solid #f5efe6;
}

.order-card__receiver {
  margin: 0 0 4px;
  font-size: 12px;
  color: #999;
}

.order-card__total {
  margin: 0 0 4px;
  font-size: 12px;
  color: #666;
}

.order-card__amount {
  font-size: 15px;
  font-weight: 700;
  color: #c0392b;
}

/* 待付款红色倒计时 */
.order-card__countdown {
  margin: 0;
  font-size: 12px;
  color: #666;
}

.order-card__countdown span {
  font-weight: 700;
  color: #e02020; /* 倒计时红色醒目 */
  font-variant-numeric: tabular-nums;
}

/* ==================== 操作按钮 ==================== */
.order-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 10px;
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
  padding: 6px 16px;
  border-radius: 15px;
  font-size: 13px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.obtn:active {
  opacity: 0.8;
}

/* ==================== 加载更多 ==================== */
.order-list-page__more {
  padding: 4px 16px 24px;
  text-align: center;
}

.order-list-page__more-btn {
  width: 100%;
  padding: 10px 0;
  border: 1px solid #e0d0c0;
  border-radius: 20px;
  background: #fff;
  color: #91572c;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.15s;
}

.order-list-page__more-btn:active {
  background: #faf7f2;
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
  animation: order-spin 0.8s linear infinite;
}

@keyframes order-spin {
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
