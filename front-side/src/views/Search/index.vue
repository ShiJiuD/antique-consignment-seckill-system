<template>
  <div class="search-page">
    <!-- 顶部搜索栏 -->
    <div class="search-page__bar">
      <span class="search-page__back" @click="goBack">←</span>
      <div class="search-page__input-wrap">
        <el-input
          v-model="keyword"
          placeholder="搜索藏品、年代、材质..."
          size="default"
          clearable
          @keyup.enter="doSearch"
        />
      </div>
      <span class="search-page__btn" @click="doSearch">搜索</span>
    </div>

    <!-- 搜索结果 -->
    <div v-if="loading" class="search-page__loading">
      <el-skeleton :rows="3" animated />
    </div>
    <div v-else-if="list.length === 0" class="search-page__empty">
      <p>暂无搜索结果</p>
    </div>
    <div v-else class="search-page__grid">
      <AntiqueCard
        v-for="item in list"
        :key="item.id"
        :antique="item"
        @click="goDetail(item.id)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchAntique, type AntiqueItem } from '@/api/antique'
import AntiqueCard from '@/components/AntiqueCard.vue'

const route = useRoute()
const router = useRouter()

const keyword = ref('')
const list = ref<AntiqueItem[]>([])
const loading = ref(false)

function goBack() {
  router.back()
}

function goDetail(id: number) {
  router.push(`/antique/${id}`)
}

async function doSearch() {
  const kw = keyword.value.trim()
  if (!kw) return
  loading.value = true
  try {
    const res = await searchAntique({ keyword: kw, page: 1, size: 20 })
    list.value = (res as any).data?.list ?? res.list ?? []
  } catch (err) {
    console.error('搜索失败:', err)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  const q = route.query.keyword as string
  if (q) {
    keyword.value = q
    doSearch()
  }
})
</script>

<style scoped>
.search-page {
  min-height: 100vh;
  background: #f7f5f2;
}

.search-page__bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: linear-gradient(135deg, #5d2e0c, #8b4513);
  position: sticky;
  top: 0;
  z-index: 100;
}

.search-page__back {
  font-size: 20px;
  color: #d4af37;
  cursor: pointer;
  flex-shrink: 0;
}

.search-page__input-wrap {
  flex: 1;
  min-width: 0;
}

.search-page__input-wrap :deep(.el-input__wrapper) {
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.95);
}

.search-page__btn {
  font-size: 14px;
  color: #d4af37;
  font-weight: 600;
  cursor: pointer;
  flex-shrink: 0;
}

.search-page__grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  padding: 12px 16px;
}

.search-page__loading {
  padding: 16px;
}

.search-page__empty {
  text-align: center;
  padding: 60px 0;
  color: #999;
  font-size: 14px;
}
</style>
