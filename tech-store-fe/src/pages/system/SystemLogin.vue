<template>
  <div style="position: fixed; inset: 0; display: flex; align-items: center; justify-content: center;
              background: #eef4f0; padding: 24px; overflow-y: auto;">

    <el-card
      shadow="always"
      :class="{ 'card-shake': shaking }"
      style="width: 420px; max-width: 100%;"
      :body-style="{ padding: '36px 32px' }"
    >
      <!-- Header -->
      <el-row justify="center" style="margin-bottom: 6px;">
        <el-tag type="success" effect="plain" size="small">Cổng hệ thống</el-tag>
      </el-row>

      <el-row justify="center" style="margin-bottom: 6px;">
        <el-text tag="div" style="font-size: 28px; font-weight: 700; color: #0f2318; letter-spacing: -0.03em; text-align: center; line-height: 1.2;">
          Truy cập hệ thống
        </el-text>
      </el-row>

      <el-row justify="center" style="margin-bottom: 28px;">
        <el-text size="small" type="info">Xác thực nhân viên &amp; quản trị</el-text>
      </el-row>

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
        <el-form-item label="Tên đăng nhập">
          <el-input
            v-model="form.identifier"
            placeholder="ví dụ: admin / staff@mail.com"
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
            placeholder="••••••••••"
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
            type="success"
            native-type="submit"
            :loading="loading"
            size="large"
            style="width: 100%;"
          >
            {{ loading ? 'Đang xác thực...' : 'Đăng nhập' }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- Footer -->
      <el-row justify="center" style="margin-top: 16px;">
        <el-text size="small" type="info" style="text-align: center;">
          HỆ THỐNG → Dashboard &nbsp;·&nbsp; SALES → Bán hàng &nbsp;·&nbsp; KHO → Đơn hàng
        </el-text>
      </el-row>

      <el-divider style="margin: 16px 0;">hoặc</el-divider>

      <el-row justify="center">
        <el-link type="info" @click="router.push('/login')">
          &nbsp;Đến trang đăng nhập khách hàng
        </el-link>
      </el-row>
    </el-card>

  </div>
</template>

<script setup>
import { Lock, Monitor, User } from "@element-plus/icons-vue";
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
    if (role === "CUSTOMER") {
      toast("Đây là tài khoản KHÁCH HÀNG. Chuyển hướng tới cổng khách hàng.", "warning");
      return router.replace("/login");
    }

    auth.setLastAuthResponse(data);
    auth.setSession({ token, user });

    toast("Đăng nhập hệ thống thành công.", "success");

    if (role === "INVENTORY") {
      router.replace("/inventory/orders/paid");
    } else if (role === "SALES") {
      router.replace("/sales/pos");
    } else {
      router.replace("/system/dashboard");
    }
  } catch (e) {
    alert.value = errToText(e);
    triggerShake();
  } finally {
    loading.value = false;
  }
}
</script>

<style></style>