<template>
  <div class="favorite-page">
    <!-- 顶部导航栏 -->
    <div class="favorite-page__bar">
      <span class="favorite-page__back" @click="goBack">←</span>
      <span class="favorite-page__title">我的收藏</span>
      <span class="favorite-page__placeholder"></span>
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
      <button type="button" class="state-box__btn" @click="fetchFavorites">
        重新加载
      </button>
    </div>

    <!-- 空状态 -->
    <div v-else-if="favoriteList.length === 0" class="state-box">
      <span class="state-box__icon">📦</span>
      <p class="state-box__text">暂无收藏藏品</p>
    </div>

    <!-- 收藏列表：封面图 + 名称 + 简介，点击跳详情，支持取消收藏 -->
    <div v-else class="favorite-list">
      <div
        v-for="item in favoriteList"
        :key="item.id"
        class="favorite-card"
        @click="goDetail(item)"
      >
        <img
          class="favorite-card__cover"
          :src="item.coverImage || defaultCover"
          :alt="item.title"
          @error="handleImgError"
        />
        <div class="favorite-card__info">
          <h4 class="favorite-card__name">{{ item.title }}</h4>
          <p class="favorite-card__desc">{{ item.description || '暂无简介' }}</p>
          <button
            type="button"
            class="favorite-card__remove"
            @click.stop="handleRemoveClick(item)"
          >
            取消收藏
          </button>
        </div>
      </div>
    </div>

    <!-- 取消收藏确认弹窗 -->
    <ConfirmDialog
      v-model:visible="confirmVisible"
      title="取消收藏"
      :content="`确定不再收藏「${pendingRemove?.title ?? ''}」吗？`"
      confirm-text="取消收藏"
      @confirm="confirmRemove"
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
  getFavoriteList,
  removeFavorite,
  type FavoriteItem,
} from '@/api/favorite'

const router = useRouter()

// ==================== 状态 ====================

/** 收藏列表数据 */
const favoriteList = ref<FavoriteItem[]>([])
/** 加载中 */
const loading = ref(false)
/** 加载异常（true 时渲染错误重试页） */
const loadError = ref(false)

/** 待取消收藏的藏品（确认弹窗数据） */
const pendingRemove = ref<FavoriteItem | null>(null)
/** 取消收藏确认弹窗显隐 */
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

// ==================== 接口请求 ====================

/**
 * 获取当前登录用户收藏的藏品列表
 * 接口由请求拦截器统一携带 Authorization: Bearer ${token} 鉴权；
 * 无 token 时路由守卫已重定向登录页
 */
async function fetchFavorites(): Promise<void> {
  loading.value = true
  loadError.value = false
  try {
    const res = await getFavoriteList({ page: 1, size: 100 })
    if (res.code === 1) {
      favoriteList.value = res.data.list
    } else {
      // 业务失败：提示后端 msg 并展示错误重试页
      showToast(res.msg || '获取收藏列表失败')
      loadError.value = true
    }
  } catch (err) {
    // 网络异常：展示错误重试页
    console.error('获取收藏列表失败：', err)
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
function goDetail(item: FavoriteItem) {
  router.push(`/antique/${item.id}`)
}

/** 点击「取消收藏」：记录待删除项并打开确认弹窗 */
function handleRemoveClick(item: FavoriteItem) {
  pendingRemove.value = item
  confirmVisible.value = true
}

/** 确认取消收藏：调用接口并移除本地列表项 */
async function confirmRemove(): Promise<void> {
  const item = pendingRemove.value
  if (!item) return
  try {
    const res = await removeFavorite(item.id)
    if (res.code === 1) {
      favoriteList.value = favoriteList.value.filter((f) => f.id !== item.id)
      showToast('已取消收藏')
    } else {
      showToast(res.msg || '操作失败，请稍后重试')
    }
  } catch (err) {
    console.error('取消收藏失败：', err)
    showToast('网络异常，请稍后重试')
  } finally {
    pendingRemove.value = null
  }
}

/** 图片加载失败时替换为占位图 */
function handleImgError(e: Event) {
  const img = e.target as HTMLImageElement
  img.src = defaultCover
}

// ==================== 生命周期 ====================

onMounted(() => {
  void fetchFavorites()
})
</script>

<style scoped>
/* ==================== 页面容器（与个人中心同款浅色国风底色） ==================== */
.favorite-page {
  min-height: 100vh;
  background: #f8f3eb; /* 页面背景 */
}

/* ==================== 顶部导航栏 ==================== */
.favorite-page__bar {
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

.favorite-page__back {
  font-size: 20px;
  cursor: pointer;
  color: #333;
}

.favorite-page__title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

/* 占位：保持标题居中 */
.favorite-page__placeholder {
  width: 20px;
}

/* ==================== 收藏列表 ==================== */
.favorite-list {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.favorite-card {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: transform 0.15s;
}

.favorite-card:active {
  transform: scale(0.99);
}

.favorite-card__cover {
  width: 96px;
  height: 96px;
  border-radius: 8px;
  object-fit: cover;
  background: #f5f0eb;
  flex-shrink: 0;
}

.favorite-card__info {
  flex: 1;
  min-width: 0; /* 防止 flex 溢出 */
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.favorite-card__name {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 藏品简介：最多两行省略 */
.favorite-card__desc {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: #999;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 取消收藏按钮 */
.favorite-card__remove {
  align-self: flex-start;
  padding: 5px 14px;
  border: 1px solid #e0d0c0;
  border-radius: 14px;
  background: #fff;
  color: #c46b2b; /* 按钮橙色 */
  font-size: 12px;
  cursor: pointer;
  transition: background 0.15s;
}

.favorite-card__remove:active {
  background: #fdf3ea;
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
  animation: favorite-spin 0.8s linear infinite;
}

@keyframes favorite-spin {
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
