<template>
  <div class="detail-page">
    <div class="detail-page__bar">
      <span class="detail-page__back" @click="goBack">←</span>
      <span class="detail-page__title">藏品详情</span>
      <span class="detail-page__fav" @click="toggleFavorite">{{ isFav ? '❤️' : '🤍' }}</span>
    </div>

    <div v-if="loading" class="detail-page__loading">
      <el-skeleton :rows="5" animated />
    </div>

    <template v-else-if="detail">
      <!-- 封面大图 -->
      <div class="detail-page__cover">
        <img :src="detail.coverImage" :alt="detail.title" />
      </div>

      <!-- 基本信息 -->
      <div class="detail-page__info">
        <h2 class="detail-page__name">{{ detail.title }}</h2>
        <div class="detail-page__tags">
          <span class="detail-page__tag">{{ detail.dynasty }}</span>
          <span class="detail-page__tag">{{ detail.material }}</span>
          <span class="detail-page__tag" v-if="detail.subCategory">{{ detail.subCategory }}</span>
        </div>
        <div class="detail-page__price">¥{{ detail.price?.toLocaleString() }}</div>
        <div class="detail-page__meta">
          <span>👁 {{ detail.viewCount }}</span>
          <span>👍 {{ detail.likeCount }}</span>
          <span>卖家：{{ detail.sellerName }}</span>
        </div>
      </div>

      <!-- 描述 -->
      <div class="detail-page__desc">
        <h4>藏品描述</h4>
        <p>{{ detail.description }}</p>
      </div>

      <!-- 图片列表 -->
      <div class="detail-page__images" v-if="detail.images?.length">
        <h4>更多图片</h4>
        <div class="detail-page__image-grid">
          <img
            v-for="(img, idx) in detail.images"
            :key="idx"
            :src="img"
            :alt="`${detail.title}-${idx + 1}`"
          />
        </div>
      </div>
    </template>

    <div v-else class="detail-page__empty">
      <p>藏品信息加载失败</p>
    </div>

    <!-- 底部操作栏 -->
    <div class="detail-page__actions" v-if="detail">
      <button class="detail-page__btn detail-page__btn--contact">联系卖家</button>
      <button class="detail-page__btn detail-page__btn--buy">立即购买</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAntiqueDetail, type AntiqueDetail } from '@/api/antique'
import { addFavorite, removeFavorite } from '@/api/favorite'

const route = useRoute()
const router = useRouter()

const detail = ref<AntiqueDetail | null>(null)
const loading = ref(false)
const isFav = ref(false)

function goBack() {
  router.back()
}

async function toggleFavorite() {
  if (!detail.value) return
  try {
    if (isFav.value) {
      await removeFavorite(detail.value.id)
    } else {
      await addFavorite(detail.value.id)
    }
    isFav.value = !isFav.value
  } catch (err) {
    console.error('收藏操作失败:', err)
  }
}

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  try {
    const res = await getAntiqueDetail(id)
    detail.value = (res as any).data ?? res
    isFav.value = detail.value?.isFavorited ?? false
  } catch (err) {
    console.error('获取详情失败:', err)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: #f7f5f2;
  padding-bottom: 70px;
}

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

.detail-page__cover img {
  width: 100%;
  max-height: 320px;
  object-fit: cover;
  display: block;
}

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

.detail-page__price {
  font-size: 24px;
  font-weight: 700;
  color: #c0392b;
  margin-bottom: 10px;
}

.detail-page__meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #999;
}

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

.detail-page__loading {
  padding: 16px;
}

.detail-page__empty {
  text-align: center;
  padding: 60px 0;
  color: #999;
}
</style>
