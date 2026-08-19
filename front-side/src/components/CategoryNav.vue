<template>
  <div class="category-nav">
    <div
      v-for="item in categories"
      :key="item.id"
      class="category-item"
      :class="{ active: activeId === item.id }"
      @click="handleClick(item.id)"
    >
      <span class="category-icon">{{ item.icon }}</span>
      <span class="category-name">{{ item.name }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

export interface CategoryItem {
  id: number
  name: string
  icon: string
}

const props = defineProps<{
  categories: CategoryItem[]
  activeId?: number
}>()

const emit = defineEmits<{
  (e: 'change', categoryId: number): void
}>()

const activeId = ref<number>(props.activeId ?? props.categories[0]?.id ?? 0)

function handleClick(id: number) {
  activeId.value = id
  emit('change', id)
}
</script>

<style scoped>
.category-nav {
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.category-nav::-webkit-scrollbar {
  display: none;
}

.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  flex-shrink: 0;
  padding: 4px 8px;
  transition: color 0.2s;
  color: #666;
}

.category-item.active {
  color: #8b4513;
}

.category-icon {
  font-size: 28px;
  line-height: 1;
}

.category-name {
  font-size: 13px;
  font-weight: 500;
}
</style>
