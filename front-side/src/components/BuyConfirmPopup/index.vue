<template>
  <transition name="buy-popup-fade">
    <div v-if="visible" class="buy-popup-mask" @click.self="handleCancel">
      <div class="buy-popup">
        <!-- 弹窗头部 -->
        <div class="buy-popup__head">
          <h4 class="buy-popup__title">确认购买</h4>
          <span class="buy-popup__close" @click="handleCancel">✕</span>
        </div>

        <!-- 购买说明（文案写死） -->
        <p class="buy-popup__text">
          您即将购买「{{ title }}」，价格
          <span class="buy-popup__price">¥{{ price.toLocaleString() }}</span>
          ，请在16分钟内完成支付
        </p>

        <!-- 收货表单（允许手动修改，假数据也可提交） -->
        <div class="buy-popup__form">
          <div class="buy-popup__field">
            <label class="buy-popup__label">收货人姓名</label>
            <input
              v-model.trim="form.receiverName"
              class="buy-popup__input"
              type="text"
              placeholder="请输入收货人姓名"
            />
          </div>
          <div class="buy-popup__field">
            <label class="buy-popup__label">手机号</label>
            <input
              v-model.trim="form.receiverPhone"
              class="buy-popup__input"
              type="tel"
              maxlength="11"
              placeholder="请输入手机号"
            />
          </div>
          <div class="buy-popup__field">
            <label class="buy-popup__label">收货地址</label>
            <textarea
              v-model.trim="form.receiverAddress"
              class="buy-popup__input buy-popup__input--area"
              rows="2"
              placeholder="请输入收货地址"
            ></textarea>
          </div>
          <div class="buy-popup__field">
            <label class="buy-popup__label">
              买家备注 <span class="buy-popup__optional">（选填）</span>
            </label>
            <textarea
              v-model.trim="form.buyerRemark"
              class="buy-popup__input buy-popup__input--area"
              rows="2"
              placeholder="选填，给卖家留言"
            ></textarea>
          </div>
        </div>

        <!-- 提交订单 -->
        <button
          type="button"
          class="buy-popup__submit"
          @click="handleSubmit"
        >
          {{ submitting ? '提交中...' : '提交订单' }}
        </button>
      </div>
    </div>
  </transition>

  <!-- 手写轻提示 -->
  <transition name="toast-fade">
    <div v-if="toastVisible" class="buy-popup__toast">{{ toastText }}</div>
  </transition>
</template>

<script setup lang="ts">
import { onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { createOrder } from '@/api/order'

// ==================== Props（TS 严格约束） ====================

/** 确认购买弹窗配置 */
interface BuyConfirmPopupProps {
  /** 弹窗显隐（v-model:visible） */
  visible: boolean
  /** 藏品ID（创建订单入参） */
  antiqueId: number
  /** 藏品名称（文案展示） */
  title: string
  /** 藏品价格（文案展示） */
  price: number
}

const props = defineProps<BuyConfirmPopupProps>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
}>()

const router = useRouter()

// ==================== 表单 ====================

/** 收货表单（允许手动修改，打开时预填本地昵称/手机号） */
interface BuyForm {
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  buyerRemark: string
}

const form = reactive<BuyForm>({
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
  buyerRemark: '',
})

/** 手机号正则：1 开头 + 3-9 + 9 位数字 */
const PHONE_REG = /^1[3-9]\d{9}$/

/** 是否提交中（防止重复提交） */
const submitting = ref(false)

// 弹窗打开：预填本地昵称/手机号 + 锁定背景滚动；关闭恢复
watch(
  () => props.visible,
  (val: boolean) => {
    if (val) {
      if (!form.receiverName) form.receiverName = localStorage.getItem('nickname') || ''
      if (!form.receiverPhone) form.receiverPhone = localStorage.getItem('phone') || ''
    }
    document.body.style.overflow = val ? 'hidden' : 'auto'
  }
)

onBeforeUnmount(() => {
  document.body.style.overflow = 'auto'
})

// ==================== 手写轻提示 ====================

const toastText = ref('')
const toastVisible = ref(false)
let toastTimer: number | undefined

function showToast(msg: string): void {
  toastText.value = msg
  toastVisible.value = true
  window.clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => {
    toastVisible.value = false
  }, 2000)
}

// ==================== 校验与提交 ====================

/** 前端简单校验：姓名/手机号/地址不能为空，手机号正则校验 */
function validate(): string {
  if (!form.receiverName) return '请输入收货人姓名'
  if (!form.receiverPhone) return '请输入手机号'
  if (!PHONE_REG.test(form.receiverPhone)) return '手机号格式不正确'
  if (!form.receiverAddress) return '请输入收货地址'
  return ''
}

/**
 * 提交创建订单
 * 成功：关闭弹窗并跳转模拟支付页（携带 orderId、payDeadline）
 * 失败：展示后端返回 msg 提示
 */
async function handleSubmit(): Promise<void> {
  const errMsg = validate()
  if (errMsg) {
    showToast(errMsg)
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    const res = await createOrder({
      antiqueId: props.antiqueId,
      receiverName: form.receiverName,
      receiverPhone: form.receiverPhone,
      receiverAddress: form.receiverAddress,
      buyerRemark: form.buyerRemark || undefined,
    })
    if (res.code === 1) {
      emit('update:visible', false)
      router.push({
        path: '/order/pay',
        query: {
          orderId: String(res.data.orderId),
          payDeadline: res.data.payDeadline,
        },
      })
    } else {
      // 业务失败：展示后端 msg
      showToast(res.msg || '创建订单失败')
    }
  } catch (err) {
    console.error('创建订单失败：', err)
    showToast('网络异常，请稍后重试')
  } finally {
    submitting.value = false
  }
}

/** 点击关闭按钮 / 遮罩：关闭弹窗 */
function handleCancel(): void {
  emit('update:visible', false)
}
</script>

<style scoped>
/* ==================== 全屏遮罩 ==================== */
.buy-popup-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6); /* 半透明遮罩 */
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

/* ==================== 弹窗主体 ==================== */
.buy-popup {
  width: 88vw;
  max-width: 420px;
  max-height: 80vh;
  overflow-y: auto;
  box-sizing: border-box;
  padding: 20px 18px;
  background: #fff; /* 白色背景 */
  border-radius: 12px; /* 圆角 */
}

.buy-popup__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.buy-popup__title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #91572c; /* 主棕色 */
}

.buy-popup__close {
  font-size: 16px;
  color: #999;
  cursor: pointer;
  padding: 2px 6px;
}

/* 购买说明（文案写死） */
.buy-popup__text {
  margin: 0 0 16px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #fdf3ea;
  font-size: 13px;
  line-height: 1.6;
  color: #666;
}

.buy-popup__price {
  font-weight: 700;
  color: #c0392b; /* 价格红色 */
}

/* ==================== 表单 ==================== */
.buy-popup__form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.buy-popup__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.buy-popup__label {
  font-size: 13px;
  color: #8b4513; /* 国风棕 */
}

.buy-popup__optional {
  font-size: 12px;
  color: #bbb;
}

.buy-popup__input {
  box-sizing: border-box;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #eee;
  border-radius: 8px;
  background: #f8f3eb; /* 米白底 */
  font-size: 14px;
  color: #333;
  outline: none;
  transition: border-color 0.2s;
}

.buy-popup__input:focus {
  border-color: #c46b2b;
}

.buy-popup__input--area {
  resize: none;
  font-family: inherit;
  line-height: 1.5;
}

/* ==================== 提交按钮（主按钮实色） ==================== */
.buy-popup__submit {
  width: 100%;
  margin-top: 18px;
  padding: 12px 0;
  border: none;
  border-radius: 22px;
  background: #c46b2b; /* 按钮橙色 */
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.buy-popup__submit:active {
  opacity: 0.85;
}

/* ==================== 轻提示 ==================== */
.buy-popup__toast {
  position: fixed;
  left: 50%;
  top: 45%;
  transform: translate(-50%, -50%);
  max-width: 70vw;
  padding: 10px 20px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  font-size: 14px;
  text-align: center;
  z-index: 2000;
}

/* ==================== 过渡动画 ==================== */
.buy-popup-fade-enter-active,
.buy-popup-fade-leave-active {
  transition: opacity 0.25s ease;
}

.buy-popup-fade-enter-active .buy-popup,
.buy-popup-fade-leave-active .buy-popup {
  transition: transform 0.25s ease;
}

.buy-popup-fade-enter-from,
.buy-popup-fade-leave-to {
  opacity: 0;
}

.buy-popup-fade-enter-from .buy-popup,
.buy-popup-fade-leave-to .buy-popup {
  transform: translateY(16px) scale(0.96);
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: opacity 0.25s;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
}
</style>
