<template>
  <div class="ai-page">
    <header class="ai-nav">
      <button class="ai-nav__back" type="button" aria-label="返回" @click="goBack">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="m15 18-6-6 6-6" />
        </svg>
      </button>
      <h1 class="ai-nav__title">AI助手</h1>
      <span class="ai-nav__placeholder"></span>
    </header>

    <section class="ai-profile">
      <span class="assistant-avatar assistant-avatar--large" aria-hidden="true">
        <svg viewBox="0 0 40 40">
          <path d="M20 8v4M20 28v4M8 20h4M28 20h4" />
          <path d="m12.5 12.5 2.7 2.7m9.6 9.6 2.7 2.7m0-15-2.7 2.7m-9.6 9.6-2.7 2.7" />
          <path d="M20 14.2c.8 3.1 2.7 5 5.8 5.8-3.1.8-5 2.7-5.8 5.8-.8-3.1-2.7-5-5.8-5.8 3.1-.8 5-2.7 5.8-5.8Z" />
        </svg>
      </span>
      <div class="ai-profile__copy">
        <strong>AI 智能助手</strong>
        <span>古玩鉴赏 · 随时解答</span>
      </div>
    </section>

    <main ref="messageListRef" class="message-list">
      <div v-if="historyLoading" class="history-loading">
        <span class="history-loading__spinner"></span>
        <span>正在加载对话...</span>
      </div>

      <template v-else>
        <article
          v-for="message in messages"
          :key="message.id"
          class="message-row"
          :class="`message-row--${message.role}`"
        >
          <span
            v-if="message.role === 'assistant'"
            class="assistant-avatar"
            aria-hidden="true"
          >
            <svg viewBox="0 0 40 40">
              <path d="M20 8v4M20 28v4M8 20h4M28 20h4" />
              <path d="m12.5 12.5 2.7 2.7m9.6 9.6 2.7 2.7m0-15-2.7 2.7m-9.6 9.6-2.7 2.7" />
              <path d="M20 14.2c.8 3.1 2.7 5 5.8 5.8-3.1.8-5 2.7-5.8 5.8-.8-3.1-2.7-5-5.8-5.8 3.1-.8 5-2.7 5.8-5.8Z" />
            </svg>
          </span>
          <p class="message-bubble">{{ message.content }}</p>
        </article>

        <article v-if="answerLoading" class="message-row message-row--assistant">
          <span class="assistant-avatar" aria-hidden="true">
            <svg viewBox="0 0 40 40">
              <path d="M20 8v4M20 28v4M8 20h4M28 20h4" />
              <path d="m12.5 12.5 2.7 2.7m9.6 9.6 2.7 2.7m0-15-2.7 2.7m-9.6 9.6-2.7 2.7" />
              <path d="M20 14.2c.8 3.1 2.7 5 5.8 5.8-3.1.8-5 2.7-5.8 5.8-.8-3.1-2.7-5-5.8-5.8 3.1-.8 5-2.7 5.8-5.8Z" />
            </svg>
          </span>
          <p class="message-bubble message-bubble--thinking" aria-label="AI正在思考">
            <span></span><span></span><span></span>
          </p>
        </article>
      </template>
    </main>

    <footer class="chat-controls">
      <section class="quick-questions" aria-label="快捷提问">
        <h2>快捷提问</h2>
        <div class="quick-questions__list">
          <button
            v-for="item in QUICK_QUESTIONS"
            :key="item"
            type="button"
            :disabled="answerLoading || historyLoading"
            @click="handleQuickQuestion(item)"
          >
            {{ item }}
          </button>
        </div>
      </section>

      <form class="composer" @submit.prevent="sendQuestion()">
        <div class="composer__field">
          <textarea
            v-model="question"
            maxlength="500"
            :disabled="answerLoading || historyLoading"
            placeholder="输入您的问题..."
            aria-label="输入您的问题"
            @keydown.enter.exact.prevent="sendQuestion()"
          ></textarea>
        </div>
        <button
          class="composer__send"
          type="submit"
          aria-label="发送问题"
          :disabled="!canSend"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="m21 3-7.8 18-2.7-7.5L3 10.8 21 3Z" />
            <path d="m10.5 13.5 4.2-4.2" />
          </svg>
        </button>
      </form>
    </footer>

    <transition name="toast-fade">
      <div v-if="toastVisible" class="page-toast">{{ toastText }}</div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { AiMessage } from '@/api/ai'
import { getAiHistory, sendAiQuestion } from '@/utils/ai'

interface ViewMessage extends AiMessage {
  id: string
}

const WELCOME_TEXT =
  '您好！我是您的古玩鉴赏助手。我可以帮您解答古玩收藏、鉴定、市场行情等问题。请问有什么可以帮您？'

const QUICK_QUESTIONS = [
  '如何辨别青花瓷真伪?',
  '清代瓷器收藏价值',
  '古玩保养方法',
] as const

const router = useRouter()
const messageListRef = ref<HTMLElement | null>(null)
const messages = ref<ViewMessage[]>([])
const question = ref('')
const historyLoading = ref(true)
const answerLoading = ref(false)

const canSend = computed(
  () =>
    question.value.trim().length > 0 &&
    !answerLoading.value &&
    !historyLoading.value,
)

function createMessage(message: AiMessage): ViewMessage {
  return { ...message, id: crypto.randomUUID() }
}

async function scrollToBottom(): Promise<void> {
  await nextTick()
  const list = messageListRef.value
  if (list) list.scrollTop = list.scrollHeight
}

function goBack(): void {
  router.back()
}

let toastTimer: number | undefined
const toastText = ref('')
const toastVisible = ref(false)

function showToast(text: string): void {
  toastText.value = text
  toastVisible.value = true
  window.clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => {
    toastVisible.value = false
  }, 2200)
}

async function loadHistory(): Promise<void> {
  historyLoading.value = true
  try {
    const response = await getAiHistory()
    if (response.code !== 1 || !response.data) {
      throw new Error(response.msg || '对话历史加载失败')
    }

    messages.value = response.data.list.length
      ? response.data.list.map(createMessage)
      : [createMessage({ role: 'assistant', content: WELCOME_TEXT })]
  } catch (error) {
    console.error('获取AI对话历史失败：', error)
    messages.value = [createMessage({ role: 'assistant', content: WELCOME_TEXT })]
    showToast('历史记录加载失败，您仍可以继续提问')
  } finally {
    historyLoading.value = false
    await scrollToBottom()
  }
}

async function sendQuestion(content = question.value): Promise<void> {
  const value = content.trim()
  if (!value || answerLoading.value || historyLoading.value) return

  if (value.length > 500) {
    showToast('问题不能超过500个字符')
    return
  }

  messages.value.push(createMessage({ role: 'user', content: value }))
  question.value = ''
  answerLoading.value = true
  await scrollToBottom()

  try {
    const response = await sendAiQuestion(value)
    if (response.code !== 1 || !response.data) {
      throw new Error(response.msg || 'AI服务暂时不可用')
    }

    messages.value.push(
      createMessage({ role: 'assistant', content: response.data.answer }),
    )
  } catch (error) {
    console.error('AI问答失败：', error)
    const message = error instanceof Error ? error.message : 'AI服务暂时不可用，请稍后重试'
    showToast(message)
  } finally {
    answerLoading.value = false
    await scrollToBottom()
  }
}

function handleQuickQuestion(content: string): void {
  void sendQuestion(content)
}

onMounted(() => {
  void loadHistory()
})

onBeforeUnmount(() => {
  window.clearTimeout(toastTimer)
})
</script>

<style scoped>
.ai-page {
  --ai-accent: #c95700;
  --ai-accent-light: #eda774;
  --avatar-from: #a348ff;
  --avatar-to: #6876ff;
  height: 100vh;
  height: 100dvh;
  width: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #f8f8f8;
  color: #333;
  text-align: left;
}

.ai-nav {
  flex: 0 0 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  box-sizing: border-box;
  background: #fff;
  border-bottom: 1px solid #f3f3f3;
}

.ai-nav__back {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 0;
  background: transparent;
  color: #444;
  cursor: pointer;
}

.ai-nav__back svg {
  width: 24px;
  height: 24px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.ai-nav__title {
  margin: 0;
  font-size: 18px;
  line-height: 1;
  font-weight: 700;
  color: #30343b;
}

.ai-nav__placeholder {
  width: 36px;
}

.ai-profile {
  flex: 0 0 72px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 15px;
  box-sizing: border-box;
  background: #fff;
  border-bottom: 1px solid #ededed;
}

.assistant-avatar {
  flex: 0 0 38px;
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: linear-gradient(145deg, var(--avatar-from), var(--avatar-to));
  color: #fff;
  box-shadow: 0 3px 8px rgba(118, 88, 255, 0.2);
}

.assistant-avatar--large {
  flex-basis: 42px;
  width: 42px;
  height: 42px;
}

.assistant-avatar svg {
  width: 26px;
  height: 26px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.7;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.ai-profile__copy {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.ai-profile__copy strong {
  font-size: 16px;
  line-height: 1.2;
  color: #282828;
}

.ai-profile__copy span {
  font-size: 13px;
  line-height: 1.2;
  color: #888;
}

.message-list {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 20px 14px 28px;
  box-sizing: border-box;
  scrollbar-width: thin;
  scrollbar-color: #ddd transparent;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 20px;
}

.message-row--user {
  justify-content: flex-end;
}

.message-bubble {
  max-width: min(76%, 560px);
  margin: 0;
  padding: 14px 15px;
  box-sizing: border-box;
  border-radius: 10px;
  font-size: 15px;
  line-height: 1.65;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.message-row--assistant .message-bubble {
  background: #fff;
  border: 1px solid #e8e8e8;
  color: #3d3d3d;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.04);
}

.message-row--user .message-bubble {
  max-width: 78%;
  background: var(--ai-accent);
  color: #fff;
  font-weight: 600;
  border-radius: 10px 10px 3px 10px;
  box-shadow: 0 3px 8px rgba(201, 87, 0, 0.14);
}

.message-bubble--thinking {
  min-width: 68px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding-block: 18px;
}

.message-bubble--thinking span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #a7a7a7;
  animation: thinking 1.2s infinite ease-in-out;
}

.message-bubble--thinking span:nth-child(2) { animation-delay: 0.15s; }
.message-bubble--thinking span:nth-child(3) { animation-delay: 0.3s; }

@keyframes thinking {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.45; }
  30% { transform: translateY(-4px); opacity: 1; }
}

.history-loading {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  color: #999;
  font-size: 13px;
}

.history-loading__spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #ececec;
  border-top-color: var(--ai-accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.chat-controls {
  flex: 0 0 auto;
  background: #fff;
  border-top: 1px solid #e9e9e9;
  padding-bottom: env(safe-area-inset-bottom);
}

.quick-questions {
  padding: 12px 14px 13px;
  border-bottom: 1px solid #eee;
}

.quick-questions h2 {
  margin: 0 0 9px;
  font-size: 13px;
  line-height: 1;
  font-weight: 500;
  color: #888;
}

.quick-questions__list {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  scrollbar-width: none;
}

.quick-questions__list::-webkit-scrollbar { display: none; }

.quick-questions button {
  flex: 0 0 auto;
  padding: 7px 13px;
  border: 1px solid #e6e6e6;
  border-radius: 17px;
  background: #fff;
  color: #3f3f3f;
  font-size: 13px;
  line-height: 1;
  font-weight: 600;
  cursor: pointer;
}

.quick-questions button:active:not(:disabled) {
  background: #faf4ef;
  border-color: #ebc09e;
}

.quick-questions button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.composer {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 12px 10px 13px;
}

.composer__field {
  flex: 1;
  padding: 10px;
  border-radius: 11px;
  background: #f4f4f4;
}

.composer textarea {
  display: block;
  width: 100%;
  height: 82px;
  resize: none;
  padding: 10px 12px;
  box-sizing: border-box;
  border: 0;
  outline: 0;
  background: #fff;
  color: #333;
  font: inherit;
  font-size: 14px;
  line-height: 1.5;
}

.composer textarea::placeholder { color: #c7c7c7; }
.composer textarea:disabled { cursor: not-allowed; }

.composer__send {
  flex: 0 0 48px;
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  margin-bottom: 1px;
  padding: 0;
  border: 0;
  border-radius: 8px;
  background: var(--ai-accent-light);
  color: #fff;
  cursor: pointer;
  transition: transform 0.15s, opacity 0.15s;
}

.composer__send:not(:disabled):active { transform: scale(0.96); }
.composer__send:disabled { opacity: 0.5; cursor: not-allowed; }

.composer__send svg {
  width: 26px;
  height: 26px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.7;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.page-toast {
  position: fixed;
  left: 50%;
  top: 48%;
  z-index: 1000;
  transform: translate(-50%, -50%);
  max-width: min(76vw, 360px);
  padding: 10px 16px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.74);
  color: #fff;
  font-size: 13px;
  line-height: 1.4;
  text-align: center;
}

.toast-fade-enter-active,
.toast-fade-leave-active { transition: opacity 0.2s; }
.toast-fade-enter-from,
.toast-fade-leave-to { opacity: 0; }

</style>
