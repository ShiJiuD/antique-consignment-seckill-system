<template>
  <!-- 全屏遮罩：rgba(0,0,0,0.6)，点击遮罩空白处关闭 -->
  <div class="sign-popup-mask" @click.self="handleClose">
    <!-- 弹窗：垂直水平居中，白色圆角，宽度 85vw，最大 420px -->
    <div class="sign-popup">
      <h3 class="sign-popup__title">签到成功</h3>
      <p class="sign-popup__reward">
        恭喜获得 <span class="sign-popup__num">{{ todayReward }}</span> 积分
      </p>
      <p class="sign-popup__total">
        当前总积分 <span class="sign-popup__num--brown">{{ newPoints }}</span>
      </p>
      <p class="sign-popup__days">
        已连续签到 <span class="sign-popup__num--brown">{{ continuousDays }}</span> 天
      </p>
      <button type="button" class="sign-popup__btn" @click="handleClose">
        确认领取
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue'

// ==================== Props（TS 严格约束，全部为 number） ====================

/** 签到成功弹窗展示数据（全部来自签到接口返回，无 mock 假数据） */
interface SignInPopupProps {
  /** 今日签到获得的积分 */
  todayReward: number
  /** 签到后的最新总积分 */
  newPoints: number
  /** 连续签到天数 */
  continuousDays: number
}

defineProps<SignInPopupProps>()

// ==================== 事件 ====================

const emit = defineEmits<{
  /** 弹窗关闭（父组件收到后刷新签到状态） */
  (e: 'close'): void
}>()

// ==================== 弹窗打开/关闭时锁定/恢复页面滚动 ====================

// 弹窗打开：禁止背景页面滚动
onMounted(() => {
  document.body.style.overflow = 'hidden'
})

// 弹窗关闭（含组件销毁兜底）：恢复页面滚动
onBeforeUnmount(() => {
  document.body.style.overflow = 'auto'
})

/** 点击【确认领取】或遮罩：恢复滚动并通知父组件关闭 */
function handleClose(): void {
  document.body.style.overflow = 'auto'
  emit('close')
}
</script>

<style scoped>
/* ==================== 全屏遮罩 ==================== */
.sign-popup-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6); /* 半透明全屏遮罩 */
  display: flex;
  align-items: center; /* 垂直居中 */
  justify-content: center; /* 水平居中 */
  z-index: 1000;
}

/* ==================== 弹窗主体 ==================== */
.sign-popup {
  width: 85vw;
  max-width: 420px;
  box-sizing: border-box;
  padding: 30px 24px 26px;
  background: #fff; /* 白色背景 */
  border-radius: 16px; /* 圆角 */
  text-align: center;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

/* 标题：签到成功（主棕色） */
.sign-popup__title {
  margin: 0 0 18px;
  font-size: 20px;
  font-weight: 700;
  color: #91572c; /* 主棕色 */
}

/* 恭喜获得 XX 积分（数字高亮橙色） */
.sign-popup__reward {
  margin: 0 0 14px;
  font-size: 15px;
  color: #666;
}

.sign-popup__num {
  font-size: 26px;
  font-weight: 700;
  color: #c46b2b; /* 按钮橙色 */
}

/* 当前总积分 / 已连续签到 X 天 */
.sign-popup__total,
.sign-popup__days {
  margin: 0 0 8px;
  font-size: 14px;
  color: #999;
}

.sign-popup__num--brown {
  font-weight: 600;
  color: #91572c;
}

/* ==================== 底部确认领取按钮（橙色） ==================== */
.sign-popup__btn {
  width: 100%;
  margin-top: 22px;
  padding: 13px 0;
  border: none;
  border-radius: 24px;
  background: #c46b2b; /* 按钮橙色 */
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.sign-popup__btn:active {
  opacity: 0.85;
}

/* ==================== 过渡动画（父组件用 transition 包裹，类名作用于组件根元素） ==================== */
.popup-fade-enter-active,
.popup-fade-leave-active {
  transition: opacity 0.25s ease;
}

.popup-fade-enter-active .sign-popup,
.popup-fade-leave-active .sign-popup {
  transition: transform 0.25s ease;
}

.popup-fade-enter-from,
.popup-fade-leave-to {
  opacity: 0;
}

.popup-fade-enter-from .sign-popup,
.popup-fade-leave-to .sign-popup {
  transform: translateY(16px) scale(0.96);
}
</style>
