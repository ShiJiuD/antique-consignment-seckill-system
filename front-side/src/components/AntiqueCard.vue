<template>
  <div class="antique-card" @click="handleClick">
    <div class="antique-card__image">
      <img
        :src="antique.coverImage || defaultCover"
        :alt="antique.title"
        @error="handleImgError"
      />
      <span v-if="antique.isHot" class="antique-card__tag">热门</span>
    </div>
    <div class="antique-card__info">
      <h4 class="antique-card__title">{{ antique.title }}</h4>
      <p class="antique-card__dynasty">{{ antique.dynasty }}</p>
      <p class="antique-card__price">
        ¥{{ antique.price?.toLocaleString() }}
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { AntiqueItem } from '@/api/antique'

const props = defineProps<{
  antique: AntiqueItem
}>()

const emit = defineEmits<{
  (e: 'click', item: AntiqueItem): void
}>()

const defaultCover = 'https://via.placeholder.com/300x300/f5f0eb/8b4513?text=古玩藏品'

function handleClick() {
  emit('click', props.antique)
}

function handleImgError(e: Event) {
  const img = e.target as HTMLImageElement
  img.src = defaultCover
}
</script>

<style scoped>
.antique-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.antique-card:active {
  transform: scale(0.98);
}

.antique-card__image {
  position: relative;
  width: 100%;
  padding-top: 100%;
  overflow: hidden;
  background: #f5f0eb;
}

.antique-card__image img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.antique-card__tag {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 2px 8px;
  border-radius: 4px;
  background: linear-gradient(135deg, #c0392b, #e74c3c);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
}

.antique-card__info {
  padding: 10px 12px;
}

.antique-card__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.antique-card__dynasty {
  margin: 4px 0;
  font-size: 12px;
  color: #999;
}

.antique-card__price {
  margin: 4px 0 0;
  font-size: 15px;
  font-weight: 700;
  color: #c0392b;
}
</style>
