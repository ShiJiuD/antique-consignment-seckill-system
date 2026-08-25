<template>
  <transition name="confirm-fade">
    <div v-if="visible" class="confirm-mask" @click.self="handleCancel">
      <div class="confirm-box">
        <h4 class="confirm-box__title">{{ title }}</h4>
        <p class="confirm-box__content">{{ content }}</p>
        <div class="confirm-box__actions">
          <button
            type="button"
            class="confirm-box__btn confirm-box__btn--cancel"
            @click="handleCancel"
          >
            {{ cancelText }}
          </button>
          <button
            type="button"
            class="confirm-box__btn confirm-box__btn--ok"
            @click="handleConfirm"
          >
            {{ confirmText }}
          </button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { onBeforeUnmount, watch } from 'vue'

// ==================== Props（TS 严格约束） ====================

/** 确认弹窗配置 */
interface ConfirmDialogProps {
  /** 弹窗显隐（v-model:visible） */
  visible: boolean
  /** 弹窗标题 */
  title?: string
  /** 提示内容 */
  content?: string
  /** 确认按钮文案 */
  confirmText?: string
  /** 取消按钮文案 */
  cancelText?: string
}

const props = withDefaults(defineProps<ConfirmDialogProps>(), {
  title: '提示',
  content: '',
  confirmText: '确认',
  cancelText: '取消',
})

// ==================== 事件 ====================

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'confirm'): void
  (e: 'cancel'): void
}>()

// ==================== 弹窗打开锁定背景滚动，关闭恢复 ====================

watch(
  () => props.visible,
  (val: boolean) => {
    document.body.style.overflow = val ? 'hidden' : 'auto'
  }
)

// 组件销毁兜底：恢复页面滚动
onBeforeUnmount(() => {
  document.body.style.overflow = 'auto'
})

// ==================== 交互 ====================

/** 点击确认：关闭弹窗并通知父组件执行操作 */
function handleConfirm(): void {
  emit('update:visible', false)
  emit('confirm')
}

/** 点击取消 / 遮罩：关闭弹窗 */
function handleCancel(): void {
  emit('update:visible', false)
  emit('cancel')
}
</script>

<style scoped>
/* ==================== 全屏遮罩 ==================== */
.confirm-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6); /* 半透明遮罩 */
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

/* ==================== 弹窗主体 ==================== */
.confirm-box {
  width: 78vw;
  max-width: 320px;
  box-sizing: border-box;
  padding: 22px 20px 18px;
  background: #fff; /* 白色背景 */
  border-radius: 12px; /* 圆角 */
  text-align: center;
}

.confirm-box__title {
  margin: 0 0 12px;
  font-size: 17px;
  font-weight: 700;
  color: #91572c; /* 主棕色 */
}

.confirm-box__content {
  margin: 0 0 20px;
  font-size: 14px;
  line-height: 1.6;
  color: #666;
  word-break: break-all;
}

/* ==================== 底部按钮 ==================== */
.confirm-box__actions {
  display: flex;
  gap: 12px;
}

.confirm-box__btn {
  flex: 1;
  padding: 10px 0;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
}

.confirm-box__btn:active {
  opacity: 0.85;
}

.confirm-box__btn--cancel {
  background: #f0e6d3; /* 浅国风米色 */
  color: #8b4513;
}

.confirm-box__btn--ok {
  background: #c46b2b; /* 按钮橙色 */
  color: #fff;
}

/* ==================== 过渡动画 ==================== */
.confirm-fade-enter-active,
.confirm-fade-leave-active {
  transition: opacity 0.25s ease;
}

.confirm-fade-enter-active .confirm-box,
.confirm-fade-leave-active .confirm-box {
  transition: transform 0.25s ease;
}

.confirm-fade-enter-from,
.confirm-fade-leave-to {
  opacity: 0;
}

.confirm-fade-enter-from .confirm-box,
.confirm-fade-leave-to .confirm-box {
  transform: translateY(16px) scale(0.96);
}
</style>
