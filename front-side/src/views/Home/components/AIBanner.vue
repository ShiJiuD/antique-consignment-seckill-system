<template>
  <div class="ai-banner" @click="handleClick">
    <div class="ai-banner__content">
      <h2 class="ai-banner__title">AI智能助手</h2>
      <p class="ai-banner__subtitle">古玩鉴赏 · 价格洞察 · 藏品对比</p>
    </div>
    <div class="ai-banner__icon-wrap">
      <span class="ai-banner__icon">🤖</span>
    </div>
    <div class="ai-banner__arrow">
      <span>›</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const emit = defineEmits<{
  (e: 'click'): void
}>()

const router = useRouter()

function handleClick() {
  emit('click')
  router.push('/ai')
}
</script>

<style scoped>
.ai-banner {
  display: flex;
  align-items: center;
  margin: 12px 16px;
  padding: 16px 18px;
  border-radius: 14px;
  background: linear-gradient(135deg, #3e1f00 0%, #6b3a1f 40%, #8b4513 100%);
  box-shadow: 0 4px 16px rgba(62, 31, 0, 0.25);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  user-select: none;
  overflow: hidden;
  position: relative;
}

.ai-banner::before {
  content: '';
  position: absolute;
  top: -30px;
  right: -30px;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(212, 175, 55, 0.3) 0%, transparent 70%);
  pointer-events: none;
}

.ai-banner:active {
  transform: scale(0.98);
  box-shadow: 0 2px 8px rgba(62, 31, 0, 0.3);
}

.ai-banner__content {
  flex: 1;
  min-width: 0;
  z-index: 1;
}

.ai-banner__title {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 700;
  color: #d4af37;
  letter-spacing: 1px;
}

.ai-banner__subtitle {
  margin: 0;
  font-size: 13px;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.75);
  letter-spacing: 0.5px;
}

.ai-banner__icon-wrap {
  flex-shrink: 0;
  margin-left: 12px;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
}

.ai-banner__icon {
  font-size: 28px;
  line-height: 1;
}

.ai-banner__arrow {
  flex-shrink: 0;
  margin-left: 8px;
  color: rgba(255, 255, 255, 0.4);
  font-size: 24px;
  line-height: 1;
  z-index: 1;
}
</style>
