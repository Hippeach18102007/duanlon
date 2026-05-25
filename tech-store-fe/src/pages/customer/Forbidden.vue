<template>
  <div class="auth-page">
    <el-card shadow="always" class="profile-card" style="width: 420px; max-width: 100%;">
      <el-result icon="error" title="403 — Truy cập bị từ chối">
        <template #icon>
          <div class="lock-icon-wrap">
            <el-icon :size="52" color="var(--el-color-danger)"><Lock /></el-icon>
            <div class="lock-ring ring-1" />
            <div class="lock-ring ring-2" />
          </div>
        </template>
        <template #sub-title>
          <el-text type="info">
            Bạn không có quyền truy cập trang này.<br />
            Sự việc này có thể được ghi lại.
          </el-text>
        </template>
        <template #extra>
          <el-space :size="10">
            <el-button type="danger" plain @click="goBack">
              <el-icon><ArrowLeft /></el-icon> Quay lại
            </el-button>
            <el-button text @click="goHome">
              <el-icon><House /></el-icon> Trang chủ
            </el-button>
          </el-space>
        </template>
      </el-result>
    </el-card>
  </div>
</template>

<script setup>
import { ArrowLeft, House, Lock } from "@element-plus/icons-vue";
import { useRouter } from "vue-router";

const router = useRouter();

function goBack() {
  if (window.history.length > 1) router.back();
  else router.replace("/");
}

function goHome() {
  router.replace("/");
}
</script>

<style scoped>
.auth-page {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-bg-color-page);
  padding: 24px;
  z-index: 9999;
}
.lock-icon-wrap {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.lock-ring {
  position: absolute;
  border-radius: 50%;
  border: 1.5px solid rgba(var(--el-color-danger-rgb), 0.15);
  animation: ripple 3s ease-out infinite;
}
.ring-1 { width: 72px; height: 72px; animation-delay: 0s; }
.ring-2 { width: 96px; height: 96px; animation-delay: 0.6s; }

@keyframes ripple {
  0%   { transform: scale(0.8); opacity: 0.6; }
  100% { transform: scale(1.4); opacity: 0; }
}
</style>