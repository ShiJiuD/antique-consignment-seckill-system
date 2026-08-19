<template>
  <div class="history-page">
    <!-- 顶部导航栏：右侧「清空」一键清空全部浏览记录 -->
    <div class="history-page__bar">
      <span class="history-page__back" @click="goBack">←</span>
      <span class="history-page__title">浏览记录</span>
      <button
        v-if="!loading && !loadError && historyList.length > 0"
        type="button"
        class="history-page__clear"
        @click="handleClearClick"
      >
        清空
      </button>
      <span v-else class="history-page__placeholder"></span>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="state-box">
      <span class="state-box__spinner"></span>
      <p class="state-box__text">加载中...</p>
    </div>

    <!-- 加载异常（网络异常 / 业务失败均可重试） -->
    <div v-else-if="loadError" class="state-box">
      <span class="state-box__icon">😥</span>
      <p class="state-box__text">加载失败，请检查网络后重试</p>
      <button type="button" class="state-box__btn" @click="fetchHistory">
        重新加载
      </button>
    </div>

    <!-- 空状态 -->
    <div v-else-if="historyList.length === 0" class="state-box">
      <span class="state-box__icon">🕐</span>
      <p class="state-box__text">暂无浏览记录</p>
    </div>

    <!-- 浏览记录列表：封面 + 名称 + 浏览时间，点击跳详情，支持单条删除 -->
    <div v-else class="history-list">
      <div
        v-for="item in historyList"
        :key="item.id"
        class="history-card"
        @click="goDetail(item)"
      >
        <img
          class="history-card__cover"
          :src="item.coverImage || defaultCover"
          :alt="item.title"
          @error="handleImgError"
        />
        <div class="history-card__info">
          <h4 class="history-card__name">{{ item.title }}</h4>
          <p class="history-card__time">{{ formatTime(item.browseTime) }}</p>
        </div>
        <button
          type="button"
          class="history-card__delete"
          @click.stop="handleDeleteClick(item)"
        >
          删除
        </button>
      </div>
    </div>

    <!-- 删除 / 清空确认弹窗（复用一个弹窗，按 mode 区分） -->
    <ConfirmDialog
      v-model:visible="confirmVisible"
      :title="confirmMode === 'clear' ? '清空浏览记录' : '删除浏览记录'"
      :content="
        confirmMode === 'clear'
          ? '确定清空全部浏览记录吗？清空后不可恢复。'
          : `确定删除「${pendingDelete?.title ?? ''}」的浏览记录吗？`
      "
      :confirm-text="confirmMode === 'clear' ? '清空' : '删除'"
      @confirm="handleConfirmAction"
    />

    <!-- 手写轻提示 -->
    <transition name="toast-fade">
      <div v-if="toastVisible" class="page-toast">{{ toastText }}</div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import ConfirmDialog from '@/components/ConfirmDialog/index.vue'
import {
  clearHistory,
  deleteHistory,
  getHistoryList,
  type HistoryItem,
} from '@/api/history'

const router = useRouter()

// ==================== 状态 ====================

/** 浏览记录列表（后端按浏览时间倒序返回） */
const historyList = ref<HistoryItem[]>([])
/** 加载中 */
const loading = ref(false)
/** 加载异常（true 时渲染错误重试页） */
const loadError = ref(false)

/** 确认弹窗模式：删除单条 / 清空全部 */
type ConfirmMode = 'delete' | 'clear'
const confirmMode = ref<ConfirmMode>('delete')
/** 待删除的浏览记录（确认弹窗数据） */
const pendingDelete = ref<HistoryItem | null>(null)
/** 确认弹窗显隐 */
const confirmVisible = ref(false)

/** 封面图加载失败时的兜底占位图 */
const defaultCover = 'https://via.placeholder.com/300x300/f5f0eb/8b4513?text=古玩藏品'

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

// ==================== 工具函数 ====================

/** 浏览时间格式化：2026-08-18T12:30:00 → 2026-08-18 12:30 */
function formatTime(time: string): string {
  return time ? time.replace('T', ' ').slice(0, 16) : ''
}

// ==================== 接口请求 ====================

/**
 * 获取当前登录用户的浏览记录列表（按浏览时间倒序）
 * 接口由请求拦截器统一携带 Authorization: Bearer ${token} 鉴权；
 * 无 token 时路由守卫已重定向登录页
 */
async function fetchHistory(): Promise<void> {
  loading.value = true
  loadError.value = false
  try {
    const res = await getHistoryList()
    if (res.code === 1) {
      historyList.value = res.data.list
    } else {
      // 业务失败：提示后端 msg 并展示错误重试页
      showToast(res.msg || '获取浏览记录失败')
      loadError.value = true
    }
  } catch (err) {
    // 网络异常：展示错误重试页
    console.error('获取浏览记录失败：', err)
    loadError.value = true
  } finally {
    loading.value = false
  }
}

// ==================== 交互 ====================

function goBack() {
  router.back()
}

/** 点击卡片跳转藏品详情页 */
function goDetail(item: HistoryItem) {
  router.push(`/antique/${item.antiqueId}`)
}

/** 点击单条「删除」：记录待删除项并打开确认弹窗 */
function handleDeleteClick(item: HistoryItem) {
  confirmMode.value = 'delete'
  pendingDelete.value = item
  confirmVisible.value = true
}

/** 点击顶部「清空」：打开清空确认弹窗 */
function handleClearClick() {
  confirmMode.value = 'clear'
  pendingDelete.value = null
  confirmVisible.value = true
}

/** 确认弹窗回调：按模式执行删除 / 清空 */
async function handleConfirmAction(): Promise<void> {
  if (confirmMode.value === 'delete') {
    await confirmDelete()
  } else {
    await confirmClear()
  }
}

/** 确认删除单条浏览记录：调用接口并移除本地列表项 */
async function confirmDelete(): Promise<void> {
  const item = pendingDelete.value
  if (!item) return
  try {
    const res = await deleteHistory(item.id)
    if (res.code === 1) {
      historyList.value = historyList.value.filter((h) => h.id !== item.id)
      showToast('已删除')
    } else {
      showToast(res.msg || '删除失败，请稍后重试')
    }
  } catch (err) {
    console.error('删除浏览记录失败：', err)
    showToast('网络异常，请稍后重试')
  } finally {
    pendingDelete.value = null
  }
}

/** 确认清空全部浏览记录：调用接口并清空本地列表 */
async function confirmClear(): Promise<void> {
  try {
    const res = await clearHistory()
    if (res.code === 1) {
      historyList.value = []
      showToast('已清空浏览记录')
    } else {
      showToast(res.msg || '清空失败，请稍后重试')
    }
  } catch (err) {
    console.error('清空浏览记录失败：', err)
    showToast('网络异常，请稍后重试')
  }
}

/** 图片加载失败时替换为占位图 */
function handleImgError(e: Event) {
  const img = e.target as HTMLImageElement
  img.src = defaultCover
}

// ==================== 生命周期 ====================

onMounted(() => {
  void fetchHistory()
})
</script>

<style scoped>
/* ==================== 页面容器（与个人中心同款浅色国风底色） ==================== */
.history-page {
  min-height: 100vh;
  background: #f8f3eb; /* 页面背景 */
}

/* ==================== 顶部导航栏 ==================== */
.history-page__bar {
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

.history-page__back {
  font-size: 20px;
  cursor: pointer;
  color: #333;
}

.history-page__title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

/* 顶部「清空」按钮（橙色文字） */
.history-page__clear {
  padding: 4px 10px;
  border: none;
  border-radius: 12px;
  background: #fdf3ea;
  color: #c46b2b; /* 按钮橙色 */
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s;
}

.history-page__clear:active {
  background: #f7e3cf;
}

/* 占位：保持标题居中 */
.history-page__placeholder {
  width: 40px;
}

/* ==================== 浏览记录列表 ==================== */
.history-list {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: transform 0.15s;
}

.history-card:active {
  transform: scale(0.99);
}

.history-card__cover {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  object-fit: cover;
  background: #f5f0eb;
  flex-shrink: 0;
}

.history-card__info {
  flex: 1;
  min-width: 0; /* 防止 flex 溢出 */
}

.history-card__name {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-card__time {
  margin: 0;
  font-size: 12px;
  color: #999;
}

/* 单条删除按钮 */
.history-card__delete {
  padding: 5px 14px;
  border: 1px solid #e0d0c0;
  border-radius: 14px;
  background: #fff;
  color: #999;
  font-size: 12px;
  cursor: pointer;
  flex-shrink: 0;
  transition: color 0.15s, border-color 0.15s;
}

.history-card__delete:active {
  color: #c46b2b;
  border-color: #c46b2b;
}

/* ==================== 加载中 / 空状态 / 异常（三态统一布局） ==================== */
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

/* 手写加载圈 */
.state-box__spinner {
  width: 28px;
  height: 28px;
  border: 3px solid #f0e6d3;
  border-top-color: #c46b2b;
  border-radius: 50%;
  animation: history-spin 0.8s linear infinite;
}

@keyframes history-spin {
  to {
    transform: rotate(360deg);
  }
}

/* 重试按钮 */
.state-box__btn {
  padding: 8px 28px;
  border: none;
  border-radius: 18px;
  background: #c46b2b; /* 按钮橙色 */
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
