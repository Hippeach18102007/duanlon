<template>
  <div class="container-xl">
    <div class="card ts-card">
      <div
        class="card-header bg-transparent border-0 d-flex align-items-center justify-content-between gap-2 pt-4 px-4"
      >
        <div>
          <div class="kicker">Customer Dashboard</div>
          <div class="h">Console mode</div>
        </div>
        <button class="btn btn-outline-primary ts-btn" @click="reload">
          Reload (log console)
        </button>
      </div>

      <div class="card-body px-4 pb-4">
        <div
          class="alert alert-info d-flex align-items-center gap-2 mb-0"
          role="alert"
        >
          <i class="bi bi-info-circle-fill"></i>
          <div>
            Đã bỏ hiển thị JSON trên UI. Mở DevTools (F12) → Console để xem
            user/response.
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from "vue";
import {
  getLastAuthResponse,
  getToken,
  getRole,
  getUser,
} from "../../stores/auth";

function reload() {
  console.log("[CustomerDashboard] token?", !!getToken());
  console.log("[CustomerDashboard] role=", (getRole() || "").toUpperCase());
  console.log("[CustomerDashboard] user=", getUser());
  console.log("[CustomerDashboard] lastAuthResponse=", getLastAuthResponse());
}

onMounted(reload);
</script>

<style scoped>
.kicker {
  font-size: 12px;
  opacity: 0.75;
  font-weight: 900;
  text-transform: uppercase;
}
.h {
  font-weight: 900;
  font-size: 18px;
}
</style>
