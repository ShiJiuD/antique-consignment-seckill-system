<template>
  <div class="ai-banner">
    <div class="ai-banner__bg"></div>
    <div class="ai-banner__content">
      <div class="ai-banner__text">
        <h3 class="ai-banner__title">AI 智能助手</h3>
        <p class="ai-banner__subtitle">古玩鉴赏 · 价格洞察 · 藏品对比</p>
      </div>
      <div class="ai-banner__action" @click="handleClick">
        <span>立即体验</span>
        <span class="ai-banner__arrow">→</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const emit = defineEmits<{
  (e: 'click'): void
}>()

function handleClick() {
  emit('click')
}
</script>

<style scoped>
.ai-banner {
  position: relative;
  margin: 12px 16px;
  border-radius: 12px;
  overflow: hidden;
  min-height: 100px;
}

.ai-banner__bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  /* 装饰性光晕 */
}

.ai-banner__bg::before {
  content: '';
  position: absolute;
  top: -30px;
  right: -20px;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(139, 69, 19, 0.3), transparent 70%);
}

.ai-banner__bg::after {
  content: '';
  position: absolute;
  bottom: -20px;
  left: 30%;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(212, 175, 55, 0.2), transparent 70%);
}

.ai-banner__content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
}

.ai-banner__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #d4af37;
  letter-spacing: 1px;
}

.ai-banner__subtitle {
  margin: 6px 0 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.ai-banner__action {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  border-radius: 20px;
  background: linear-gradient(135deg, #d4af37, #b8942e);
  color: #1a1a2e;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  transition: opacity 0.2s;
}

.ai-banner__action:active {
  opacity: 0.85;
}

.ai-banner__arrow {
  font-size: 14px;
}
</style>
