<template>
  <transition name="sign-dialog-fade">
    <div v-if="modelValue" class="sign-dialog-overlay" @click.self="handleConfirm">
      <div class="sign-dialog-card">
        <div class="sign-dialog__icon">🎉</div>
        <h3 class="sign-dialog__title">签到成功</h3>
        <p class="sign-dialog__reward">
          恭喜获得 <span class="sign-dialog__points">{{ rewardPoints ?? 0 }}</span> 积分
        </p>
        <div class="sign-dialog__divider"></div>
        <div class="sign-dialog__info">
          <p class="sign-dialog__info-line">
            当前连续签到 <span class="sign-dialog__strong">{{ continuousDays ?? 0 }}</span> 天
          </p>
          <p v-if="daysToExtraReward > 0" class="sign-dialog__info-line">
            再连续签到 <span class="sign-dialog__strong">{{ daysToExtraReward ?? 0 }}</span> 天
            即可获得额外奖励
          </p>
          <p v-else class="sign-dialog__info-line sign-dialog__info-line--extra">
            今日已达成额外奖励，明日继续加油！
          </p>
        </div>
        <button class="sign-dialog__confirm" @click="handleConfirm">确认领取</button>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
const props = defineProps<{
  /** 弹窗显示状态（v-model） */
  modelValue: boolean
  /** 本次签到获得的积分 */
  rewardPoints: number
  /** 签到后的连续签到天数 */
  continuousDays: number
  /** 距离额外奖励还需要的天数（0 表示已达成额外奖励） */
  daysToExtraReward: number
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'confirm'): void
}>()

/** 点击【确认领取】/ 点击遮罩：关闭弹窗并通知父组件 */
function handleConfirm() {
  emit('update:modelValue', false)
  emit('confirm')
}
</script>

<style scoped>
/* ==================== 遮罩层 ==================== */
.sign-dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 24px;
}

/* ==================== 弹窗卡片 ==================== */
.sign-dialog-card {
  width: 100%;
  max-width: 320px;
  background: #fff;
  border-radius: 16px;
  padding: 28px 24px 24px;
  text-align: center;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}

.sign-dialog__icon {
  font-size: 44px;
  line-height: 1;
  margin-bottom: 10px;
}

.sign-dialog__title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 700;
  color: #333;
}

.sign-dialog__reward {
  margin: 0;
  font-size: 14px;
  color: #666;
}

.sign-dialog__points {
  font-size: 22px;
  font-weight: 700;
  color: #f2701d;
}

.sign-dialog__divider {
  height: 1px;
  background: #f0f0f0;
  margin: 16px 0;
}

.sign-dialog__info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
}

.sign-dialog__info-line {
  margin: 0;
  font-size: 13px;
  color: #999;
}

.sign-dialog__info-line--extra {
  color: #f2701d;
}

.sign-dialog__strong {
  font-weight: 700;
  color: #8b4513;
}

/* ==================== 确认按钮（橙色） ==================== */
.sign-dialog__confirm {
  width: 100%;
  padding: 13px 0;
  border: none;
  border-radius: 24px;
  background: linear-gradient(135deg, #ff9a3d, #f2701d);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.1s;
}

.sign-dialog__confirm:active {
  opacity: 0.85;
  transform: scale(0.97);
}

/* ==================== 过渡动画 ==================== */
.sign-dialog-fade-enter-active,
.sign-dialog-fade-leave-active {
  transition: opacity 0.25s ease;
}

.sign-dialog-fade-enter-active .sign-dialog-card,
.sign-dialog-fade-leave-active .sign-dialog-card {
  transition: transform 0.25s ease;
}

.sign-dialog-fade-enter-from,
.sign-dialog-fade-leave-to {
  opacity: 0;
}

.sign-dialog-fade-enter-from .sign-dialog-card,
.sign-dialog-fade-leave-to .sign-dialog-card {
  transform: translateY(16px) scale(0.96);
}
</style>
