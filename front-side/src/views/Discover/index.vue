<template>
  <div class="discover-page">
    <!-- 顶部：标题 + 搜索栏（点击弹出全屏搜索弹窗，与首页一致） -->
    <div class="discover-page__header">
      <h2>古玩寄卖</h2>
      <div class="search-bar">
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

    <!-- 热门藏品（复用首页 AntiqueCard 卡片，上拉分页加载更多） -->
    <div class="discover-page__section">
      <h4 class="discover-page__section-title">🔥 热门藏品</h4>

      <!-- 首次加载骨架 -->
      <div v-if="hotLoading && hotList.length === 0" class="hot-section__loading">
        <el-skeleton :rows="3" animated />
      </div>

      <!-- 无数据 -->
      <div v-else-if="hotList.length === 0" class="hot-section__empty">
        <p>暂无藏品</p>
      </div>

      <!-- 卡片网格 + 上拉加载状态 -->
      <template v-else>
        <div class="hot-section__grid">
          <AntiqueCard
            v-for="item in hotList"
            :key="item?.id"
            :antique="item"
            @click="goDetail"
          />
        </div>
        <div class="hot-section__footer">
          <span v-if="hotLoading" class="hot-section__status">加载中...</span>
          <span v-else-if="hasMore" class="hot-section__status">上拉加载更多</span>
          <span v-else class="hot-section__status">没有更多了</span>
        </div>
      </template>
    </div>

    <div class="discover-page__placeholder--bottom"></div>

    <!-- 全屏搜索弹窗（搜索逻辑全部在弹窗内完成，不跳转页面） -->
    <SearchPopup v-model:visible="searchPopupVisible" />

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
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getAntiqueList, type AntiqueItem } from '@/api/antique'
import AntiqueCard from '@/components/AntiqueCard.vue'
import SearchPopup from '@/components/SearchPopup/index.vue'

const router = useRouter()

// ==================== 搜索弹窗 ====================

/** 全屏搜索弹窗显隐 */
const searchPopupVisible = ref(false)

/** 点击搜索框 / 搜索按钮：弹出全屏搜索弹窗 */
function openSearchPopup() {
  searchPopupVisible.value = true
}

// ==================== 分类浏览 ====================

const categories = [
  { id: 1, name: '瓷器', icon: '🏺' },
  { id: 2, name: '字画', icon: '🖼️' },
  { id: 3, name: '玉器', icon: '💎' },
  { id: 4, name: '铜器', icon: '🔔' },
  { id: 5, name: '杂项', icon: '📿' },
]

// ==================== 热门藏品（上拉分页加载） ====================

const hotList = ref<AntiqueItem[]>([])
const hotLoading = ref(false)
const hotPage = ref(1)
const hotTotal = ref(0)
/** 每页数量（后端列表接口参数为 size，范围 1-50） */
const PAGE_SIZE = 10

/** 是否还有更多热门藏品可加载 */
const hasMore = computed(() => hotList.value.length < hotTotal.value)

/**
 * 拉取热门藏品列表（GET /api/antique/list，isHot=1）
 * append 为 true 时追加到现有列表（上拉加载更多），否则替换列表（首次加载）
 */
async function fetchHotAntiques(append: boolean): Promise<void> {
  if (hotLoading.value) return
  hotLoading.value = true
  let ok = false
  try {
    const res = await getAntiqueList({ isHot: 1, page: hotPage.value, size: PAGE_SIZE })
    // 后端统一返回 { code, msg, data }，data 为 { list, total, page, size }
    const list = res?.data?.list ?? []
    hotList.value = append ? [...hotList.value, ...list] : list
    hotTotal.value = res?.data?.total ?? 0
    ok = true
  } catch (err) {
    console.error('获取热门藏品失败：', err)
  } finally {
    hotLoading.value = false
    // 加载成功后若内容仍不足一屏，自动补拉下一页，保证上拉加载可触发
    if (ok) {
      void nextTick(() => {
        if (hasMore.value && document.documentElement.scrollHeight <= window.innerHeight) {
          loadMore()
        }
      })
    }
  }
}

/** 上拉加载更多：页码 +1 并追加结果 */
function loadMore(): void {
  if (hotLoading.value || !hasMore.value) return
  hotPage.value += 1
  void fetchHotAntiques(true)
}

/** 上拉触底检测：距页面底部 200px 内触发加载更多 */
function handleScroll(): void {
  if (hotLoading.value || !hasMore.value) return
  const { scrollTop } = document.documentElement
  if (scrollTop + window.innerHeight >= document.documentElement.scrollHeight - 200) {
    loadMore()
  }
}

/** 点击藏品卡片跳转详情页 */
function goDetail(item: AntiqueItem) {
  router.push(`/antique/${item?.id}`)
}

// ==================== 底部 Tab 栏 ====================

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

// ==================== 生命周期 ====================

onMounted(() => {
  void fetchHotAntiques(false)
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.discover-page{
  min-height: 100vh;
  background: #f7f5f2;
  padding-bottom: 70px;
}
.search-bar {
  padding-top: 6px;
  display: flex;
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

.discover-page__header {
  padding: 16px 16px 16px;
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

/* ==================== 热门藏品区域 ==================== */

.hot-section__loading {
  padding: 8px 0;
}

.hot-section__empty {
  text-align: center;
  padding: 40px 0;
  color: #999;
  font-size: 14px;
}

.hot-section__grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.hot-section__footer {
  display: flex;
  justify-content: center;
  padding: 14px 0 4px;
}

.hot-section__status {
  font-size: 12px;
  color: #999;
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
