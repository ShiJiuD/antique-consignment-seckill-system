<template>
  <div class="antique-card" @click="handleClick">
    <!-- 封面图区域 -->
    <div class="antique-card__cover">
      <img
        class="antique-card__img"
        :src="item.coverImage"
        :alt="item.title"
        loading="lazy"
        @error="onImageError"
      />
      <!-- 热门标签 -->
      <span v-if="item.isHot" class="antique-card__hot-badge">热门</span>
    </div>

    <!-- 藏品信息 -->
    <div class="antique-card__body">
      <h3 class="antique-card__title">{{ item.title }}</h3>
      <p class="antique-card__dynasty">{{ item.dynasty }}</p>
      <p class="antique-card__price">{{ formattedPrice }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { AntiqueItem } from '@/api/antique'

// ==================== Props ====================

const props = defineProps<{
  item: AntiqueItem
}>()

const emit = defineEmits<{
  (e: 'click', item: AntiqueItem): void
}>()

// ==================== 路由 ====================

const router = useRouter()

// ==================== 格式化价格 ====================

const formattedPrice = computed(() => {
  return '￥' + props.item.price.toLocaleString('zh-CN')
})

// ==================== 事件处理 ====================

function handleClick() {
  emit('click', props.item)
  router.push(`/antique/${props.item.id}`)
}

/** 图片加载失败时显示占位图 */
function onImageError(e: Event) {
  const target = e.target as HTMLImageElement
  target.src =
    'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="200" height="200" viewBox="0 0 200 200"%3E%3Crect fill="%23ede8e0" width="200" height="200"/%3E%3Ctext fill="%23b5a898" font-size="14" text-anchor="middle" x="100" y="105"%3E暂无图片%3C/text%3E%3C/svg%3E'
}
</script>

<style scoped>
/* ==================== 卡片容器 ==================== */
.antique-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(139, 69, 19, 0.08);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.antique-card:active {
  transform: scale(0.97);
}

/* ==================== 封面图区域 ==================== */
.antique-card__cover {
  position: relative;
  width: 100%;
  padding-top: 100%; /* 1:1 正方比例 */
  overflow: hidden;
  background: #f5f0e8;
}

.antique-card__img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

/* ==================== 热门标签 ==================== */
.antique-card__hot-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 2px 8px;
  background: linear-gradient(135deg, #d43725, #e65b3c);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  border-radius: 4px;
  letter-spacing: 0.5px;
  box-shadow: 0 2px 6px rgba(212, 55, 37, 0.3);
}

/* ==================== 信息区 ==================== */
.antique-card__body {
  padding: 10px 12px 12px;
}

.antique-card__title {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 600;
  color: #3e2c1a;
  line-height: 1.4;
  /* 单行截断 */
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.antique-card__dynasty {
  margin: 0 0 8px;
  font-size: 12px;
  color: #a08c75;
}

.antique-card__price {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #b85c1a;
  letter-spacing: 0.5px;
}
</style>
