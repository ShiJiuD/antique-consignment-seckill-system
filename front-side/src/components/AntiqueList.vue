<template>
  <div class="antique-list">
    <div class="antique-list__header">
      <h3 class="antique-list__title">🔥 热门推荐</h3>
      <span class="antique-list__more" @click="$emit('more')">
        更多 <span class="antique-list__arrow">›</span>
      </span>
    </div>

    <div v-if="loading" class="antique-list__loading">
      <el-skeleton :rows="2" animated />
    </div>

    <div v-else-if="list.length === 0" class="antique-list__empty">
      <p>暂无藏品推荐</p>
    </div>

    <div v-else class="antique-list__grid">
      <AntiqueCard
        v-for="item in list"
        :key="item.id"
        :antique="item"
        @click="handleItemClick"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { AntiqueItem } from '@/api/antique'
import AntiqueCard from './AntiqueCard.vue'

defineProps<{
  list: AntiqueItem[]
  loading?: boolean
}>()

const emit = defineEmits<{
  (e: 'item-click', item: AntiqueItem): void
  (e: 'more'): void
}>()

function handleItemClick(item: AntiqueItem) {
  emit('item-click', item)
}
</script>

<style scoped>
.antique-list {
  padding: 0 16px;
}

.antique-list__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.antique-list__title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #333;
}

.antique-list__more {
  font-size: 13px;
  color: #999;
  cursor: pointer;
}

.antique-list__arrow {
  font-size: 16px;
}

.antique-list__grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.antique-list__loading {
  padding: 16px 0;
}

.antique-list__empty {
  text-align: center;
  padding: 40px 0;
  color: #999;
  font-size: 14px;
}
</style>
