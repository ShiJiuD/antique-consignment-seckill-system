<template>
  <div class="home-page">
    <!-- ==================== 顶部搜索栏 ==================== -->
    <div class="search-bar">
      <div class="search-bar__left">
        <span class="search-bar__icon">🏺</span>
        <h1 class="search-bar__title" @click="handleTitleClick">古玩寄卖</h1>
      </div>
      <div class="search-bar__right">
        <div class="search-bar__input-wrap" @click="openSearchPopup">
          <el-input
            readonly
            placeholder="搜索藏品、年代、材质..."
            size="default"
          >
            <template #prefix>
              <span class="search-bar__search-icon">🔍</span>
            </template>
          </el-input>
        </div>
        <button class="search-bar__btn" @click="openSearchPopup">搜索</button>
      </div>
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

    <!-- ==================== 全屏搜索弹窗 ==================== -->
    <SearchPopup v-model:visible="searchPopupVisible" />

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
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAntiqueList, type AntiqueItem } from '@/api/antique'
import CategoryNav from '@/components/CategoryNav.vue'
import type { CategoryItem } from '@/components/CategoryNav.vue'
import AIBanner from '@/components/AIBanner.vue'
import AntiqueList from '@/components/AntiqueList.vue'
import SearchPopup from '@/components/SearchPopup/index.vue'

const router = useRouter()

// ==================== 搜索栏 ====================
/** 全屏搜索弹窗显隐 */
const searchPopupVisible = ref(false)

/** 点击搜索框 / 搜索按钮：弹出全屏搜索弹窗 */
function openSearchPopup() {
  searchPopupVisible.value = true
}

function handleTitleClick() {
  // 点击标题回顶部并刷新列表
  window.scrollTo({ top: 0, behavior: 'smooth' })
  activeCategoryId.value = 1
  fetchAntiques(1)
}

// ==================== 分类导航 ====================
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
  fetchAntiques(categoryId)
}

// ==================== AI 智能助手 ====================
function handleAIClick() {
  ElMessage.info('AI 智能助手功能即将上线，敬请期待！')
}

// ==================== 热门推荐 ====================
const hotAntiqueList = ref<AntiqueItem[]>([])
const hotLoading = ref(false)

async function fetchAntiques(categoryId?: number) {
  hotLoading.value = true
  try {
    const res = await getAntiqueList({ categoryId, page: 1, size: 20 })
    // 响应拦截器已解包 axios response，后端统一返回 { code, msg, data }
    hotAntiqueList.value = (res as any).data?.list ?? []
  } catch (err) {
    console.error('获取藏品列表失败:', err)
    ElMessage.error('加载藏品失败，请稍后重试')
  } finally {
    hotLoading.value = false
  }
}

function handleAntiqueClick(item: AntiqueItem) {
  router.push(`/antique/${item.id}`)
}

function handleMore() {
  router.push('/search')
}

// ==================== 底部 Tab 栏 ====================
type TabKey = 'home' | 'discover' | 'message' | 'mine'

const activeTab = ref<TabKey>('home')

interface TabItem {
  key: TabKey
  label: string
  icon: string
}

const tabList: TabItem[] = [
  { key: 'home', label: '首页', icon: '🏠' },
  { key: 'discover', label: '发现', icon: '🔍' },
  { key: 'message', label: '消息', icon: '💬' },
  { key: 'mine', label: '我的', icon: '👤' },
]

/** Tab → 路由路径映射 */
const TAB_ROUTES: Record<TabKey, string> = {
  home: '/home',
  discover: '/discover',
  message: '/message',
  mine: '/mine',
}

function handleTabChange(key: TabKey) {
  if (key === activeTab.value) return
  activeTab.value = key
  router.push(TAB_ROUTES[key])
}

// ==================== 生命周期 ====================
onMounted(() => {
  fetchAntiques()
})
</script>

<style scoped>
/* ==================== 页面容器 ==================== */
.home-page {
  min-height: 100vh;
  background: #f7f5f2;
  padding-bottom: 60px;
}

/* ==================== 搜索栏 ==================== */
.search-bar {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 16px;
  background: linear-gradient(160deg, #4a1d0a 0%, #5d2e0c 40%, #7a3d16 70%, #5d2e0c 100%);
  position: sticky;
  top: 0;
  z-index: 100;
  /* 底部装饰纹理 */
  box-shadow:
    0 2px 8px rgba(0, 0, 0, 0.15),
    inset 0 -1px 0 rgba(212, 175, 55, 0.15);
}

.search-bar::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 16px;
  right: 16px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(212, 175, 55, 0.3), transparent);
}

/* ---- 左侧标题区 ---- */
.search-bar__left {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.search-bar__icon {
  font-size: 24px;
  line-height: 1;
}

.search-bar__title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #d4af37;
  cursor: pointer;
  letter-spacing: 2px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

/* ---- 右侧搜索区（输入框 + 按钮） ---- */
.search-bar__right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-bar__input-wrap {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

/* 输入框为只读触发区：点击弹出搜索弹窗 */
.search-bar__input-wrap :deep(.el-input__inner) {
  cursor: pointer;
}

.search-bar__search-icon {
  font-size: 14px;
  opacity: 0.6;
}

.search-bar__input-wrap :deep(.el-input__wrapper) {
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  padding-left: 14px;
  transition: box-shadow 0.2s;
}

.search-bar__input-wrap :deep(.el-input__wrapper:hover) {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.search-bar__input-wrap :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(212, 175, 55, 0.3), 0 2px 8px rgba(0, 0, 0, 0.15);
}

.search-bar__input-wrap :deep(.el-input__inner) {
  font-size: 13px;
}

.search-bar__input-wrap :deep(.el-input__inner::placeholder) {
  color: #bbb;
}

.search-bar__btn {
  flex-shrink: 0;
  padding: 8px 18px;
  border: none;
  border-radius: 22px;
  background: linear-gradient(135deg, #d4af37, #b8942e);
  color: #3e1f00;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
  transition: opacity 0.2s, transform 0.1s;
}

.search-bar__btn:active {
  opacity: 0.85;
  transform: scale(0.96);
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
