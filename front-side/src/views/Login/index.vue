<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()

// ==================== Tab 切换 ====================
const activeTab = ref<'sms' | 'pwd'>('sms')

// ==================== 短信登录表单 ====================
const smsForm = reactive({
  phone: '',
  code: '',
})

// ==================== 密码登录表单 ====================
const pwdForm = reactive({
  phone: '',
  password: '',
})

// ==================== 验证码倒计时 ====================
const countdown = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

const countdownText = computed(() => {
  return countdown.value > 0 ? `${countdown.value}s后重发` : '获取验证码'
})

const sendCode = async () => {
  // 校验手机号
  if (!/^1[3-9]\d{9}$/.test(smsForm.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }

  // TODO: 调用发送验证码接口
  ElMessage.success('验证码已发送')
  countdown.value = 60
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer!)
      timer = null
    }
  }, 1000)
}

// ==================== 协议勾选 ====================
const agreed = ref(false)

// ==================== 登录加载态 ====================
const loading = ref(false)

// ==================== 校验 ====================
const validate = (): boolean => {
  if (activeTab.value === 'sms') {
    if (!smsForm.phone) {
      ElMessage.warning('请输入手机号')
      return false
    }
    if (!/^1[3-9]\d{9}$/.test(smsForm.phone)) {
      ElMessage.warning('手机号格式不正确')
      return false
    }
    if (!smsForm.code) {
      ElMessage.warning('请输入验证码')
      return false
    }
    if (!/^\d{6}$/.test(smsForm.code)) {
      ElMessage.warning('验证码必须为6位数字')
      return false
    }
  } else {
    if (!pwdForm.phone) {
      ElMessage.warning('请输入手机号')
      return false
    }
    if (!/^1[3-9]\d{9}$/.test(pwdForm.phone)) {
      ElMessage.warning('手机号格式不正确')
      return false
    }
    if (!pwdForm.password) {
      ElMessage.warning('请输入密码')
      return false
    }
  }

  if (!agreed.value) {
    ElMessage.warning('请先阅读并同意相关协议')
    return false
  }

  return true
}

// ==================== 登录 ====================
const handleLogin = async () => {
  if (!validate()) return

  loading.value = true
  try {
    // TODO: 替换为真实登录接口
    await new Promise((resolve) => setTimeout(resolve, 1500))

    const mockToken = 'mock_token_' + Date.now()
    localStorage.setItem('token', mockToken)

    ElMessage.success('登录成功')
    router.push('/home')
  } catch {
    ElMessage.error('登录失败，请重试')
  } finally {
    loading.value = false
  }
}

// ==================== 组件卸载清除定时器 ====================
import { onUnmounted } from 'vue'
onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="login-overlay">
    <div class="login-card">
      <!-- 顶部标题 -->
      <h2 class="login-title">登录</h2>

      <!-- Tab 切换 -->
      <div class="tab-bar">
        <span
          :class="['tab-item', { active: activeTab === 'sms' }]"
          @click="activeTab = 'sms'"
        >
          短信登录
        </span>
        <span
          :class="['tab-item', { active: activeTab === 'pwd' }]"
          @click="activeTab = 'pwd'"
        >
          密码登录
        </span>
      </div>

      <!-- 短信登录面板 -->
      <div v-show="activeTab === 'sms'" class="form-panel">
        <div class="input-group">
          <span class="prefix">+86</span>
          <input
            v-model="smsForm.phone"
            type="tel"
            maxlength="11"
            placeholder="请输入手机号"
            class="form-input"
          />
        </div>

        <div class="input-group">
          <input
            v-model="smsForm.code"
            type="text"
            maxlength="6"
            placeholder="请输入验证码"
            class="form-input flex-1"
          />
          <button
            class="code-btn"
            :disabled="countdown > 0"
            @click="sendCode"
          >
            {{ countdownText }}
          </button>
        </div>
      </div>

      <!-- 密码登录面板 -->
      <div v-show="activeTab === 'pwd'" class="form-panel">
        <div class="input-group">
          <span class="prefix">+86</span>
          <input
            v-model="pwdForm.phone"
            type="tel"
            maxlength="11"
            placeholder="请输入手机号"
            class="form-input"
          />
        </div>

        <div class="input-group">
          <input
            v-model="pwdForm.password"
            type="password"
            placeholder="请输入密码"
            class="form-input"
          />
        </div>
      </div>

      <!-- 协议勾选 -->
      <div class="agreement" @click="agreed = !agreed">
        <span :class="['checkbox', { checked: agreed }]">
          <svg v-if="agreed" viewBox="0 0 24 24" fill="none" class="check-icon">
            <path d="M5 13l4 4L19 7" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </span>
        <span class="agreement-text">
          您已阅读并同意<span class="link">《用户协议》</span><span class="link">《隐私政策》</span><span class="link">《软件许可协议》</span>
        </span>
      </div>

      <!-- 登录按钮 -->
      <button
        class="login-btn"
        :disabled="loading"
        @click="handleLogin"
      >
        <span v-if="loading" class="spinner"></span>
        {{ loading ? '登录中...' : '登录' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
/* ==================== 遮罩层 ==================== */
.login-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 24px;
}

/* ==================== 卡片 ==================== */
.login-card {
  width: 100%;
  max-width: 360px;
  background: #fff;
  border-radius: 16px;
  padding: 32px 24px 28px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  animation: fadeIn 0.25s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(16px) scale(0.96); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

/* ==================== 标题 ==================== */
.login-title {
  text-align: center;
  font-size: 22px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 24px;
}

/* ==================== Tab 栏 ==================== */
.tab-bar {
  display: flex;
  border-bottom: 2px solid #f0f0f0;
  margin-bottom: 24px;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 10px 0 12px;
  font-size: 15px;
  color: #999;
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
  user-select: none;
}

.tab-item.active {
  color: #c9a96e;
  font-weight: 600;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 50%;
  transform: translateX(-50%);
  width: 32px;
  height: 2px;
  background: #c9a96e;
  border-radius: 1px;
}

/* ==================== 表单面板 ==================== */
.form-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* ==================== 输入组 ==================== */
.input-group {
  display: flex;
  align-items: center;
  background: #f7f7f7;
  border-radius: 10px;
  overflow: hidden;
  transition: background 0.2s;
}

.input-group:focus-within {
  background: #f0ebe0;
}

.prefix {
  padding: 0 12px;
  font-size: 14px;
  color: #666;
  font-weight: 500;
  white-space: nowrap;
  border-right: 1px solid #e8e8e8;
  line-height: 46px;
  user-select: none;
}

.form-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  padding: 0 12px;
  font-size: 15px;
  color: #1a1a1a;
  height: 46px;
  min-width: 0;
}

.form-input::placeholder {
  color: #bbb;
}

.form-input.flex-1 {
  flex: 1;
}

/* ==================== 验证码按钮 ==================== */
.code-btn {
  white-space: nowrap;
  border: none;
  background: transparent;
  color: #c9a96e;
  font-size: 13px;
  padding: 0 14px;
  height: 46px;
  cursor: pointer;
  font-weight: 500;
  transition: color 0.2s;
  border-left: 1px solid #e8e8e8;
}

.code-btn:disabled {
  color: #bbb;
  cursor: not-allowed;
}

.code-btn:not(:disabled):active {
  color: #b08d4a;
}

/* ==================== 协议 ==================== */
.agreement {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 18px;
  cursor: pointer;
  user-select: none;
}

.checkbox {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2px solid #d0d0d0;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  margin-top: 1px;
}

.checkbox.checked {
  background: #c9a96e;
  border-color: #c9a96e;
}

.check-icon {
  width: 12px;
  height: 12px;
}

.agreement-text {
  font-size: 12px;
  color: #999;
  line-height: 1.6;
}

.agreement-text .link {
  color: #c9a96e;
}

/* ==================== 登录按钮 ==================== */
.login-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  margin-top: 22px;
  height: 48px;
  border: none;
  border-radius: 24px;
  background: linear-gradient(135deg, #c9a96e, #b08d4a);
  color: #fff;
  font-size: 17px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.1s;
  letter-spacing: 2px;
}

.login-btn:active:not(:disabled) {
  transform: scale(0.97);
}

.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

/* ==================== Loading 旋转 ==================== */
.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
