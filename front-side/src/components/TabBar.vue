<template>
  <div class="tab-bar">
    <div
      v-for="tab in tabList"
      :key="tab.key"
      class="tab-bar__item"
      :class="{ active: active === tab.key }"
      @click="handleChange(tab.key)"
    >
      <span class="tab-bar__icon">{{ tab.icon }}</span>
      <span class="tab-bar__label">{{ tab.label }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

type TabKey = 'home' | 'discover' | 'message' | 'mine'

defineProps<{
  active: TabKey
}>()

const emit = defineEmits<{
  (e: 'change', key: TabKey): void
}>()

const router = useRouter()

const tabList = [
  { key: 'home' as TabKey, label: '首页', icon: '🏠', path: '/home' },
  { key: 'discover' as TabKey, label: '发现', icon: '🔍', path: '/discover' },
  { key: 'message' as TabKey, label: '消息', icon: '💬', path: '/message' },
  { key: 'mine' as TabKey, label: '我的', icon: '👤', path: '/mine' },
]

function handleChange(key: TabKey) {
  emit('change', key)
  const tab = tabList.find(t => t.key === key)
  if (tab) {
    router.push(tab.path)
  }
}
</script>

<style scoped>
.tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-around;
  align-items: center;
  height: 56px;
  background: #fff;
  border-top: 1px solid #eee;
  z-index: 100;
  padding-bottom: env(safe-area-inset-bottom);
}

.tab-bar__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  cursor: pointer;
  color: #999;
  transition: color 0.2s;
}

.tab-bar__item.active {
  color: #8b4513;
}

.tab-bar__icon {
  font-size: 22px;
  line-height: 1;
}

.tab-bar__label {
  font-size: 11px;
  font-weight: 500;
}
</style>
