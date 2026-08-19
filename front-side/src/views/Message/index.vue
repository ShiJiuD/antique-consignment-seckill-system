<template>
  <div class="message-page">
    <div class="message-page__header">
      <h2>消息</h2>
    </div>
    <div class="message-page__placeholder">
      <p>💬</p>
      <p>暂无新消息</p>
    </div>
    <TabBar active="message" />
  </div>
</template>

<script setup lang="ts">
import TabBar from '@/components/TabBar.vue'
</script>

<style scoped>
.message-page {
  min-height: 100vh;
  background: #f7f5f2;
}

.message-page__header {
  padding: 12px 16px;
  background: #fff;
}

.message-page__header h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

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
