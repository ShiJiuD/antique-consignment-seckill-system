<template>
  <div class="home-page">
    <!-- ==================== 顶部搜索栏 ==================== -->
    <div class="search-bar">
      <h1 class="search-bar__title" @click="handleTitleClick">古玩寄卖</h1>
      <div class="search-bar__input-wrap">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索藏品、年代、材质..."
          size="default"
          clearable
          @keyup.enter="handleSearch"
        />
      </div>
      <button class="search-bar__btn" @click="handleSearch">搜索</button>
    </div>

    <!-- ==================== 分类导航 ==================== -->
    <CategoryNav
      :categories="categoryList"
      :active-id="activeCategoryId"
      @change="handleCategoryChange"
    />

    <!-- ==================== AI 智能助手横幅 ==================== -->
    <AIBanner @click="handleAIClick" />

    <!-- ==================== 热门推荐 ==================== -->
    <AntiqueList
      :list="hotAntiqueList"
      :loading="hotLoading"
      @item-click="handleAntiqueClick"
      @more="handleMore"
    />

    <!-- ==================== 底部占位（防止 Tab 栏遮挡内容） ==================== -->
    <div class="home-page__placeholder"></div>

    <!-- ==================== 底部 Tab 栏 ==================== -->
    <div class="tab-bar">
      <div
        v-for="tab in tabList"
        :key="tab.key"
        class="tab-bar__item"
        :class="{ active: activeTab === tab.key }"
        @click="handleTabChange(tab.key)"
      >
        <span class="tab-bar__icon">{{ tab.icon }}</span>
        <span class="tab-bar__label">{{ tab.label }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAntiqueList, type AntiqueItem } from '@/api/antique'
import CategoryNav from '@/components/CategoryNav.vue'
import type { CategoryItem } from '@/components/CategoryNav.vue'
import AIBanner from '@/components/AIBanner.vue'
import AntiqueList from '@/components/AntiqueList.vue'

// ==================== 搜索栏 ====================
const searchKeyword = ref('')

function handleSearch() {
  const keyword = searchKeyword.value.trim()
  if (!keyword) {
    // ElMessage 在 Element Plus 中可直接使用（已全局注册）
    return
  }
  // TODO: 跳转搜索页或调用搜索接口
  console.log('搜索:', keyword)
}

function handleTitleClick() {
  // 点击标题回顶部或刷新
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// ==================== 分类导航（前端写死） ====================
const activeCategoryId = ref<number>(1)

const categoryList: CategoryItem[] = [
  { id: 1, name: '瓷器', icon: '🏺' },
  { id: 2, name: '字画', icon: '🖼️' },
  { id: 3, name: '玉器', icon: '💎' },
  { id: 4, name: '铜器', icon: '🔔' },
  { id: 5, name: '杂项', icon: '📿' },
]

function handleCategoryChange(categoryId: number) {
  activeCategoryId.value = categoryId
  console.log('切换分类:', categoryId)
  // TODO: 根据分类加载对应藏品列表
}

// ==================== AI 智能助手 ====================
function handleAIClick() {
  console.log('打开 AI 智能助手')
  // TODO: 跳转 AI 助手页面或弹出对话面板
}

// ==================== 热门推荐 ====================
const hotAntiqueList = ref<AntiqueItem[]>([])
const hotLoading = ref(false)

async function fetchHotAntiques() {
  hotLoading.value = true
  try {
    const res = await getAntiqueList({ isHot: true, page: 1, size: 20 })
    hotAntiqueList.value = res.list ?? []
  } catch (err) {
    console.error('获取热门藏品失败:', err)
  } finally {
    hotLoading.value = false
  }
}

function handleAntiqueClick(item: AntiqueItem) {
  console.log('点击藏品:', item)
  // TODO: 跳转藏品详情页
}

function handleMore() {
  console.log('查看更多热门藏品')
  // TODO: 跳转热门藏品列表页
}

// ==================== 底部 Tab 栏（前端写死） ====================
const activeTab = ref<'home' | 'discover' | 'message' | 'mine'>('home')

interface TabItem {
  key: 'home' | 'discover' | 'message' | 'mine'
  label: string
  icon: string
}

const tabList: TabItem[] = [
  { key: 'home', label: '首页', icon: '🏠' },
  { key: 'discover', label: '发现', icon: '🔍' },
  { key: 'message', label: '消息', icon: '💬' },
  { key: 'mine', label: '我的', icon: '👤' },
]

function handleTabChange(key: 'home' | 'discover' | 'message' | 'mine') {
  activeTab.value = key
  console.log('切换 Tab:', key)
  // TODO: 根据 tab 跳转对应页面
}

// ==================== 生命周期 ====================
onMounted(() => {
  fetchHotAntiques()
})
</script>

<style scoped>
/* ==================== 页面容器 ==================== */
.home-page {
  min-height: 100vh;
  background: #f7f5f2;
  padding-bottom: 60px; /* Tab 栏高度 */
}

/* ==================== 搜索栏 ==================== */
.search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: linear-gradient(135deg, #5d2e0c, #8b4513);
  position: sticky;
  top: 0;
  z-index: 100;
}

.search-bar__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #d4af37;
  white-space: nowrap;
  cursor: pointer;
  letter-spacing: 1px;
}

.search-bar__input-wrap {
  flex: 1;
  min-width: 0;
}

.search-bar__input-wrap :deep(.el-input__wrapper) {
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: none;
}

.search-bar__input-wrap :deep(.el-input__inner) {
  font-size: 13px;
}

.search-bar__input-wrap :deep(.el-input__inner::placeholder) {
  color: #bbb;
}

.search-bar__btn {
  padding: 7px 16px;
  border: none;
  border-radius: 20px;
  background: linear-gradient(135deg, #d4af37, #b8942e);
  color: #3e1f00;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: opacity 0.2s;
}

.search-bar__btn:active {
  opacity: 0.85;
}

/* ==================== 底部占位 ==================== */
.home-page__placeholder {
  height: 20px;
}

/* ==================== 底部 Tab 栏 ==================== */
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
