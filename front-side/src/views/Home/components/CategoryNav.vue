<template>
  <div class="category-nav">
    <div
      v-for="item in categoryList"
      :key="item.id"
      class="category-item"
      :class="{ active: activeId === item.id }"
      @click="handleSelect(item.id)"
    >
      <span class="category-icon">{{ item.icon }}</span>
      <span class="category-name">{{ item.name }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

export interface CategoryItem {
  id: number
  name: string
  icon: string
}

const props = withDefaults(defineProps<{
  activeId?: number
}>(), {
  activeId: 1,
})

const emit = defineEmits<{
  (e: 'select', categoryId: number): void
}>()

/** 内置 5 大古玩分类 */
const categoryList: CategoryItem[] = [
  { id: 1, name: '瓷器', icon: '🏺' },
  { id: 2, name: '字画', icon: '🖼️' },
  { id: 3, name: '玉器', icon: '💎' },
  { id: 4, name: '铜器', icon: '🔔' },
  { id: 5, name: '杂项', icon: '📿' },
]

const activeId = ref<number>(props.activeId)

watch(() => props.activeId, (val) => {
  activeId.value = val
})

function handleSelect(id: number) {
  activeId.value = id
  emit('select', id)
}
</script>

<style scoped>
.category-nav {
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding: 14px 12px;
  background: #fff;
  border-bottom: 1px solid #f0ebe4;
}

.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  flex-shrink: 0;
  padding: 6px 10px;
  border-radius: 10px;
  transition: color 0.2s, background 0.2s;
  color: #999;
  min-width: 56px;
}

.category-item.active {
  color: #8b4513;
  background: #faf6f0;
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
