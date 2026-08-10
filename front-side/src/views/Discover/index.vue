<template>
  <div class="discover-page">
    <div class="discover-page__header">
      <h2>发现</h2>
      <p class="discover-page__subtitle">探索更多古玩藏品</p>
    </div>

    <!-- 分类浏览 -->
    <div class="discover-page__section">
      <h4 class="discover-page__section-title">分类浏览</h4>
      <div class="discover-page__grid">
        <div
          v-for="cat in categories"
          :key="cat.id"
          class="discover-page__card"
          @click="goCategory(cat.id)"
        >
          <span class="discover-page__card-icon">{{ cat.icon }}</span>
          <span class="discover-page__card-name">{{ cat.name }}</span>
        </div>
      </div>
    </div>

    <div class="discover-page__placeholder--bottom"></div>

    <!-- 底部 Tab 栏 -->
    <div class="tab-bar">
      <div
        v-for="tab in tabList"
        :key="tab.key"
        class="tab-bar__item"
        :class="{ active: 'discover' === tab.key }"
        @click="handleTabChange(tab.key)"
      >
        <span class="tab-bar__icon">{{ tab.icon }}</span>
        <span class="tab-bar__label">{{ tab.label }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const categories = [
  { id: 1, name: '瓷器', icon: '🏺' },
  { id: 2, name: '字画', icon: '🖼️' },
  { id: 3, name: '玉器', icon: '💎' },
  { id: 4, name: '铜器', icon: '🔔' },
  { id: 5, name: '杂项', icon: '📿' },
]

type TabKey = 'home' | 'discover' | 'message' | 'mine'
const tabList = [
  { key: 'home' as TabKey, label: '首页', icon: '🏠' },
  { key: 'discover' as TabKey, label: '发现', icon: '🔍' },
  { key: 'message' as TabKey, label: '消息', icon: '💬' },
  { key: 'mine' as TabKey, label: '我的', icon: '👤' },
]

const TAB_ROUTES: Record<TabKey, string> = {
  home: '/home',
  discover: '/discover',
  message: '/message',
  mine: '/mine',
}

function handleTabChange(key: TabKey) {
  router.push(TAB_ROUTES[key])
}

function goCategory(id: number) {
  router.push({ path: '/search', query: { categoryId: String(id) } })
}
</script>

<style scoped>
.discover-page {
  min-height: 100vh;
  background: #f7f5f2;
  padding-bottom: 70px;
}

.discover-page__header {
  padding: 28px 16px 16px;
  background: linear-gradient(160deg, #4a1d0a, #5d2e0c, #7a3d16, #5d2e0c);
  color: #fff;
}

.discover-page__header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #d4af37;
}

.discover-page__subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.discover-page__section {
  padding: 16px;
}

.discover-page__section-title {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.discover-page__grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.discover-page__card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px 12px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: transform 0.2s;
}

.discover-page__card:active {
  transform: scale(0.96);
}

.discover-page__card-icon {
  font-size: 32px;
}

.discover-page__card-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.discover-page__placeholder--bottom {
  height: 20px;
}

/* ========== Tab 栏 ========== */
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
