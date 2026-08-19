<template>
  <transition name="popup-fade">
    <div v-if="visible" class="search-popup">
      <!-- ==================== 顶部搜索栏：输入框 + 取消 ==================== -->
      <div class="search-popup__bar">
        <div class="search-popup__input-wrap">
          <el-input
            ref="inputRef"
            v-model="keyword"
            placeholder="搜索藏品、年代、材质..."
            size="default"
            clearable
            @keyup.enter="handleSearch"
            @input="handleInput"
          >
            <template #prefix>
              <span class="search-popup__search-icon">🔍</span>
            </template>
          </el-input>
        </div>
        <span class="search-popup__cancel" @click="handleClose">取消</span>
      </div>

      <!-- ==================== 默认态：历史搜索 + 热门推荐 ==================== -->
      <div v-if="!hasSearched" class="search-popup__body">
        <!-- 历史搜索：localStorage 读取，支持一键清空 -->
        <div v-if="historyList.length > 0" class="search-section">
          <div class="search-section__header">
            <h3 class="search-section__title">🕐 历史搜索</h3>
            <span class="search-section__clear" @click="handleClearHistory">🗑️ 清空</span>
          </div>
          <div class="search-section__tags">
            <span
              v-for="(word, index) in historyList"
              :key="index"
              class="search-section__tag"
              @click="handleTagClick(word)"
            >
              {{ word }}
            </span>
          </div>
        </div>

        <!-- 热门推荐：前端静态词数组 -->
        <div class="search-section">
          <div class="search-section__header">
            <h3 class="search-section__title">🔥 热门推荐</h3>
          </div>
          <div class="search-section__tags">
            <span
              v-for="word in hotWords"
              :key="word"
              class="search-section__tag search-section__tag--hot"
              @click="handleTagClick(word)"
            >
              {{ word }}
            </span>
          </div>
        </div>
      </div>

      <!-- ==================== 搜索态：结果卡片列表 ==================== -->
      <div v-else class="search-popup__results">
        <div v-if="loading" class="search-popup__loading">
          <el-skeleton :rows="3" animated />
        </div>

        <div v-else-if="resultList.length === 0" class="search-popup__empty">
          <span class="search-popup__empty-icon">🔍</span>
          <p class="search-popup__empty-text">未找到与「{{ keyword }}」相关的藏品</p>
        </div>

        <template v-else>
          <div class="search-popup__grid">
            <AntiqueCard
              v-for="item in resultList"
              :key="item?.id"
              :antique="item"
              @click="handleResultClick"
            />
          </div>
          <div v-if="hasMore" class="search-popup__more">
            <button type="button" class="search-popup__more-btn" @click="handleLoadMore">
              加载更多
            </button>
          </div>
        </template>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElInput, ElMessage } from 'element-plus'
import { searchAntique, type AntiqueItem } from '@/api/antique'
import AntiqueCard from '@/components/AntiqueCard.vue'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
}>()

const router = useRouter()

// ==================== 历史搜索（localStorage 持久化） ====================

/** localStorage 存储键 */
const HISTORY_KEY = 'antique_search_history'
/** 最多保留的历史条数 */
const HISTORY_MAX = 10

const historyList = ref<string[]>([])

/** 从 localStorage 读取历史搜索（解析失败兜底为空数组） */
function loadHistory(): void {
  try {
    const raw = localStorage.getItem(HISTORY_KEY)
    const parsed: unknown = raw ? JSON.parse(raw) : []
    historyList.value = Array.isArray(parsed)
      ? parsed.filter((item): item is string => typeof item === 'string').slice(0, HISTORY_MAX)
      : []
  } catch (err) {
    console.error('读取历史搜索失败：', err)
    historyList.value = []
  }
}

/** 保存关键词到历史搜索：去重、最新在前、截断上限 */
function saveHistory(keyword: string): void {
  const kw = keyword.trim()
  if (!kw) return
  const next = [kw, ...historyList.value.filter((item) => item !== kw)].slice(0, HISTORY_MAX)
  historyList.value = next
  try {
    localStorage.setItem(HISTORY_KEY, JSON.stringify(next))
  } catch (err) {
    console.error('保存历史搜索失败：', err)
  }
}

/** 清空历史搜索 */
function handleClearHistory(): void {
  historyList.value = []
  try {
    localStorage.removeItem(HISTORY_KEY)
  } catch (err) {
    console.error('清空历史搜索失败：', err)
  }
}

// ==================== 热门推荐（前端静态词数组） ====================

const hotWords: string[] = [
  '青花瓷',
  '汝窑',
  '和田玉',
  '翡翠',
  '紫砂壶',
  '宣德炉',
  '铜镜',
  '名家字画',
  '唐三彩',
  '文房四宝',
]

// ==================== 搜索状态 ====================

const inputRef = ref<InstanceType<typeof ElInput>>()
const keyword = ref('')
/** 是否已进入搜索态（true 显示结果区，false 显示历史/热门） */
const hasSearched = ref(false)
const resultList = ref<AntiqueItem[]>([])
const loading = ref(false)

const page = ref(1)
/** 每页数量（后端 AntiqueSearchQueryDTO 参数名为 size，范围 1-50） */
const pageSize = 10
const total = ref(0)

/** 是否还有更多结果可加载 */
const hasMore = computed(() => resultList.value.length < total.value)

// ==================== 搜索请求 ====================

/**
 * 调用 GET /api/antique/search 拉取指定页结果
 * append 为 true 时追加到现有列表（加载更多），否则替换列表（新搜索）
 */
async function fetchResults(append: boolean): Promise<void> {
  loading.value = true
  try {
    const res = await searchAntique({ keyword: keyword.value.trim(), page: page.value, size: pageSize })
    // 后端统一返回 { code, msg, data }，data 为 { list, total, page, size }
    const list = res?.data?.list ?? []
    const listTotal = res?.data?.total ?? 0
    resultList.value = append ? [...resultList.value, ...list] : list
    total.value = listTotal
  } catch (err) {
    console.error('搜索藏品失败：', err)
    ElMessage.error('搜索失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

/** 触发新搜索：校验关键词 → 重置分页 → 拉取第一页 → 写入历史 */
function handleSearch(): void {
  const kw = keyword.value?.trim() ?? ''
  if (!kw) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  hasSearched.value = true
  page.value = 1
  void fetchResults(false)
  saveHistory(kw)
}

/** 加载更多：页码 +1 并追加结果 */
function handleLoadMore(): void {
  if (loading.value || !hasMore.value) return
  page.value += 1
  void fetchResults(true)
}

/** 输入内容清空时回到默认态（历史 + 热门） */
function handleInput(value: string): void {
  if (!value?.trim()) {
    hasSearched.value = false
    resultList.value = []
  }
}

/** 点击历史词 / 热词：回填输入框并直接搜索 */
function handleTagClick(word: string): void {
  keyword.value = word ?? ''
  handleSearch()
}

/** 点击结果卡片：关闭弹窗并跳转藏品详情 */
function handleResultClick(item: AntiqueItem): void {
  handleClose()
  router.push(`/antique/${item?.id}`)
}

// ==================== 弹窗开合 ====================

function handleClose(): void {
  emit('update:visible', false)
}

/**
 * 弹窗打开时：重新读取历史 → 复位状态 → 输入框自动聚焦 → 锁定页面滚动
 * 弹窗关闭时：恢复页面滚动
 */
watch(
  () => props.visible,
  (val) => {
    if (val) {
      loadHistory()
      keyword.value = ''
      hasSearched.value = false
      resultList.value = []
      page.value = 1
      total.value = 0
      document.body.style.overflow = 'hidden'
      void nextTick(() => {
        inputRef.value?.focus()
      })
    } else {
      document.body.style.overflow = ''
    }
  }
)
</script>

<style scoped>
/* ==================== 全屏弹层 ==================== */
.search-popup {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: #f7f5f2;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

/* 淡入淡出过渡 */
.popup-fade-enter-active,
.popup-fade-leave-active {
  transition: opacity 0.2s;
}

.popup-fade-enter-from,
.popup-fade-leave-to {
  opacity: 0;
}

/* ==================== 顶部搜索栏 ==================== */
.search-popup__bar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: linear-gradient(160deg, #4a1d0a 0%, #5d2e0c 40%, #7a3d16 70%, #5d2e0c 100%);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.search-popup__input-wrap {
  flex: 1;
  min-width: 0;
}

.search-popup__input-wrap :deep(.el-input__wrapper) {
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  padding-left: 14px;
}

.search-popup__input-wrap :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(212, 175, 55, 0.3), 0 2px 8px rgba(0, 0, 0, 0.15);
}

.search-popup__search-icon {
  font-size: 14px;
  opacity: 0.6;
}

.search-popup__cancel {
  flex-shrink: 0;
  font-size: 15px;
  font-weight: 600;
  color: #d4af37;
  cursor: pointer;
  white-space: nowrap;
}

.search-popup__cancel:active {
  opacity: 0.7;
}

/* ==================== 历史搜索 / 热门推荐区域 ==================== */
.search-popup__body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.search-section__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.search-section__title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #333;
}

.search-section__clear {
  font-size: 12px;
  color: #999;
  cursor: pointer;
}

.search-section__clear:active {
  color: #8b4513;
}

.search-section__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.search-section__tag {
  padding: 6px 14px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #eee;
  font-size: 13px;
  color: #555;
  cursor: pointer;
  transition: color 0.15s, border-color 0.15s, background 0.15s;
}

.search-section__tag:active {
  color: #8b4513;
  border-color: #d4af37;
  background: #fdf8ec;
}

.search-section__tag--hot {
  background: #fdf8ec;
  border-color: #f0e2c0;
  color: #a0522d;
}

/* ==================== 搜索结果区域 ==================== */
.search-popup__results {
  flex: 1;
  padding: 12px 16px;
}

.search-popup__grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.search-popup__loading {
  padding: 16px 0;
}

.search-popup__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 80px 16px;
}

.search-popup__empty-icon {
  font-size: 44px;
  opacity: 0.5;
}

.search-popup__empty-text {
  margin: 0;
  font-size: 14px;
  color: #999;
}

/* 加载更多 */
.search-popup__more {
  display: flex;
  justify-content: center;
  padding: 16px 0 8px;
}

.search-popup__more-btn {
  padding: 8px 32px;
  border: 1px solid #d4af37;
  border-radius: 18px;
  background: #fff;
  color: #8b4513;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}

.search-popup__more-btn:active {
  background: #fdf8ec;
}
</style>
