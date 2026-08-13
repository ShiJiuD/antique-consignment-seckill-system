<template>
  <div class="detail-page">
    <!-- 顶部导航栏 -->
    <div class="detail-page__bar">
      <span class="detail-page__back" @click="goBack">←</span>
      <span class="detail-page__title">藏品详情</span>
      <span class="detail-page__fav" @click="toggleFavorite">
        {{ detail?.isFavorited ? '❤️' : '🤍' }}
      </span>
    </div>

    <!-- 加载中：detail 无数据时显示骨架屏 -->
    <div v-if="loading || !detail" class="detail-page__loading">
      <el-skeleton :rows="6" animated />
      <p class="detail-page__loading-text">加载中...</p>
    </div>

    <!-- 藏品信息（正常展示全部字段） -->
    <template v-else>
      <!-- 封面大图 -->
      <div class="detail-page__cover">
        <img
          :src="detail?.coverImage || defaultCover"
          :alt="detail?.title ?? '藏品图片'"
          @error="handleImgError"
        />
        <span v-if="detail?.isHot === 1" class="detail-page__hot-tag">🔥 热门</span>
      </div>

      <!-- 基本信息 -->
      <div class="detail-page__info">
        <h2 class="detail-page__name">{{ detail?.title }}</h2>
        <div class="detail-page__tags">
          <span class="detail-page__tag">{{ detail?.dynasty }}</span>
          <span class="detail-page__tag">{{ detail?.material }}</span>
          <span v-if="detail?.subCategory" class="detail-page__tag">{{ detail?.subCategory }}</span>
          <span
            class="detail-page__tag"
            :class="detail?.status === 1 ? 'detail-page__tag--sale' : 'detail-page__tag--off'"
          >
            {{ detail?.status === 1 ? '在售' : '下架' }}
          </span>
        </div>
        <div class="detail-page__price">¥{{ (detail?.price ?? 0).toLocaleString() }}</div>
        <div class="detail-page__meta">
          <span>👁 {{ detail?.viewCount ?? 0 }}</span>
          <span>👍 {{ detail?.likeCount ?? 0 }}</span>
          <span>卖家：{{ detail?.sellerName }}</span>
        </div>
        <div class="detail-page__extra">
          <span>藏品编号：#{{ detail?.id ?? 0 }}</span>
          <span>分类ID：{{ detail?.categoryId ?? 0 }}</span>
          <span>卖家ID：{{ detail?.sellerId ?? 0 }}</span>
        </div>
        <div class="detail-page__time">上架时间：{{ detail?.createdTime }}</div>
      </div>

      <!-- 藏品描述 -->
      <div class="detail-page__desc">
        <h4>藏品描述</h4>
        <p>{{ detail?.description || '暂无描述' }}</p>
      </div>

      <!-- 更多图片（images 为空数组时整块不渲染，无图不报错） -->
      <div v-if="detail?.images?.length" class="detail-page__images">
        <h4>更多图片</h4>
        <div class="detail-page__image-grid">
          <img
            v-for="(img, idx) in detail?.images"
            :key="idx"
            :src="img"
            :alt="`${detail?.title ?? '藏品'}-${idx + 1}`"
            @error="handleImgError"
          />
        </div>
      </div>
    </template>

    <!-- 底部操作栏 -->
    <div class="detail-page__actions" v-if="detail">
      <button class="detail-page__btn detail-page__btn--contact" @click="handleContact">
        联系卖家
      </button>
      <button class="detail-page__btn detail-page__btn--buy" @click="handleBuy">
        立即购买
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAntiqueDetail, type AntiqueDetail } from '@/api/antique'
import { addFavorite, removeFavorite } from '@/api/favorite'

const route = useRoute()
const router = useRouter()

// ==================== 状态 ====================

/** 详情数据：初始值为 null，加载完成后赋值（做好类型约束） */
const detail = ref<AntiqueDetail | null>(null)
/** 加载状态 */
const loading = ref(false)

/** 封面图加载失败时的兜底占位图 */
const defaultCover = 'https://via.placeholder.com/300x200/f5f0eb/8b4513?text=古玩藏品'

// ==================== 获取详情 ====================

async function fetchDetail() {
  // 从路由 params 中获取藏品 id
  const id = Number(route.params.id)
  if (!id) {
    ElMessage.error('藏品ID无效')
    return
  }

  loading.value = true
  try {
    // 接口返回 { code, msg, data }，code === 1 代表成功
    const res = await getAntiqueDetail(id)
    if (res.code === 1) {
      // 严格取 res.data 赋值
      detail.value = res.data
    } else {
      ElMessage.error(res.msg || '获取藏品详情失败')
    }
  } catch (err) {
    console.error('获取藏品详情失败:', err)
    ElMessage.error('网络异常，请稍后重试')
  } finally {
    loading.value = false
  }
}

// ==================== 交互 ====================

function goBack() {
  router.back()
}

/** 收藏 / 取消收藏 */
async function toggleFavorite() {
  if (!detail.value) return
  try {
    if (detail.value.isFavorited) {
      await removeFavorite(detail.value.id)
    } else {
      await addFavorite(detail.value.id)
    }
    // 收藏状态翻转
    detail.value.isFavorited = !detail.value.isFavorited
    ElMessage.success(detail.value.isFavorited ? '收藏成功' : '已取消收藏')
  } catch (err) {
    console.error('收藏操作失败:', err)
    ElMessage.error('操作失败，请稍后重试')
  }
}

function handleContact() {
  ElMessage.info(`请联系卖家：${detail.value?.sellerName ?? ''}`)
}

function handleBuy() {
  ElMessage.info('购买功能即将上线，敬请期待！')
}

/** 图片加载失败时替换为占位图 */
function handleImgError(e: Event) {
  const img = e.target as HTMLImageElement
  img.src = defaultCover
}

// ==================== 生命周期 ====================
onMounted(() => {
  fetchDetail()
})
</script>

<style scoped>
/* ==================== 页面容器 ==================== */
.detail-page {
  min-height: 100vh;
  background: #f7f5f2;
  padding-bottom: 70px;
}

/* ==================== 顶部导航 ==================== */
.detail-page__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 100;
}

.detail-page__back {
  font-size: 20px;
  cursor: pointer;
  color: #333;
}

.detail-page__title {
  font-size: 16px;
  font-weight: 600;
}

.detail-page__fav {
  font-size: 22px;
  cursor: pointer;
}

/* ==================== 封面 ==================== */
.detail-page__cover {
  position: relative;
}

.detail-page__cover img {
  width: 100%;
  max-height: 320px;
  object-fit: cover;
  display: block;
}

.detail-page__hot-tag {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 3px 10px;
  border-radius: 12px;
  background: linear-gradient(135deg, #c0392b, #e74c3c);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

/* ==================== 基本信息 ==================== */
.detail-page__info {
  padding: 16px;
  background: #fff;
  margin-bottom: 10px;
}

.detail-page__name {
  margin: 0 0 10px;
  font-size: 20px;
  font-weight: 700;
  color: #333;
}

.detail-page__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.detail-page__tag {
  padding: 3px 10px;
  border-radius: 4px;
  background: #f0e6d3;
  color: #8b4513;
  font-size: 12px;
}

.detail-page__tag--sale {
  background: #e8f5e9;
  color: #2e7d32;
}

.detail-page__tag--off {
  background: #f5f5f5;
  color: #999;
}

.detail-page__price {
  font-size: 24px;
  font-weight: 700;
  color: #c0392b;
  margin-bottom: 10px;
}

.detail-page__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 13px;
  color: #999;
  margin-bottom: 8px;
}

.detail-page__extra {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 12px;
  color: #bbb;
  margin-bottom: 4px;
}

.detail-page__time {
  font-size: 12px;
  color: #bbb;
}

/* ==================== 描述 ==================== */
.detail-page__desc {
  padding: 16px;
  background: #fff;
  margin-bottom: 10px;
}

.detail-page__desc h4 {
  margin: 0 0 8px;
  font-size: 15px;
  color: #333;
}

.detail-page__desc p {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: #666;
}

/* ==================== 更多图片 ==================== */
.detail-page__images {
  padding: 16px;
  background: #fff;
}

.detail-page__images h4 {
  margin: 0 0 10px;
  font-size: 15px;
  color: #333;
}

.detail-page__image-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.detail-page__image-grid img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 6px;
}

/* ==================== 底部操作栏 ==================== */
.detail-page__actions {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 12px;
  padding: 10px 16px;
  padding-bottom: calc(10px + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1px solid #eee;
  z-index: 100;
}

.detail-page__btn {
  flex: 1;
  padding: 12px 0;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}

.detail-page__btn--contact {
  background: #f0e6d3;
  color: #8b4513;
}

.detail-page__btn--buy {
  background: linear-gradient(135deg, #8b4513, #a0522d);
  color: #fff;
}

/* ==================== 加载态 ==================== */
.detail-page__loading {
  padding: 16px;
  text-align: center;
}

.detail-page__loading-text {
  margin: 8px 0 0;
  font-size: 13px;
  color: #999;
}
</style>
