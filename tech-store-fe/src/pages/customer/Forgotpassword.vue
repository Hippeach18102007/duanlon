<template>
  <div style="min-height: 100vh; display: flex; align-items: center; justify-content: center;
              background: #f0f2fa; padding: 24px;">

    <el-card
      shadow="always"
      :class="{ 'card-shake': shaking }"
      style="width: 420px; max-width: 100%;"
      :body-style="{ padding: '36px 32px' }"
    >
      <!-- Header -->
      <el-space direction="vertical" :size="6" style="width: 100%; margin-bottom: 28px;">
        <el-tag type="primary" effect="plain" size="small">Cổng khách hàng</el-tag>
        <el-text tag="div" style="font-size: 28px; font-weight: 700; color: #111827;
                                   letter-spacing: -0.02em; line-height: 1.2;">
          {{ stepTitle }}
        </el-text>
        <el-text size="small" type="info">{{ stepDesc }}</el-text>
      </el-space>

      <!-- Step indicator -->
      <el-steps :active="step - 1" finish-status="success" simple style="margin-bottom: 28px;">
        <el-step title="Email" />
        <el-step title="OTP" />
        <el-step title="Mật khẩu" />
      </el-steps>

      <!-- Alert -->
      <el-alert
        v-if="alert"
        :type="alertType"
        :title="alert"
        show-icon
        :closable="false"
        style="margin-bottom: 20px;"
      />

      <!-- STEP 1: Nhập email -->
      <el-form v-if="step === 1" label-position="top" @submit.prevent="sendOtp">
        <el-form-item label="Email đăng nhập">
          <el-input
            v-model="email"
            placeholder="Nhập email của bạn"
            size="large"
            type="email"
          >
            <template #prefix><el-icon><Message /></el-icon></template>
          </el-input>
        </el-form-item>

        <el-form-item style="margin-bottom: 0;">
          <el-button
            type="primary"
            native-type="submit"
            :loading="loading"
            size="large"
            style="width: 100%;"
          >
            {{ loading ? 'Đang gửi...' : 'Gửi mã OTP' }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- STEP 2: Nhập OTP -->
      <el-form v-if="step === 2" label-position="top" @submit.prevent="verifyOtp">
        <el-form-item>
          <el-text size="small" type="info">
            Mã OTP đã được gửi đến <strong>{{ email }}</strong>. Có hiệu lực trong 5 phút.
          </el-text>
        </el-form-item>

        <el-form-item label="Mã OTP (6 số)">
          <el-input
            v-model="otp"
            placeholder="_ _ _ _ _ _"
            size="large"
            maxlength="6"
            style="letter-spacing: 8px; font-size: 22px; font-weight: 700; text-align: center;"
            @input="otp = otp.replace(/\D/g, '')"
          >
            <template #prefix><el-icon><Key /></el-icon></template>
          </el-input>
        </el-form-item>

        <el-form-item style="margin-bottom: 0;">
          <el-button
            type="primary"
            native-type="submit"
            :loading="loading"
            :disabled="otp.length !== 6"
            size="large"
            style="width: 100%;"
          >
            {{ loading ? 'Đang xác nhận...' : 'Xác nhận OTP' }}
          </el-button>
        </el-form-item>

        <el-row justify="center" style="margin-top: 14px;">
          <el-text size="small" type="info">Chưa nhận được mã?&nbsp;</el-text>
          <el-link
            type="primary"
            size="small"
            :disabled="resendCooldown > 0"
            @click="resendOtp"
          >
            {{ resendCooldown > 0 ? `Gửi lại (${resendCooldown}s)` : 'Gửi lại' }}
          </el-link>
        </el-row>
      </el-form>

      <!-- STEP 3: Nhập mật khẩu mới -->
      <el-form v-if="step === 3" label-position="top" @submit.prevent="resetPassword">
        <el-form-item label="Mật khẩu mới">
          <el-input
            v-model="newPassword"
            type="password"
            show-password
            placeholder="Tối thiểu 6 ký tự"
            size="large"
          >
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>

        <el-form-item label="Xác nhận mật khẩu">
          <el-input
            v-model="confirmPassword"
            type="password"
            show-password
            placeholder="Nhập lại mật khẩu"
            size="large"
          >
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>

        <el-form-item style="margin-bottom: 0;">
          <el-button
            type="primary"
            native-type="submit"
            :loading="loading"
            size="large"
            style="width: 100%;"
          >
            {{ loading ? 'Đang cập nhật...' : 'Đặt lại mật khẩu' }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- STEP 4: Thành công -->
      <div v-if="step === 4" style="text-align: center; padding: 12px 0;">
        <el-icon style="font-size: 56px; color: #22c55e; margin-bottom: 16px;">
          <CircleCheckFilled />
        </el-icon>
        <el-text tag="div" style="font-size: 18px; font-weight: 600; color: #111827; margin-bottom: 8px;">
          Đặt lại mật khẩu thành công!
        </el-text>
        <el-text size="small" type="info" tag="div" style="margin-bottom: 24px;">
          Bạn có thể đăng nhập bằng mật khẩu mới ngay bây giờ.
        </el-text>
        <el-button type="primary" size="large" style="width: 100%;" @click="router.replace('/login')">
          Về trang đăng nhập
        </el-button>
      </div>

      <!-- Back to login (step 1-3) -->
      <el-row v-if="step < 4" justify="center" style="margin-top: 20px;">
        <el-link type="info" @click="router.push('/login')">← Quay lại đăng nhập</el-link>
      </el-row>

    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { CircleCheckFilled, Key, Lock, Message } from '@element-plus/icons-vue'
import { toast } from '../../ui/toast'
import http from '../../api/http'

const router = useRouter()

const step = ref(1)
const email = ref('')
const otp = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const alert = ref('')
const alertType = ref('error')
const shaking = ref(false)
const resendCooldown = ref(0)
let cooldownTimer = null

const stepTitle = computed(() => {
  if (step.value === 1) return 'Quên mật khẩu'
  if (step.value === 2) return 'Nhập mã OTP'
  if (step.value === 3) return 'Mật khẩu mới'
  return 'Hoàn tất'
})

const stepDesc = computed(() => {
  if (step.value === 1) return 'Nhập email để nhận mã xác nhận'
  if (step.value === 2) return 'Kiểm tra hộp thư và nhập mã 6 số'
  if (step.value === 3) return 'Tạo mật khẩu mới cho tài khoản'
  return ''
})

function triggerShake() {
  shaking.value = true
  setTimeout(() => (shaking.value = false), 600)
}

function setAlert(msg, type = 'error') {
  alert.value = msg
  alertType.value = type
}

function startCooldown(seconds = 60) {
  resendCooldown.value = seconds
  cooldownTimer = setInterval(() => {
    resendCooldown.value--
    if (resendCooldown.value <= 0) clearInterval(cooldownTimer)
  }, 1000)
}

async function sendOtp() {
  if (!email.value.trim()) return setAlert('Vui lòng nhập email')
  loading.value = true
  alert.value = ''
  try {
    await http.post('/api/auth/forgot-password', { email: email.value.trim() })
    setAlert('Mã OTP đã được gửi về email của bạn', 'success')
    step.value = 2
    startCooldown(60)
  } catch (e) {
    setAlert(e?.response?.data?.message || 'Không thể gửi OTP, thử lại sau')
    triggerShake()
  } finally {
    loading.value = false
  }
}

async function resendOtp() {
  if (resendCooldown.value > 0) return
  loading.value = true
  alert.value = ''
  try {
    await http.post('/api/auth/forgot-password', { email: email.value.trim() })
    setAlert('Đã gửi lại mã OTP', 'success')
    startCooldown(60)
  } catch (e) {
    setAlert(e?.response?.data?.message || 'Không thể gửi lại OTP')
  } finally {
    loading.value = false
  }
}

function verifyOtp() {
  if (otp.value.length !== 6) return setAlert('Vui lòng nhập đủ 6 số')
  // OTP sẽ được verify ở bước reset, ở đây chỉ chuyển bước
  alert.value = ''
  step.value = 3
}

async function resetPassword() {
  if (!newPassword.value || newPassword.value.length < 6)
    return setAlert('Mật khẩu tối thiểu 6 ký tự')
  if (newPassword.value !== confirmPassword.value)
    return setAlert('Mật khẩu xác nhận không khớp')

  loading.value = true
  alert.value = ''
  try {
    await http.post('/api/auth/reset-password', {
      email: email.value.trim(),
      otp: otp.value,
      newPassword: newPassword.value,
    })
    step.value = 4
    toast('Đặt lại mật khẩu thành công!', 'success')
  } catch (e) {
    setAlert(e?.response?.data?.message || 'OTP không đúng hoặc đã hết hạn')
    triggerShake()
  } finally {
    loading.value = false
  }
}

onUnmounted(() => clearInterval(cooldownTimer))
</script>

<style scoped>
.card-shake {
  animation: shake 0.6s cubic-bezier(.36,.07,.19,.97) both;
}
::v-deep .el-step__title {
  white-space: nowrap;
}
@keyframes shake {
  10%, 90% { transform: translateX(-2px); }
  20%, 80% { transform: translateX(4px); }
  30%, 50%, 70% { transform: translateX(-6px); }
  40%, 60% { transform: translateX(6px); }
}
</style>