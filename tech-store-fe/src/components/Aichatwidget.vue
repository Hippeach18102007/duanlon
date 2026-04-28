<template>
  <div class="aichat-root">

    <!-- Bubble button -->
    <transition name="bubble-pop">
      <el-badge v-if="!isOpen" value="AI" :hidden="hasInteracted">
        <el-button
          circle
          :class="{ 'has-pulse': !hasInteracted }"
          class="aichat-bubble"
          @click="openChat"
          aria-label="Chat với AI"
        >
          <svg width="26" height="26" viewBox="0 0 24 24" fill="none">
            <path d="M12 2C6.48 2 2 6.02 2 11c0 2.5 1.06 4.76 2.77 6.38L4 22l4.89-1.56A10.1 10.1 0 0 0 12 20.98c5.52 0 10-4.02 10-8.98S17.52 2 12 2z" fill="currentColor"/>
            <circle cx="8.5" cy="11" r="1.2" fill="white"/>
            <circle cx="12" cy="11" r="1.2" fill="white"/>
            <circle cx="15.5" cy="11" r="1.2" fill="white"/>
          </svg>
        </el-button>
      </el-badge>
    </transition>

    <!-- Chat panel -->
    <transition name="panel-slide">
      <div v-if="isOpen" class="aichat-panel">

        <!-- Header -->
        <div class="panel-header">
          <el-space :size="12" align="center">
            <div class="agent-avatar">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <path d="M12 2C6.48 2 2 6.02 2 11c0 2.5 1.06 4.76 2.77 6.38L4 22l4.89-1.56A10.1 10.1 0 0 0 12 20.98c5.52 0 10-4.02 10-8.98S17.52 2 12 2z" fill="white"/>
              </svg>
              <span class="agent-online"></span>
            </div>
            <el-space direction="vertical" :size="1">
              <el-text tag="div" style="font-size: 14px; font-weight: 800; color: white; line-height: 1.2;">RetailPro AI</el-text>
              <el-text tag="div" style="font-size: 11.5px; color: rgba(255,255,255,0.7);">
                {{ isTyping ? 'Đang soạn tin...' : 'Trực tuyến' }}
              </el-text>
            </el-space>
          </el-space>
          <el-space :size="6">
            <el-tooltip content="Cuộc trò chuyện mới" placement="top">
              <el-button text class="header-btn" @click="clearChat">
                <el-icon style="color: rgba(255,255,255,0.85);"><RefreshLeft /></el-icon>
              </el-button>
            </el-tooltip>
            <el-button text class="header-btn header-btn--close" @click="isOpen = false">
              <el-icon style="color: rgba(255,255,255,0.85);"><Close /></el-icon>
            </el-button>
          </el-space>
        </div>

        <!-- Welcome screen -->
        <div v-if="messages.length === 0" class="panel-welcome">
          <div class="welcome-emoji">👋</div>
          <el-text tag="div" style="font-size: 16px; font-weight: 800; color: #0f172a; margin-bottom: 4px;">
            Xin chào{{ customerName ? ', ' + customerName : '' }}!
          </el-text>
          <el-text size="small" type="info" style="display: block; margin-bottom: 16px;">Tôi có thể giúp gì cho bạn hôm nay?</el-text>
          <el-space wrap style="justify-content: center;">
            <el-button
              v-for="q in quickQuestions"
              :key="q"
              size="small"
              plain
              style="border-radius: 100px; border-color: rgba(99,102,241,0.2); color: #4f46e5;"
              @click="sendQuick(q)"
            >{{ q }}</el-button>
          </el-space>
        </div>

        <!-- Messages -->
        <el-scrollbar class="panel-messages" ref="messagesEl" always>
          <div v-for="(msg, i) in messages" :key="i" class="msg-row" :class="msg.role === 'user' ? 'msg-row--user' : 'msg-row--ai'">
            <div v-if="msg.role !== 'user'" class="msg-avatar">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none">
                <path d="M12 2C6.48 2 2 6.02 2 11c0 2.5 1.06 4.76 2.77 6.38L4 22l4.89-1.56A10.1 10.1 0 0 0 12 20.98c5.52 0 10-4.02 10-8.98S17.52 2 12 2z" fill="white"/>
              </svg>
            </div>
            <div class="msg-bubble" v-html="formatMsg(msg.content)"></div>
          </div>

          <!-- Typing indicator -->
          <div v-if="isTyping" class="msg-row msg-row--ai">
            <div class="msg-avatar">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none">
                <path d="M12 2C6.48 2 2 6.02 2 11c0 2.5 1.06 4.76 2.77 6.38L4 22l4.89-1.56A10.1 10.1 0 0 0 12 20.98c5.52 0 10-4.02 10-8.98S17.52 2 12 2z" fill="white"/>
              </svg>
            </div>
            <div class="msg-bubble msg-bubble--typing">
              <span></span><span></span><span></span>
            </div>
          </div>

          <!-- Escalate -->
          <el-alert
            v-if="showEscalate"
            type="warning"
            :closable="false"
            style="border-radius: 14px; margin-top: 8px;"
          >
            <template #default>
              <el-row align="middle" justify="space-between">
                <el-text size="small">🎧 Bạn muốn được hỗ trợ bởi nhân viên?</el-text>
                <el-button size="small" type="warning" plain @click="showEscalate = false">Để AI xử lý</el-button>
              </el-row>
            </template>
          </el-alert>
        </el-scrollbar>

        <!-- Input -->
        <div class="panel-input">
          <el-input
            ref="inputEl"
            v-model="inputText"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 4 }"
            placeholder="Nhập tin nhắn..."
            resize="none"
            class="input-field"
            @keydown.enter.exact.prevent="send"
          />
          <el-button
            circle
            type="primary"
            :disabled="!inputText.trim() || isTyping"
            class="send-btn"
            @click="send"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M22 2 11 13M22 2l-7 20-4-9-9-4 20-7z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </el-button>
        </div>
        <div class="panel-footer">Powered by RetailPro AI · <span>Groq LLaMA</span></div>

      </div>
    </transition>
  </div>
</template>

<script setup>
import { Close, RefreshLeft } from "@element-plus/icons-vue";
import { ref, nextTick, computed } from 'vue';
import { useAuthStore } from '../stores/auth';
import http from '../api/http';

const auth = useAuthStore();
const customerName = computed(() => auth.user?.name || auth.user?.username || '');

const isOpen       = ref(false);
const hasInteracted = ref(false);
const isTyping     = ref(false);
const inputText    = ref('');
const sessionId    = ref(null);
const messages     = ref([]);
const showEscalate = ref(false);
const messagesEl   = ref(null);
const inputEl      = ref(null);

const quickQuestions = [
  '💎 Điểm thưởng của tôi',
  '🛍️ Gợi ý sản phẩm',
  '📦 Kiểm tra đơn hàng',
  '🎁 Ưu đãi VIP',
];

function openChat() {
  isOpen.value = true;
  hasInteracted.value = true;
  nextTick(() => inputEl.value?.focus());
}

function clearChat() {
  messages.value = [];
  sessionId.value = null;
  showEscalate.value = false;
}

async function send() {
  const text = inputText.value.trim();
  if (!text || isTyping.value) return;
  messages.value.push({ role: 'user', content: text });
  inputText.value = '';
  isTyping.value = true;
  showEscalate.value = false;
  await scrollBottom();
  try {
    const res = await http.post('/api/chat/customer', { message: text, sessionId: sessionId.value });
    const data = res.data;
    sessionId.value = data.sessionId;
    messages.value.push({ role: 'ai', content: data.reply });
    if (data.escalate) showEscalate.value = true;
  } catch {
    messages.value.push({ role: 'ai', content: 'Xin lỗi, có lỗi xảy ra. Vui lòng thử lại sau.' });
  } finally {
    isTyping.value = false;
    await scrollBottom();
  }
}

function sendQuick(q) {
  inputText.value = q.replace(/^[\p{Emoji}\s]+/u, '').trim();
  send();
}

function formatMsg(text) {
  if (!text) return '';
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>');
}

async function scrollBottom() {
  await nextTick();
  const el = messagesEl.value?.$el?.querySelector('.el-scrollbar__wrap');
  if (el) el.scrollTop = el.scrollHeight;
}
</script>

<style scoped>
/* ── Root (fixed positioning — cannot be replaced by EP) ── */
.aichat-root {
  position: fixed;
  bottom: 28px;
  right: 28px;
  z-index: 9999;
}

/* ── Bubble button ── */
.aichat-bubble {
  width: 58px !important;
  height: 58px !important;
  background: linear-gradient(135deg, #6366f1, #4f46e5) !important;
  border: none !important;
  color: white !important;
  box-shadow: 0 8px 24px rgba(99,102,241,0.45);
  transition: transform 0.2s cubic-bezier(0.34,1.5,0.64,1), box-shadow 0.2s;
}
.aichat-bubble:hover { transform: scale(1.08); box-shadow: 0 12px 32px rgba(99,102,241,0.55); }
.aichat-bubble.has-pulse::before {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 2px solid rgba(99,102,241,0.4);
  animation: pulse-ring 2s ease-out infinite;
}
@keyframes pulse-ring { 0%{transform:scale(1);opacity:0.6} 100%{transform:scale(1.5);opacity:0} }

/* ── Panel ── */
.aichat-panel {
  width: 360px;
  height: 520px;
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 24px 80px rgba(15,23,42,0.18), 0 4px 16px rgba(15,23,42,0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid rgba(99,102,241,0.12);
}

/* ── Header ── */
.panel-header {
  background: linear-gradient(135deg, #4f46e5, #6366f1);
  padding: 16px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}
.agent-avatar {
  width: 38px; height: 38px;
  border-radius: 50%;
  background: rgba(255,255,255,0.2);
  display: flex; align-items: center; justify-content: center;
  position: relative; flex-shrink: 0;
}
.agent-online {
  position: absolute; bottom: 1px; right: 1px;
  width: 9px; height: 9px;
  background: #4ade80; border-radius: 50%; border: 2px solid #4f46e5;
}
.header-btn { width: 30px !important; height: 30px !important; border-radius: 8px !important; background: rgba(255,255,255,0.15) !important; }
.header-btn:hover { background: rgba(255,255,255,0.25) !important; }
.header-btn--close:hover { background: rgba(239,68,68,0.4) !important; }

/* ── Welcome ── */
.panel-welcome { padding: 24px 20px 12px; text-align: center; flex-shrink: 0; }
.welcome-emoji { font-size: 36px; margin-bottom: 8px; }

/* ── Messages area ── */
.panel-messages {
  flex: 1;
  padding: 12px 16px;
}
:deep(.panel-messages .el-scrollbar__view) {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.msg-row { display: flex; align-items: flex-end; gap: 8px; animation: msg-in 0.3s cubic-bezier(0.34,1.3,0.64,1) both; }
@keyframes msg-in { from{opacity:0;transform:translateY(8px)} to{opacity:1;transform:translateY(0)} }
.msg-row--user { flex-direction: row-reverse; }
.msg-avatar {
  width: 26px; height: 26px; border-radius: 50%;
  background: linear-gradient(135deg, #4f46e5, #6366f1);
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.msg-bubble {
  max-width: 78%; padding: 10px 14px; border-radius: 18px;
  font-size: 13.5px; line-height: 1.55; word-break: break-word;
}
.msg-row--ai .msg-bubble   { background: #f1f5f9; color: #1e293b; border-bottom-left-radius: 4px; }
.msg-row--user .msg-bubble { background: linear-gradient(135deg, #4f46e5, #6366f1); color: white; border-bottom-right-radius: 4px; }
.msg-bubble--typing { display: flex; align-items: center; gap: 5px; padding: 12px 16px; min-width: 56px; }
.msg-bubble--typing span {
  width: 7px; height: 7px; border-radius: 50%; background: #94a3b8;
  animation: typing-dot 1.2s ease-in-out infinite;
}
.msg-bubble--typing span:nth-child(2) { animation-delay: 0.2s; }
.msg-bubble--typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes typing-dot { 0%,60%,100%{transform:translateY(0);opacity:0.4} 30%{transform:translateY(-5px);opacity:1} }

/* ── Input area ── */
.panel-input {
  padding: 12px 14px;
  border-top: 1px solid rgba(15,23,42,0.07);
  display: flex; align-items: flex-end; gap: 10px;
  background: #fff; flex-shrink: 0;
}
.input-field { flex: 1; }
:deep(.input-field .el-textarea__inner) {
  border: 1.5px solid rgba(99,102,241,0.2);
  border-radius: 14px;
  background: #f8faff;
  font-size: 13.5px;
  resize: none;
  line-height: 1.5;
}
:deep(.input-field .el-textarea__inner:focus) {
  border-color: #6366f1;
  background: #fff;
}
.send-btn { flex-shrink: 0; background: linear-gradient(135deg, #4f46e5, #6366f1) !important; border: none !important; }

/* ── Footer ── */
.panel-footer {
  text-align: center; font-size: 10.5px;
  color: #94a3b8; padding: 6px 0 10px; flex-shrink: 0;
}
.panel-footer span { color: #6366f1; font-weight: 700; }

/* ── Transitions ── */
.bubble-pop-enter-active { transition: all 0.3s cubic-bezier(0.34,1.5,0.64,1); }
.bubble-pop-leave-active { transition: all 0.2s ease; }
.bubble-pop-enter-from, .bubble-pop-leave-to { opacity: 0; transform: scale(0.6); }
.panel-slide-enter-active { transition: all 0.35s cubic-bezier(0.34,1.3,0.64,1); }
.panel-slide-leave-active { transition: all 0.25s ease; }
.panel-slide-enter-from { opacity: 0; transform: translateY(20px) scale(0.95); }
.panel-slide-leave-to   { opacity: 0; transform: translateY(16px) scale(0.97); }

@media (max-width: 480px) {
  .aichat-root  { bottom: 16px; right: 16px; }
  .aichat-panel { width: calc(100vw - 32px); height: 480px; }
}
</style>