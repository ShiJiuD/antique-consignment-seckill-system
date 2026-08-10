<template>
  <div class="mine-page">
    <div class="mine-page__header">
      <div class="mine-page__avatar">👤</div>
      <div class="mine-page__header-info">
        <h3 class="mine-page__name">{{ username }}</h3>
        <p class="mine-page__phone">{{ phone }}</p>
      </div>
    </div>

    <div class="mine-page__menu">
      <div
        class="mine-page__item"
        v-for="item in menuList"
        :key="item.key"
        @click="handleMenu(item.key)"
      >
        <span class="mine-page__item-left">
          <span class="mine-page__item-icon">{{ item.icon }}</span>
          {{ item.label }}
        </span>
        <span class="mine-page__arrow">›</span>
      </div>
    </div>

    <button class="mine-page__logout" @click="handleLogout">退出登录</button>

    <!-- 底部 Tab 栏 -->
    <div class="tab-bar">
      <div
        v-for="tab in tabList"
        :key="tab.key"
        class="tab-bar__item"
        :class="{ active: 'mine' === tab.key }"
        @click="handleTabChange(tab.key)"
      >
        <span class="tab-bar__icon">{{ tab.icon }}</span>
        <span class="tab-bar__label">{{ tab.label }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const username = ref(localStorage.getItem('nickname') || '古玩爱好者')
const phone = ref(localStorage.getItem('phone') || '')

const menuList = [
  { key: 'favorites', label: '我的收藏', icon: '❤️' },
  { key: 'orders', label: '我的订单', icon: '📦' },
  { key: 'history', label: '浏览记录', icon: '🕐' },
  { key: 'settings', label: '设置', icon: '⚙️' },
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

function handleMenu(key: string) {
  console.log('点击菜单:', key)
}

function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('nickname')
  localStorage.removeItem('phone')
  router.push('/login')
}
</script>

<style scoped>
.mine-page {
  min-height: 100vh;
  background: #f7f5f2;
  padding-bottom: 70px;
}

.mine-page__header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 24px 20px;
  background: linear-gradient(160deg, #4a1d0a, #5d2e0c, #7a3d16, #5d2e0c);
}

.mine-page__avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  flex-shrink: 0;
}

.mine-page__header-info {
  color: #fff;
}

.mine-page__name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #d4af37;
}

.mine-page__phone {
  margin: 4px 0 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.mine-page__menu {
  margin-top: 10px;
  background: #fff;
}

.mine-page__item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;
  font-size: 15px;
  color: #333;
  transition: background 0.15s;
}

.mine-page__item:active {
  background: #fafafa;
}

.mine-page__item-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.mine-page__item-icon {
  font-size: 18px;
}

.mine-page__arrow {
  color: #ccc;
  font-size: 20px;
}

.mine-page__logout {
  display: block;
  width: calc(100% - 32px);
  margin: 30px auto;
  padding: 14px 0;
  border: 1px solid #e0d0c0;
  border-radius: 10px;
  background: #fff;
  color: #c0392b;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}

.mine-page__logout:active {
  background: #fef5f5;
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
