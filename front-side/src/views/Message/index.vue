<template>
  <div class="message-page">
    <!-- 顶部标题栏 -->
    <div class="message-page__header">
      <h2>古玩寄卖</h2>
    </div>

    <!-- 消息列表滚动区（上拉触底加载更多） -->
    <div class="message-page__body" @scroll="handleScroll">
      <template v-if="store.messageList.length">
        <div
          v-for="item in store.messageList"
          :key="item?.messageId"
          class="message-item"
          @click="handleItemClick(item)"
        >
          <!-- 左侧圆形头像：type=1 系统通知（土黄“系”） / type=2 订单消息（浅绿“订”） -->
          <div
            class="message-item__avatar"
            :class="item?.type === 1 ? 'message-item__avatar--sys' : 'message-item__avatar--order'"
          >
            {{ item?.type === 1 ? '系' : '订' }}
          </div>

          <!-- 右侧内容区 -->
          <div class="message-item__content">
            <!-- 上行：标题 + 未读角标 + 右上角时间 -->
            <div class="message-item__title-row">
              <span class="message-item__title">{{ item?.title }}</span>
              <!-- 未读状态：红色小圆角数字角标；已读不显示 -->
              <span v-if="item?.isRead === 0" class="message-item__badge">1</span>
              <span class="message-item__time">{{ formatTime(item?.createdTime) }}</span>
            </div>
            <!-- 下行：简短消息摘要 -->
            <p class="message-item__summary">{{ item?.content }}</p>
          </div>
        </div>

        <!-- 列表底部：加载提示 -->
        <div class="message-page__footer">
          <span v-if="store.loading">加载中...</span>
          <span v-else-if="!store.hasMore">没有更多了</span>
        </div>
      </template>

      <!-- 空状态 -->
      <div v-else-if="!store.loading" class="message-page__placeholder">
        <p>💬</p>
        <p>暂无新消息</p>
      </div>
    </div>

    <!-- 底部 Tab 导航栏：消息 tab 激活选中 -->
    <TabBar active="message" />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import TabBar from '@/components/TabBar.vue'
import { useMessageStore } from '@/stores/message'
import type { MessageItem } from '@/types/message'

const store = useMessageStore()

/**
 * 相对时间格式化：今天 / 昨天 / 周一~周六 / 当年 MM月DD日 / 跨年 YYYY-MM-DD
 * 解析失败时原样返回（可选链空值防护）
 */
function formatTime(timeStr?: string): string {
  if (!timeStr) return ''
  // 替换 - 为 /，兼容部分浏览器对 'YYYY-MM-DD HH:mm:ss' 的解析差异
  const time = new Date(timeStr.replace(/-/g, '/'))
  if (Number.isNaN(time.getTime())) return timeStr

  const now = new Date()
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const targetDayStart = new Date(time.getFullYear(), time.getMonth(), time.getDate()).getTime()
  const diffDays = Math.round((todayStart - targetDayStart) / 86_400_000)

  if (diffDays === 0) return '今天'
  if (diffDays === 1) return '昨天'
  // 一周内展示星期几
  if (diffDays > 1 && diffDays < 7) {
    const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    return weekdays[time.getDay()]
  }
  if (time.getFullYear() === now.getFullYear()) {
    return `${time.getMonth() + 1}月${time.getDate()}日`
  }
  return `${time.getFullYear()}-${time.getMonth() + 1}-${time.getDate()}`
}

/** 上拉触底：距底部不足 60px 时加载下一页 */
function handleScroll(e: Event) {
  const el = e.target as HTMLElement
  if (el.scrollHeight - el.scrollTop - el.clientHeight < 60) {
    if (store.hasMore && !store.loading) {
      store.fetchMessageList(true)
    }
  }
}

/** 点击单条消息：调用 /api/message/read 标记已读（仓库内乐观更新，失败回滚） */
function handleItemClick(item: MessageItem) {
  if (item?.messageId == null) return
  store.markRead(item.messageId)
}

onMounted(() => {
  // 建立 ws 连接（幂等；登录成功后也会建立）
  store.connect()
  // 刷新未读总数，供全局角标使用
  store.fetchUnreadCount()
  // 加载第一页消息
  store.fetchMessageList()
})
</script>

<style scoped>
.message-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  /* 整体页面背景：浅米黄色 */
  background: #f7f2e7;
}

/* 顶部标题栏 */
.message-page__header {
  flex-shrink: 0;
  padding: 12px 16px;
  background: #fff;
  background: linear-gradient(160deg, #4a1d0a, #5d2e0c, #7a3d16, #5d2e0c);
}

.message-page__header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #d4af37;
}

/* 列表滚动区（底部留出 TabBar 高度，避免内容被遮挡） */
.message-page__body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 12px 68px;
  box-sizing: border-box;
}

/* 单条消息：白色卡片条目，上下分隔 */
.message-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px 12px;
  margin-bottom: 10px;
  background: #fff;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.message-item:active {
  background: #faf8f3;
}

/* 左侧圆形头像 */
.message-item__avatar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
}

/* type=1 系统通知：圆形底色土黄色 */
.message-item__avatar--sys {
  background: #c8a05a;
}

/* type=2 订单消息：圆形底色浅绿色 */
.message-item__avatar--order {
  background: #95d5a4;
}

/* 右侧内容区 */
.message-item__content {
  flex: 1;
  min-width: 0;
}

/* 上行：标题 + 未读角标 + 时间 */
.message-item__title-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.message-item__title {
  flex: 0 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

/* 未读角标：红色小圆角数字角标，已读不显示 */
.message-item__badge {
  flex-shrink: 0;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: #f56c6c;
  color: #fff;
  font-size: 11px;
  line-height: 16px;
  text-align: center;
}

/* 条目最右上角：相对时间 */
.message-item__time {
  flex-shrink: 0;
  margin-left: auto;
  font-size: 12px;
  color: #999;
}

/* 下行：简短消息摘要 */
.message-item__summary {
  margin: 4px 0 0;
  font-size: 13px;
  line-height: 1.5;
  color: #999;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  overflow: hidden;
}

/* 列表底部加载提示 */
.message-page__footer {
  padding: 12px 0;
  text-align: center;
  font-size: 12px;
  color: #bbb;
}

/* 空状态 */
.message-page__placeholder {
  text-align: center;
  padding: 80px 0;
  color: #999;
  font-size: 16px;
}

.message-page__placeholder p {
  margin: 4px 0;
}
</style>
