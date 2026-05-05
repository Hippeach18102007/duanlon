<template>
  <Analytics />
  <el-container style="height: 100%; overflow: hidden;">

    <!-- ─── Header ─────────────────────────────────────── -->
    <el-header v-if="showHeader" height="72px" style="position: sticky; top: 0; z-index: 100; background: rgba(255,255,255,0.92); backdrop-filter: blur(16px); border-bottom: 1px solid var(--el-border-color-light); box-shadow: var(--el-box-shadow-light);">
      <el-row align="middle" justify="space-between" style="height: 100%">

        <!-- Left: Brand + Search -->
        <el-col :span="14">
          <el-space :size="16">

            <!-- Brand -->
            <el-link :underline="false" @click="goHome">
              <el-space :size="10">
                <el-avatar :size="36" style="background: #1d4ed8; flex-shrink: 0">
                  <el-icon :size="16"><Monitor /></el-icon>
                </el-avatar>
                <el-space direction="vertical" :size="1">
                  <el-text tag="b">Tech Store</el-text>
                  <el-text size="small" type="info">Laptop · Gaming · Workstation</el-text>
                </el-space>
              </el-space>
            </el-link>

            <!-- Search -->
            <el-input
              v-if="showSearch"
              v-model="q"
              placeholder="Tìm laptop, CPU, RAM…"
              style="width: 310px"
              clearable
              @input="emitSearch"
              @clear="() => { q = ''; emitSearch(); }"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>

          </el-space>
        </el-col>

        <!-- Right: Controls -->
        <el-col :span="10">
          <el-row justify="end" align="middle">
            <el-space :size="8">

              <!-- Customer action buttons -->
              <template v-if="isAuthed && isCustomer">

                <!-- Sự kiện -->
                <el-tooltip content="Sự kiện" placement="bottom">
                  <el-badge is-dot>
                    <el-button :icon="Trophy" circle text @click="$router.push('/spin-wheel')" />
                  </el-badge>
                </el-tooltip>

                <!-- Thông báo -->
                <el-tooltip content="Thông báo" placement="bottom">
                  <el-badge :value="unreadNotificationCount" :max="9" :hidden="unreadNotificationCount === 0">
                    <el-button :icon="Bell" circle text @click="viewAllNotifications" />
                  </el-badge>
                </el-tooltip>

                <!-- Giỏ hàng -->
                <el-tooltip content="Giỏ hàng" placement="bottom">
                  <el-badge :value="cartStore.count" :max="9" :hidden="cartStore.count === 0">
                    <el-button :icon="ShoppingBag" circle text type="primary" @click="$router.push('/cart')" />
                  </el-badge>
                </el-tooltip>

              </template>

              <!-- Guest -->
              <el-button v-if="!isAuthed" type="primary" plain @click="goLogin">
                <el-icon><Right /></el-icon>
                Đăng nhập
              </el-button>

              <!-- Authed user dropdown -->
              <el-dropdown v-if="isAuthed" trigger="click" placement="bottom-end">
                <el-button text>
                  <el-space :size="8">
                    <el-avatar :size="32">{{ avatarLetter }}</el-avatar>
                    <el-space direction="vertical" :size="1">
                      <el-text tag="b" size="small">{{ displayName }}</el-text>
                      <el-text size="small" type="info">{{ portalLabel }}</el-text>
                    </el-space>
                    <el-icon><ArrowDown /></el-icon>
                  </el-space>
                </el-button>

                <template #dropdown>
                  <el-dropdown-menu>
                    <div style="padding: 10px 14px 6px">
                      <el-space :size="10">
                        <el-avatar>{{ avatarLetter }}</el-avatar>
                        <el-space direction="vertical" :size="4">
                          <el-text tag="b" type="primary">{{ displayName }}</el-text>
                          <el-tag size="small" type="primary" effect="plain">{{ portalLabel }}</el-tag>
                        </el-space>
                      </el-space>
                    </div>
                    <el-divider style="margin: 4px 0" />

                    <el-dropdown-item v-if="isSystemUser" @click="router.push('/system/dashboard')">
                      <el-icon><Grid /></el-icon> System Dashboard
                    </el-dropdown-item>
                    <el-dropdown-item v-if="isCustomer" @click="router.push('/profile')">
                      <el-icon><User /></el-icon> Thông tin cá nhân
                    </el-dropdown-item>
                    <el-dropdown-item v-if="isCustomer" @click="router.push('/my-orders')">
                      <el-icon><Document /></el-icon> Đơn hàng của tôi
                    </el-dropdown-item>
                    <el-dropdown-item v-if="isCustomer" @click="router.push('/change-password')">
                      <el-icon><Lock /></el-icon> Đổi mật khẩu
                    </el-dropdown-item>

                    <el-divider style="margin: 4px 0" />
                    <el-dropdown-item @click="logout">
                      <el-icon><SwitchButton /></el-icon> Đăng xuất
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>

            </el-space>
          </el-row>
        </el-col>

      </el-row>
    </el-header>

    <!-- ─── Main ───────────────────────────────────────── -->
    <el-main :style="mainStyle">
      <router-view />
    </el-main>

    <!-- ─── Notification Dialog ─────────────────────────── -->
    <el-dialog v-model="notificationsDialog" width="540px" align-center>
      <template #header>
        <el-space :size="8">
          <el-icon><Bell /></el-icon>
          <el-text tag="b" size="large">Thông báo</el-text>
        </el-space>
      </template>

      <el-scrollbar max-height="420px">
        <el-empty v-if="allNotifications.length === 0" description="Chưa có thông báo nào" />
        <el-space v-else direction="vertical" fill style="width: 100%">
          <el-card
            v-for="notif in allNotifications"
            :key="notif.id"
            shadow="never"
            :body-style="{ padding: '12px 16px' }"
          >
            <el-row align="middle" :gutter="8">
              <el-col :span="2">
                <el-icon style="font-size: 20px; color: var(--el-color-primary);">
                  <component :is="notifIconComponent(notif.type)" />
                </el-icon>
              </el-col>
              <el-col :span="20">
                <el-space direction="vertical" :size="2" fill>
                  <el-row align="middle" justify="space-between">
                    <el-text tag="b" type="primary">{{ notif.title }}</el-text>
                    <el-tag v-if="!notif.isRead" size="small" type="danger" effect="plain">Mới</el-tag>
                  </el-row>
                  <el-text size="small"><span v-html="formatMessage(notif.message)" /></el-text>
                  <el-text size="small" type="info">{{ formatDate(notif.createdAt) }}</el-text>
                </el-space>
              </el-col>
              <el-col :span="2">
                <el-tooltip v-if="!notif.isRead" content="Đánh dấu đã đọc" placement="left">
                  <el-button
                    :icon="Check"
                    circle
                    text
                    type="success"
                    size="small"
                    @click="markNotifAsRead(notif.id)"
                  />
                </el-tooltip>
              </el-col>
            </el-row>
          </el-card>
        </el-space>
      </el-scrollbar>

      <template #footer>
        <el-space>
          <el-button text @click="notificationsDialog = false">Đóng</el-button>
          <el-button type="primary" plain @click="markAllAsRead">Đánh dấu tất cả đã đọc</el-button>
        </el-space>
      </template>
    </el-dialog>

  </el-container>
</template>

<script setup>
import {
  ArrowDown, Bell, Calendar, Check, Document, Grid, Lock,
  Monitor, Present, Right, Search, ShoppingBag, ShoppingCart, StarFilled,
  SwitchButton, Timer, Trophy, User,
} from "@element-plus/icons-vue";
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { toast } from "./ui/toast";
import { clearLastAuthResponse, clearSession, useAuthStore } from "./stores/auth";
import { useCartStore } from "./stores/cart";
import http from "./api/http";
import { Analytics } from '@vercel/analytics/vue';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const cartStore = useCartStore();

const q = ref("");
const unreadNotificationCount = ref(0);
const notificationsDialog = ref(false);
const allNotifications = ref([]);

const isAuthed = computed(() => auth.isAuthed);
const isCustomer = computed(() => auth.isCustomer);
const isSystemUser = computed(() => isAuthed.value && !isCustomer.value);
const displayName = computed(() => auth.displayName);
const portalLabel = computed(() => (isCustomer.value ? "Khách hàng" : "Hệ thống"));

const avatarLetter = computed(() => {
  const s = String(displayName.value || "U").trim();
  return (s[0] || "U").toUpperCase();
});

const showHeader = computed(() => route.meta?.hideHeader !== true);
const showSearch = computed(
  () => route.name === "home" && (route.meta?.portal || "customer") === "customer",
);

const showFooter = computed(
  () => route.meta?.hideHeader !== true && (route.meta?.portal || "customer") === "customer",
);

// el-main style: system pages get no padding and fill remaining height
const mainStyle = computed(() => {
  if (isSystemUser.value || route.meta?.hideHeader === true) {
    const headerH = showHeader.value ? 72 : 0;
    return {
      padding: '0',
      height: `calc(100% - ${headerH}px)`,
      overflow: 'hidden',
    };
  }
  // Customer pages: padding 0, ch-wrap manages its own internal padding
  return { padding: '0', overflow: 'auto' };
});

function emitSearch() {
  window.dispatchEvent(new CustomEvent("products:search", { detail: q.value }));
}
function goHome() {
  if (!isAuthed.value) return router.push("/");
  return isCustomer.value ? router.push("/") : router.push("/system/dashboard");
}
function goLogin() { router.push("/login"); }
async function logout() {
  clearSession(); clearLastAuthResponse();
  toast("Đăng xuất thành công. Hẹn gặp lại!", "success", { title: "Đã đăng xuất" });
  router.push(isSystemUser.value ? "/system/login" : "/login");
}

// Tập hợp các ID thông báo đã hiển thị popup — tránh spam khi reload
const shownNotifIds = new Set();

async function loadNotificationCount() {
  if (!isCustomer.value) return;
  try {
    const prevCount = unreadNotificationCount.value;
    const res = await http.get("/api/auth/notifications/my/unread-count");
    const newCount = res.data?.unreadCount || 0;
    unreadNotificationCount.value = newCount;
    // Chỉ check thông báo mới khi count tăng lên
    if (newCount > prevCount) {
      await checkAndShowNewNotifications();
    }
  } catch {}
}
async function loadAllNotifications() {
  try {
    const res = await http.get("/api/auth/notifications/my");
    allNotifications.value = res.data || [];
  } catch {}
}

/** Map loại thông báo backend → loại toast */
function notifToastType(type) {
  return ({ WELCOME: "success", BIRTHDAY: "success", VIP_TIER_UPGRADE: "success",
            PURCHASE_REMINDER: "info", WINBACK: "info",
            SPIN_EXPIRY_WARNING: "warning" }[type] ?? "info");
}

/** Khởi tạo Set với tất cả ID hiện có — không popup thông báo cũ khi mới load trang */
async function initShownNotifIds() {
  try {
    const res = await http.get("/api/auth/notifications/my");
    (res.data || []).forEach(n => shownNotifIds.add(n.id));
  } catch {}
}

/** Fetch danh sách, tìm thông báo chưa đọc chưa hiển thị và popup từng cái */
async function checkAndShowNewNotifications() {
  try {
    const res = await http.get("/api/auth/notifications/my");
    allNotifications.value = res.data || [];
    const unread = allNotifications.value.filter(n => !n.isRead);
    unread.forEach(notif => {
      if (!shownNotifIds.has(notif.id)) {
        shownNotifIds.add(notif.id);
        toast(
          formatMessage(notif.message),
          notifToastType(notif.type),
          { title: notif.title, delay: 5000, html: true }
        );
      }
    });
  } catch {}
}

function viewAllNotifications() {
  notificationsDialog.value = true;
  loadAllNotifications();
}
async function markNotifAsRead(id) {
  try {
    await http.put(`/api/auth/notifications/${id}/read`);
    await Promise.all([loadAllNotifications(), loadNotificationCount()]);
  } catch { toast("Không thể đánh dấu đã đọc", "error", { title: "Lỗi cập nhật" }); }
}
async function markAllAsRead() {
  try {
    await http.put("/api/auth/notifications/read-all");
    await Promise.all([loadAllNotifications(), loadNotificationCount()]);
    toast("Đã đánh dấu tất cả thông báo là đã đọc", "success", { title: "Cập nhật thành công" });
  } catch { toast("Không thể cập nhật thông báo", "error", { title: "Lỗi cập nhật" }); }
}

function notifIconComponent(type) {
  return ({
    WELCOME:              Present,
    BIRTHDAY:             Calendar,
    PURCHASE_REMINDER:    ShoppingCart,
    WINBACK:              StarFilled,
    VIP_TIER_UPGRADE:     Trophy,
    SPIN_EXPIRY_WARNING:  Timer,
  }[type] ?? Bell);
}
function formatMessage(msg) { return msg ? msg.replace(/\n/g, "<br>") : ""; }
function formatDate(d) { return new Date(d).toLocaleDateString("vi-VN"); }

function onAutoLogout(e) {
  clearSession(); clearLastAuthResponse();
  toast("Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.", "warning", { title: "Phiên hết hạn", delay: 4500 });
  router.push("/login");
}

let notifInterval = null;
watch(isCustomer, (val) => {
  if (val) {
    cartStore.refreshCount();
    // Khởi tạo Set trước, rồi mới load count (để không popup thông báo cũ)
    initShownNotifIds().then(() => loadNotificationCount());
    notifInterval = setInterval(loadNotificationCount, 30000);
  } else {
    if (notifInterval) { clearInterval(notifInterval); notifInterval = null; }
    unreadNotificationCount.value = 0;
    shownNotifIds.clear();
  }
}, { immediate: true });

onMounted(() => {
  window.addEventListener("auth:logout", onAutoLogout);
  window.addEventListener("notifications:updated", loadNotificationCount);
  window.addEventListener("header:open-notifications", viewAllNotifications);
});
onBeforeUnmount(() => {
  window.removeEventListener("auth:logout", onAutoLogout);
  window.removeEventListener("notifications:updated", loadNotificationCount);
  window.removeEventListener("header:open-notifications", viewAllNotifications);
  if (notifInterval) clearInterval(notifInterval);
});
</script>

<style></style>