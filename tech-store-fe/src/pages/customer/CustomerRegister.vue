<template>
  <div style="position: fixed; inset: 0; z-index: 9999; display: flex; align-items: center;
              justify-content: center; background: #f0f2fa; padding: 24px; overflow-y: auto;">

    <el-card
      shadow="always"
      :class="{ 'card-shake': shaking }"
      style="width: 520px; max-width: 100%; margin-top: 16px; margin-bottom: 16px;"
      :body-style="{ padding: '36px 32px' }"
    >
      <!-- Header -->
      <el-space direction="vertical" :size="6" style="width: 100%; margin-bottom: 28px;">
        <el-tag type="primary" effect="plain" size="small">
          &nbsp;Cổng khách hàng
        </el-tag>
        <el-text tag="div" style="font-size: 28px; font-weight: 700; color: #111827; letter-spacing: -0.02em; line-height: 1.2;">
          Tạo tài khoản
        </el-text>
        <el-text size="small" type="info">Tham gia cùng chúng tôi — chỉ mất một phút</el-text>
      </el-space>

      <!-- Alert -->
      <el-alert
        v-if="alert"
        type="error"
        :title="alert"
        show-icon
        :closable="false"
        style="margin-bottom: 20px;"
      />

      <!-- Form -->
      <el-form label-position="top" @submit.prevent="doRegister">

        <!-- Row: Username + Email -->
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item
              label="Tên đăng nhập"
              :error="fieldErrors.username ? 'Cần ít nhất 3 ký tự' : ''"
            >
              <el-input
                v-model="form.username"
                placeholder="tên đăng nhập"
                autocomplete="username"
                @focus="focusedField = 'username'"
                @blur="focusedField = ''; validateField('username')"
              >
                <template #prefix><el-icon><User /></el-icon></template>
                <template #suffix>
                  <el-icon v-if="fieldErrors.username" style="color: var(--el-color-danger);"><CircleClose /></el-icon>
                  <el-icon v-else-if="form.username && !fieldErrors.username" style="color: var(--el-color-success);"><CircleCheck /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item
              label="Email"
              :error="fieldErrors.email ? 'Email không hợp lệ' : ''"
            >
              <el-input
                v-model="form.email"
                type="email"
                placeholder="email@domain.com"
                autocomplete="email"
                @focus="focusedField = 'email'"
                @blur="focusedField = ''; validateField('email')"
              >
                <template #prefix><el-icon><Message /></el-icon></template>
                <template #suffix>
                  <el-icon v-if="fieldErrors.email" style="color: var(--el-color-danger);"><CircleClose /></el-icon>
                  <el-icon v-else-if="form.email && !fieldErrors.email" style="color: var(--el-color-success);"><CircleCheck /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- Phone -->
        <el-form-item
          label="Số điện thoại"
          :error="fieldErrors.phone ? 'Vui lòng nhập số điện thoại hợp lệ' : ''"
        >
          <el-input
            v-model="form.phone"
            type="tel"
            placeholder="0912 345 678"
            autocomplete="tel"
            @focus="focusedField = 'phone'"
            @blur="focusedField = ''; validateField('phone')"
          >
            <template #prefix><el-icon><Phone /></el-icon></template>
            <template #suffix>
              <el-icon v-if="fieldErrors.phone" style="color: var(--el-color-danger);"><CircleClose /></el-icon>
              <el-icon v-else-if="form.phone && !fieldErrors.phone" style="color: var(--el-color-success);"><CircleCheck /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <!-- Password -->
        <el-form-item label="Mật khẩu">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="Tạo mật khẩu"
            autocomplete="new-password"
            @focus="focusedField = 'password'"
            @blur="focusedField = ''"
          >
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
          <el-progress
            v-if="form.password"
            :percentage="strengthPct"
            :status="strengthLevel === 'weak' ? 'exception' : strengthLevel === 'fair' ? 'warning' : 'success'"
            :show-text="true"
            :format="() => strengthText"
            style="width: 100%; margin-top: 8px;"
          />
        </el-form-item>

        <!-- Confirm Password -->
        <el-form-item
          label="Xác nhận mật khẩu"
          :error="passwordMismatch ? 'Mật khẩu không khớp' : ''"
        >
          <el-input
            v-model="form.password2"
            type="password"
            show-password
            placeholder="Nhập lại mật khẩu"
            autocomplete="new-password"
            @focus="focusedField = 'password2'"
            @blur="focusedField = ''"
          >
            <template #prefix><el-icon><Key /></el-icon></template>
            <template #suffix>
              <el-icon v-if="passwordMismatch" style="color: var(--el-color-danger);"><CircleClose /></el-icon>
              <el-icon v-else-if="form.password2 && form.password === form.password2" style="color: var(--el-color-success);"><CircleCheck /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <!-- Optional divider -->
        <el-divider content-position="center">
          <el-text size="small" type="info">Thông tin tùy chọn</el-text>
        </el-divider>

        <!-- Row: Date of Birth + Address -->
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item>
              <template #label>
                <el-space :size="6">
                  <el-text size="small">Ngày sinh</el-text>
                  <el-tag size="small" type="info" effect="plain">tùy chọn</el-tag>
                </el-space>
              </template>
              <el-date-picker
                v-model="form.dateOfBirth"
                type="date"
                placeholder="Chọn ngày sinh"
                :disabled-date="(d) => d > new Date()"
                value-format="YYYY-MM-DD"
                style="width: 100%;"
                popper-class="datepicker-above-overlay"
                @focus="focusedField = 'dateOfBirth'"
                @blur="focusedField = ''"
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item>
              <template #label>
                <el-space :size="6">
                  <el-text size="small">Địa chỉ</el-text>
                  <el-tag size="small" type="info" effect="plain">tùy chọn</el-tag>
                </el-space>
              </template>
              <el-input
                v-model="form.address"
                placeholder="Địa chỉ của bạn"
                autocomplete="street-address"
                @focus="focusedField = 'address'"
                @blur="focusedField = ''"
              >
                <template #prefix><el-icon><Location /></el-icon></template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- Submit -->
        <el-form-item style="margin-bottom: 0;">
          <el-button
            type="primary"
            native-type="submit"
            :loading="loading"
            size="large"
            style="width: 100%;"
          >
            <el-icon v-if="!loading"><UserFilled /></el-icon>
            {{ loading ? 'Đang tạo tài khoản...' : 'Tạo tài khoản' }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- Footer -->
      <el-row justify="center" align="middle" style="margin-top: 20px;">
        <el-text size="small" type="info">Đã có tài khoản?&nbsp;</el-text>
        <el-link type="primary" @click="router.push('/login')">Đăng nhập</el-link>
      </el-row>
    </el-card>

  </div>
</template>

<script setup>
import {
  CircleCheck, CircleClose, Goods, Key,
  Location, Lock, Message, Phone, User, UserFilled,
} from "@element-plus/icons-vue";
import { reactive, ref, computed } from "vue";
import { useRouter } from "vue-router";
import { toast } from "../../ui/toast";
import { authApi } from "../../api/auth.api";

const router = useRouter();
const loading = ref(false);
const alert = ref("");
const shaking = ref(false);
const focusedField = ref("");
const showPassword = ref(false);
const showPassword2 = ref(false);

// Today's date as yyyy-MM-dd for max attribute on date input
const today = new Date().toISOString().split("T")[0];

const form = reactive({
  username: "",
  email: "",
  phone: "",         // required
  password: "",
  password2: "",
  dateOfBirth: "",   // optional – LocalDate on backend
  address: "",       // optional
});

const fieldErrors = reactive({
  username: false,
  email: false,
  phone: false,
});

function validateField(field) {
  if (field === "username") {
    fieldErrors.username = form.username.trim().length > 0 && form.username.trim().length < 3;
  }
  if (field === "email") {
    fieldErrors.email = form.email.length > 0 && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email);
  }
  if (field === "phone") {
    fieldErrors.phone = form.phone.length > 0 && !/^[0-9+\s\-().]{7,15}$/.test(form.phone.trim());
  }
}

const passwordMismatch = computed(() =>
  form.password2.length > 0 && form.password !== form.password2
);

const strengthScore = computed(() => {
  const p = form.password;
  if (!p) return 0;
  let s = 0;
  if (p.length >= 8) s++;
  if (p.length >= 12) s++;
  if (/[A-Z]/.test(p)) s++;
  if (/[0-9]/.test(p)) s++;
  if (/[^A-Za-z0-9]/.test(p)) s++;
  return s;
});

const strengthPct = computed(() => (strengthScore.value / 5) * 100);
const strengthLevel = computed(() => {
  if (strengthScore.value <= 1) return "weak";
  if (strengthScore.value <= 3) return "fair";
  return "strong";
});
const strengthText = computed(() => {
  if (strengthScore.value <= 1) return "Yếu";
  if (strengthScore.value <= 3) return "Trung bình";
  return "Mạnh";
});

function triggerShake() {
  shaking.value = true;
  setTimeout(() => (shaking.value = false), 600);
}

function errToText(e) {
  const msg = e?.response?.data?.message || e?.message || "Đăng ký thất bại";
  return typeof msg === "string" ? msg : JSON.stringify(msg);
}

async function doRegister() {
  alert.value = "";
  if (!form.username || !form.email || !form.phone || !form.password) {
    alert.value = "Vui lòng điền tất cả các trường bắt buộc.";
    triggerShake();
    return;
  }
  if (fieldErrors.phone) {
    alert.value = "Vui lòng nhập số điện thoại hợp lệ.";
    triggerShake();
    return;
  }
  if (form.password !== form.password2) {
    alert.value = "Mật khẩu không khớp.";
    triggerShake();
    return;
  }

  loading.value = true;
  try {
    const payload = {
      username: form.username,
      email: form.email,
      phone: form.phone.trim(),
      password: form.password,
      // Only include optional fields when they have a value
      ...(form.dateOfBirth && { dateOfBirth: form.dateOfBirth }),
      ...(form.address.trim() && { address: form.address.trim() }),
    };
    await authApi.register(payload);
    toast("Tạo tài khoản thành công! Vui lòng đăng nhập.", "success");
    router.replace("/login");
  } catch (e) {
    alert.value = errToText(e);
    triggerShake();
  } finally {
    loading.value = false;
  }
}
</script>

<style>
.datepicker-above-overlay {
  z-index: 10000 !important;
}
</style>