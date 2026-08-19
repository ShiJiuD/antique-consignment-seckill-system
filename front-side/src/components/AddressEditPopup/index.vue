<template>
  <transition name="addr-popup-fade">
    <div v-if="visible" class="addr-popup-mask" @click.self="handleCancel">
      <div class="addr-popup">
        <!-- 弹窗头部 -->
        <div class="addr-popup__head">
          <h4 class="addr-popup__title">修改收货地址</h4>
          <span class="addr-popup__close" @click="handleCancel">✕</span>
        </div>

        <!-- 收货表单（打开时预填原收货信息，允许修改） -->
        <div class="addr-popup__form">
          <div class="addr-popup__field">
            <label class="addr-popup__label">收货人姓名</label>
            <input
              v-model.trim="form.receiverName"
              class="addr-popup__input"
              type="text"
              placeholder="请输入收货人姓名"
            />
          </div>
          <div class="addr-popup__field">
            <label class="addr-popup__label">手机号</label>
            <input
              v-model.trim="form.receiverPhone"
              class="addr-popup__input"
              type="tel"
              maxlength="11"
              placeholder="请输入手机号"
            />
          </div>
          <div class="addr-popup__field">
            <label class="addr-popup__label">收货地址</label>
            <textarea
              v-model.trim="form.receiverAddress"
              class="addr-popup__input addr-popup__input--area"
              rows="2"
              placeholder="请输入收货地址"
            ></textarea>
          </div>
        </div>

        <!-- 保存 -->
        <button
          type="button"
          class="addr-popup__submit"
          @click="handleSubmit"
        >
          {{ submitting ? '保存中...' : '保存' }}
        </button>
      </div>
    </div>
  </transition>

  <!-- 手写轻提示 -->
  <transition name="toast-fade">
    <div v-if="toastVisible" class="addr-popup__toast">{{ toastText }}</div>
  </transition>
</template>

<script setup lang="ts">
import { onBeforeUnmount, reactive, ref, watch } from 'vue'
import { updateOrderAddress } from '@/api/order'

// ==================== Props（TS 严格约束） ====================

/** 修改收货地址弹窗配置 */
interface AddressEditPopupProps {
  /** 弹窗显隐（v-model:visible） */
  visible: boolean
  /** 订单ID */
  orderId: number
  /** 原收货人姓名（预填） */
  receiverName?: string
  /** 原手机号（预填） */
  receiverPhone?: string
  /** 原收货地址（预填） */
  receiverAddress?: string
}

const props = withDefaults(defineProps<AddressEditPopupProps>(), {
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
})

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  /** 地址修改成功（父组件收到后刷新数据） */
  (e: 'success'): void
}>()

// ==================== 表单 ====================

interface AddressForm {
  receiverName: string
  receiverPhone: string
  receiverAddress: string
}

const form = reactive<AddressForm>({
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
})

/** 手机号正则：1 开头 + 3-9 + 9 位数字 */
const PHONE_REG = /^1[3-9]\d{9}$/

/** 是否提交中（防止重复提交） */
const submitting = ref(false)

// 弹窗打开：预填原收货信息 + 锁定背景滚动；关闭恢复
watch(
  () => props.visible,
  (val: boolean) => {
    if (val) {
      form.receiverName = props.receiverName
      form.receiverPhone = props.receiverPhone
      form.receiverAddress = props.receiverAddress
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
 * 提交修改地址
 * 成功：toast 提示、关闭弹窗并通知父组件刷新
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
    const res = await updateOrderAddress({
      orderId: props.orderId,
      receiverName: form.receiverName,
      receiverPhone: form.receiverPhone,
      receiverAddress: form.receiverAddress,
    })
    if (res.code === 1) {
      showToast('地址修改成功')
      emit('update:visible', false)
      emit('success')
    } else {
      // 业务失败：展示后端 msg
      showToast(res.msg || '修改失败，请稍后重试')
    }
  } catch (err) {
    console.error('修改收货地址失败：', err)
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
.addr-popup-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6); /* 半透明遮罩 */
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

/* ==================== 弹窗主体 ==================== */
.addr-popup {
  width: 88vw;
  max-width: 420px;
  box-sizing: border-box;
  padding: 20px 18px;
  background: #fff; /* 白色背景 */
  border-radius: 12px; /* 圆角 */
}

.addr-popup__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.addr-popup__title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #91572c; /* 主棕色 */
}

.addr-popup__close {
  font-size: 16px;
  color: #999;
  cursor: pointer;
  padding: 2px 6px;
}

/* ==================== 表单 ==================== */
.addr-popup__form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.addr-popup__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.addr-popup__label {
  font-size: 13px;
  color: #8b4513; /* 国风棕 */
}

.addr-popup__input {
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

.addr-popup__input:focus {
  border-color: #c46b2b;
}

.addr-popup__input--area {
  resize: none;
  font-family: inherit;
  line-height: 1.5;
}

/* ==================== 保存按钮（主按钮实色） ==================== */
.addr-popup__submit {
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

.addr-popup__submit:active {
  opacity: 0.85;
}

/* ==================== 轻提示 ==================== */
.addr-popup__toast {
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
.addr-popup-fade-enter-active,
.addr-popup-fade-leave-active {
  transition: opacity 0.25s ease;
}

.addr-popup-fade-enter-active .addr-popup,
.addr-popup-fade-leave-active .addr-popup {
  transition: transform 0.25s ease;
}

.addr-popup-fade-enter-from,
.addr-popup-fade-leave-to {
  opacity: 0;
}

.addr-popup-fade-enter-from .addr-popup,
.addr-popup-fade-leave-to .addr-popup {
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
