<template>
  <div style="min-height: 100vh; display: flex; align-items: center; justify-content: center;
              background: #f0f2fa; padding: 24px; overflow: hidden;">

    <el-card
      shadow="always"
      :class="{ 'card-shake': shaking }"
      style="width: 420px; max-width: 100%;"
      :body-style="{ padding: '36px 32px' }"
    >
      <!-- Header -->
      <el-space direction="vertical" :size="6" style="width: 100%; margin-bottom: 28px;">
        <el-tag type="primary" effect="plain" size="small">
          &nbsp;Cổng khách hàng
        </el-tag>
        <el-text tag="div" style="font-size: 28px; font-weight: 700; color: #111827; letter-spacing: -0.02em; line-height: 1.2;">
          Chào mừng trở lại
        </el-text>
        <el-text size="small" type="info">Đăng nhập vào tài khoản của bạn để tiếp tục</el-text>
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
      <el-form label-position="top" @submit.prevent="doLogin">
        <el-form-item label="Email hoặc tên đăng nhập">
          <el-input
            v-model="form.identifier"
            placeholder="email hoặc tên đăng nhập"
            autocomplete="username"
            size="large"
            @focus="focusedField = 'identifier'"
            @blur="focusedField = ''"
          >
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>

        <el-form-item label="Mật khẩu">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="Mật khẩu của bạn"
            autocomplete="current-password"
            size="large"
            @focus="focusedField = 'password'"
            @blur="focusedField = ''"
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
            <el-icon v-if="!loading"><Right /></el-icon>
            {{ loading ? 'Đang đăng nhập...' : 'Đăng nhập' }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- Footer -->
      <el-space direction="vertical" fill :size="10" style="margin-top: 24px; width: 100%;">
        <el-row justify="center" align="middle">
          <el-text size="small" type="info">Bạn chưa có tài khoản?&nbsp;</el-text>
          <el-link type="primary" @click="router.push('/register')">Tạo tài khoản</el-link>
        </el-row>
        <el-row justify="center" align="middle">
  <el-text size="small" type="info">Quên mật khẩu?&nbsp;</el-text>
  <el-link type="primary" @click="router.push('/forgot-password')">Đặt lại ngay</el-link>
</el-row>
        <el-divider style="margin: 0;">hoặc</el-divider>
        <el-row justify="center">
          <el-link type="info" @click="router.push('/system/login')">
            &nbsp;Hệ thống
          </el-link>
        </el-row>
      </el-space>
    </el-card>

  </div>
</template>

<script setup>
import { Goods, Lock, Monitor, Right, User } from "@element-plus/icons-vue";
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { toast } from "../../ui/toast";
import { authApi } from "../../api/auth.api";
import { useAuthStore } from "../../stores/auth";

const router = useRouter();
const auth = useAuthStore();

const loading = ref(false);
const alert = ref("");
const shaking = ref(false);
const focusedField = ref("");
const showPassword = ref(false);

const form = reactive({
  identifier: "",
  password: "",
});

function triggerShake() {
  shaking.value = true;
  setTimeout(() => (shaking.value = false), 600);
}

function errToText(e) {
  const msg = e?.response?.data?.message || e?.message || "Đăng nhập thất bại";
  return typeof msg === "string" ? msg : JSON.stringify(msg);
}

function pickTokenUser(payload) {
  const root = payload?.data ?? payload;
  const token =
    root?.token ?? root?.data?.token ?? payload?.token ?? payload?.data?.data?.token;
  const user =
    root?.user ?? root?.data?.user ?? payload?.user ?? payload?.data?.data?.user;
  return { token, user };
}

async function doLogin() {
  loading.value = true;
  alert.value = "";
  try {
    const res = await authApi.login({ ...form });
    const data = res?.data;
    const { token, user } = pickTokenUser(data);

    const rawRole = user?.role?.name ?? user?.role?.roleName ?? user?.role ?? "";
  const role = String(rawRole).toUpperCase();
    if (role !== "CUSTOMER") {
      toast("Tài khoản này không phải khách hàng. Chuyển sang đăng nhập hệ thống.", "warning");
      return router.replace("/system/login");
    }

    auth.setLastAuthResponse(data);
    auth.setSession({ token, user });

    toast("Đăng nhập thành công.", "success");
    router.replace("/");
  } catch (e) {
    alert.value = errToText(e);
    triggerShake();
  } finally {
    loading.value = false;
  }
}
</script>

<style></style>