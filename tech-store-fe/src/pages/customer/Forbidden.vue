<template>
  <div style="position: fixed; inset: 0; display: flex; align-items: center; justify-content: center;
              background: #f0f2fa; padding: 24px; z-index: 9999;">
    <el-card
      shadow="always"
      style="width: 400px; max-width: 100%; text-align: center;"
      :body-style="{ padding: '44px 40px 40px' }"
    >
      <el-space direction="vertical" fill :size="0" style="width: 100%;">
        <el-tag type="danger" effect="plain" round style="margin-bottom: 24px; font-size: 11px; letter-spacing: 0.1em;">
          403
        </el-tag>

        <div style="position: relative; display: inline-flex; align-items: center; justify-content: center; margin-bottom: 24px;">
          <el-icon :size="48" style="color: var(--el-color-danger); z-index: 1;"><Lock /></el-icon>
          <div class="lock-ring ring-1"></div>
          <div class="lock-ring ring-2"></div>
        </div>

        <el-text tag="div" style="font-size: 28px; font-weight: 700; letter-spacing: -0.02em; margin-bottom: 10px;">
          Access Denied
        </el-text>
        <el-text type="info" style="line-height: 1.7; display: block; margin-bottom: 28px;">
          You don't have permission to view this page.<br>
          This incident may be logged.
        </el-text>

        <el-divider style="margin: 0 0 28px;" />

        <el-space :size="10" style="justify-content: center;">
          <el-button type="danger" plain @click="goBack">
            <el-icon><ArrowLeft /></el-icon> Go Back
          </el-button>
          <el-button text @click="goHome">
            <el-icon><House /></el-icon> Home
          </el-button>
        </el-space>
      </el-space>
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