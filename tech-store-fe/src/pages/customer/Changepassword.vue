<template>
  <div style="position: fixed; inset: 0; display: flex; align-items: center; justify-content: center;
              background: #f0f2fa; padding: 24px; overflow-y: auto;">
    <el-card
      shadow="always"
      :class="{ 'card-shake': shaking }"
      style="width: 440px; max-width: 100%;"
      :body-style="{ padding: '40px' }"
    >
      <!-- Header -->
      <el-space direction="vertical" fill :size="6" style="margin-bottom: 24px; text-align: center; align-items: center;">
        <el-tag type="primary" effect="plain" size="small">
          &nbsp;Bảo mật
        </el-tag>
        <el-text tag="div" style="font-size: 28px; font-weight: 800; letter-spacing: -0.02em; color: #0f172a;">
          Đổi mật khẩu
        </el-text>
        <el-text size="small" type="info">Giữ tài khoản an toàn với mật khẩu mạnh</el-text>
      </el-space>

      <!-- Force banner -->
      <el-alert
        v-if="isForced"
        type="warning"
        show-icon
        :closable="false"
        style="margin-bottom: 20px;"
      >
        Tài khoản của bạn cần đổi mật khẩu trước khi tiếp tục.
        Mật khẩu hiện tại là <strong>số điện thoại</strong> của bạn.
      </el-alert>

      <!-- Success -->
      <el-alert
        v-if="success"
        type="success"
        title="Đổi mật khẩu thành công! Đang chuyển hướng..."
        show-icon
        :closable="false"
        style="margin-bottom: 20px;"
      />

      <!-- Error -->
      <el-alert
        v-if="alert"
        type="error"
        :title="alert"
        show-icon
        :closable="false"
        style="margin-bottom: 20px;"
      />

      <!-- Form -->
      <el-form label-position="top" @submit.prevent="doChange">
        <el-form-item
          label="Mật khẩu hiện tại"
          :error="fieldErrors.current ? 'Mật khẩu hiện tại là bắt buộc' : ''"
        >
          <el-input
            v-model="form.currentPassword"
            type="password"
            show-password
            placeholder="Nhập mật khẩu hiện tại"
            autocomplete="current-password"
            size="large"
            @blur="validateField('current')"
          >
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>

        <el-divider content-position="center">
          <el-text size="small" type="info">Mật khẩu mới</el-text>
        </el-divider>

        <el-form-item label="Mật khẩu mới">
          <el-input
            v-model="form.newPassword"
            type="password"
            show-password
            placeholder="Tạo mật khẩu mới"
            autocomplete="new-password"
            size="large"
          >
            <template #prefix><el-icon><Key /></el-icon></template>
          </el-input>
          <el-progress
            v-if="form.newPassword"
            :percentage="strengthPct"
            :status="strengthLevel === 'weak' ? 'exception' : strengthLevel === 'fair' ? 'warning' : 'success'"
            :format="() => strengthText"
            style="width: 100%; margin-top: 8px;"
          />
        </el-form-item>

        <!-- Requirements checklist -->
        <el-card
          v-if="form.newPassword"
          shadow="never"
          style="margin-bottom: 14px; background: var(--el-fill-color-lighter);"
          :body-style="{ padding: '14px' }"
        >
          <el-row :gutter="12">
            <el-col :span="12">
              <el-space :size="6" align="center">
                <el-icon :style="form.newPassword.length >= 6 ? 'color: var(--el-color-success)' : 'color: var(--el-text-color-placeholder)'"><Select /></el-icon>
                <el-text size="small" :type="form.newPassword.length >= 6 ? 'success' : 'info'">Ít nhất 6 ký tự</el-text>
              </el-space>
            </el-col>
            <el-col :span="12">
              <el-space :size="6" align="center">
                <el-icon :style="/[A-Z]/.test(form.newPassword) ? 'color: var(--el-color-success)' : 'color: var(--el-text-color-placeholder)'"><Select /></el-icon>
                <el-text size="small" :type="/[A-Z]/.test(form.newPassword) ? 'success' : 'info'">Chữ hoa</el-text>
              </el-space>
            </el-col>
            <el-col :span="12">
              <el-space :size="6" align="center">
                <el-icon :style="/[0-9]/.test(form.newPassword) ? 'color: var(--el-color-success)' : 'color: var(--el-text-color-placeholder)'"><Select /></el-icon>
                <el-text size="small" :type="/[0-9]/.test(form.newPassword) ? 'success' : 'info'">Số</el-text>
              </el-space>
            </el-col>
            <el-col :span="12">
              <el-space :size="6" align="center">
                <el-icon :style="/[^A-Za-z0-9]/.test(form.newPassword) ? 'color: var(--el-color-success)' : 'color: var(--el-text-color-placeholder)'"><Select /></el-icon>
                <el-text size="small" :type="/[^A-Za-z0-9]/.test(form.newPassword) ? 'success' : 'info'">Ký tự đặc biệt</el-text>
              </el-space>
            </el-col>
          </el-row>
        </el-card>

        <el-form-item
          label="Xác nhận mật khẩu mới"
          :error="passwordMismatch ? 'Mật khẩu không khớp' : ''"
        >
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="Nhập lại mật khẩu mới"
            autocomplete="new-password"
            size="large"
          >
            <template #prefix><el-icon><CircleCheck /></el-icon></template>
            <template #suffix>
              <el-icon v-if="passwordMismatch" style="color: var(--el-color-danger);"><CircleClose /></el-icon>
              <el-icon v-else-if="form.confirmPassword && !passwordMismatch" style="color: var(--el-color-success);"><CircleCheck /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item style="margin-bottom: 0;">
          <el-row justify="end" :gutter="10" style="width: 100%;">
            <el-col v-if="!isForced" :span="12">
              <el-button style="width: 100%;" size="large" @click="resetForm">Hủy</el-button>
            </el-col>
            <el-col :span="isForced ? 12 : 12" :offset="isForced ? 12 : 0">
              <el-button
                type="primary"
                native-type="submit"
                :loading="loading"
                :disabled="!canSubmit"
                size="large"
                style="width: 100%;"
              >
                {{ loading ? 'Đang cập nhật...' : 'Cập nhật mật khẩu' }}
              </el-button>
            </el-col>
          </el-row>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { CircleCheck, CircleClose, Key, Lock, Select } from "@element-plus/icons-vue";
import { reactive, ref, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { authApi } from "../../api/auth.api";
import { toast } from "../../ui/toast";

const route  = useRoute();
const router = useRouter();

const isForced = computed(() => route.query.force === "1");

const loading      = ref(false);
const alert        = ref("");
const success      = ref(false);
const shaking      = ref(false);

const form = reactive({ currentPassword: "", newPassword: "", confirmPassword: "" });
const show = reactive({ current: false, newPass: false, confirm: false });
const fieldErrors = reactive({ current: false });

function validateField(field) {
  if (field === "current") fieldErrors.current = form.currentPassword.trim().length === 0;
}

const passwordMismatch = computed(() =>
  form.confirmPassword.length > 0 && form.newPassword !== form.confirmPassword
);

const strengthScore = computed(() => {
  const p = form.newPassword;
  if (!p) return 0;
  let s = 0;
  if (p.length >= 6)           s++;
  if (p.length >= 12)          s++;
  if (/[A-Z]/.test(p))         s++;
  if (/[0-9]/.test(p))         s++;
  if (/[^A-Za-z0-9]/.test(p)) s++;
  return s;
});

const strengthPct   = computed(() => (strengthScore.value / 5) * 100);
const strengthLevel = computed(() => {
  if (strengthScore.value <= 1) return "weak";
  if (strengthScore.value <= 3) return "fair";
  return "strong";
});
const strengthText  = computed(() => {
  if (strengthScore.value <= 1) return "Yếu";
  if (strengthScore.value <= 3) return "Trung bình";
  return "Mạnh";
});

const canSubmit = computed(() =>
  form.currentPassword.trim().length > 0 &&
  form.newPassword.length >= 6 &&
  form.newPassword === form.confirmPassword
);

function triggerShake() {
  shaking.value = true;
  setTimeout(() => (shaking.value = false), 600);
}

function resetForm() {
  form.currentPassword = "";
  form.newPassword = "";
  form.confirmPassword = "";
  alert.value = "";
  success.value = false;
  fieldErrors.current = false;
}

function errToText(e) {
  const msg = e?.response?.data?.message || e?.message || "Đổi mật khẩu thất bại";
  return typeof msg === "string" ? msg : JSON.stringify(msg);
}

async function doChange() {
  alert.value = "";
  success.value = false;

  if (!form.currentPassword.trim()) { alert.value = "Vui lòng nhập mật khẩu hiện tại."; triggerShake(); return; }
  if (form.newPassword.length < 6) { alert.value = "Mật khẩu mới phải có ít nhất 6 ký tự."; triggerShake(); return; }
  if (form.newPassword !== form.confirmPassword) { alert.value = "Mật khẩu không khớp."; triggerShake(); return; }
  if (form.newPassword === form.currentPassword) { alert.value = "Mật khẩu mới phải khác mật khẩu hiện tại."; triggerShake(); return; }

  loading.value = true;
  try {
    await authApi.changePassword({ currentPassword: form.currentPassword, newPassword: form.newPassword });
    localStorage.removeItem("mustChangePassword");
    success.value = true;
    toast("Cập nhật mật khẩu thành công!", "success");
    resetForm();
    setTimeout(() => router.replace("/"), 1500);
  } catch (e) {
    alert.value = errToText(e);
    triggerShake();
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.card-shake {
  animation: shake 0.5s cubic-bezier(0.36,0.07,0.19,0.97);
}
@keyframes shake {
  0%,100%{transform:translateX(0)} 15%{transform:translateX(-8px)} 30%{transform:translateX(7px)}
  45%{transform:translateX(-5px)} 60%{transform:translateX(4px)} 75%{transform:translateX(-2px)}
}
</style>