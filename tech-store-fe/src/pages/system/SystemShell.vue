<template>
  <el-container style="height: 100vh; overflow: hidden;">
 
    <!-- ═══════════════════════════════════
         SIDEBAR
    ═══════════════════════════════════ -->
    <el-aside
      width="256px"
      style="position: fixed; top: 0; left: 0; height: 100vh; background: #0f172a;
             display: flex; flex-direction: column; overflow: hidden;
             border-right: 1px solid rgba(255,255,255,0.06); z-index: 100;"
    >
      <!-- Brand -->
      <div
        style="padding: 20px 20px 16px; display: flex; align-items: center; width: 100%; box-sizing: border-box;"
      >
        <el-space :size="10">
          <img src="/logo.png" alt="Logo" style="height: 36px; object-fit: contain; border-radius: 8px;" />
          <el-space direction="vertical" :size="1">
            <el-text tag="b" style="color: #f8fafc; font-size: 15px; letter-spacing: -0.3px;">TechStore</el-text>
            <el-text size="small" style="color: #64748b; letter-spacing: 0.5px;">System Portal</el-text>
          </el-space>
        </el-space>
      </div>
 
      <!-- User pill -->
      <el-card
        shadow="never"
        style="margin: 0 14px 12px; background: rgba(255,255,255,0.05);
               border: 1px solid rgba(255,255,255,0.08); border-radius: 8px;"
        :body-style="{ padding: '9px 11px' }"
      >
        <el-space :size="9">
          <el-avatar
            :size="30"
            shape="square"
            style="background: linear-gradient(135deg, #3b82f6, #6366f1); border-radius: 7px;
                   flex-shrink: 0; font-size: 10px; font-weight: 700;"
          >
            {{ initials(auth.displayName) }}
          </el-avatar>
          <el-space direction="vertical" :size="3" style="min-width: 0;">
            <el-text
              size="small"
              style="color: #e2e8f0; font-weight: 600; display: block;
                     overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 148px;"
            >{{ auth.displayName }}</el-text>
            <el-tag
              size="small"
              effect="plain"
              style="color: #93c5fd; background: rgba(59,130,246,0.12);
                     border-color: rgba(59,130,246,0.28); font-size: 9px; letter-spacing: 0.8px;
                     height: 18px; padding: 0 6px;"
            >{{ (auth.role || "SYSTEM").toUpperCase() }}</el-tag>
          </el-space>
        </el-space>
      </el-card>
 
      <el-divider style="margin: 0 0 4px; border-color: rgba(255,255,255,0.06);" />
 
      <!-- Scrollable menu -->
      <el-scrollbar style="flex: 1;">
        <el-menu
          router
          :default-active="activePath"
          background-color="#0f172a"
          text-color="#94a3b8"
          active-text-color="#93c5fd"
          style="border: none; padding: 0 8px;"
        >
          <!-- Dashboard -->
          <el-menu-item index="/system/dashboard">
            <el-icon><Grid /></el-icon>
            <span>Tổng quan</span>
          </el-menu-item>
 
          <!-- Người dùng & Khách hàng -->
          <el-menu-item-group title="NGƯỜI DÙNG">
            <el-menu-item index="/system/users">
              <el-icon><UserFilled /></el-icon>
              <span>Tài khoản</span>
            </el-menu-item>
 
            <el-sub-menu index="customers">
              <template #title>
                <el-icon><User /></el-icon>
                <span>Khách hàng</span>
              </template>
              <el-menu-item index="/system/customers">Danh sách</el-menu-item>
              <el-menu-item index="/system/Toployalcustomers">Top Khách hàng</el-menu-item>
              <el-menu-item index="/system/Loyaltysummaryadmin">Tổng kết Khách hàng</el-menu-item>
            </el-sub-menu>
          </el-menu-item-group>
 
          <!-- Kho & Sản phẩm -->
          <el-menu-item-group title="KHO & SẢN PHẨM">
            <el-menu-item index="/system/categories">
              <el-icon><List /></el-icon>
              <span>Danh mục</span>
            </el-menu-item>
 
            <el-sub-menu index="products">
              <template #title>
                <el-icon><Monitor /></el-icon>
                <span>Sản phẩm</span>
              </template>
              <el-menu-item index="/system/products">Danh sách</el-menu-item>
              <el-menu-item index="/system/serials">Serials</el-menu-item>
              <el-menu-item index="/system/dashboard-products">Báo cáo Sản phẩm</el-menu-item>
            </el-sub-menu>
          </el-menu-item-group>
 
          <!-- Đơn hàng & Tài chính -->
          <el-menu-item-group title="ĐƠN HÀNG & TÀI CHÍNH">
            <el-sub-menu index="orders">
              <template #title>
                <el-icon><List /></el-icon>
                <span>Đơn hàng</span>
              </template>
              <el-menu-item index="/system/order-dashboard">Tổng quan</el-menu-item>
              <el-menu-item index="/system/orders/filter">Bộ lọc Đơn hàng</el-menu-item>
              <el-menu-item index="/system/return-manager">Quản lý Trả hàng</el-menu-item>
            </el-sub-menu>
 
            <el-menu-item index="/system/payments">
              <el-icon><CreditCard /></el-icon>
              <span>Thanh toán</span>
            </el-menu-item>
 
            <el-menu-item index="/system/pricing">
              <el-icon><Money /></el-icon>
              <span>Giá cả</span>
            </el-menu-item>
 
            <el-menu-item index="/system/promotions">
              <el-icon><Discount /></el-icon>
              <span>Khuyến mãi</span>
            </el-menu-item>
          </el-menu-item-group>
 
          <!-- Báo cáo -->
          <el-menu-item-group title="BÁO CÁO">
            <el-menu-item index="/system/reports">
              <el-icon><DataLine /></el-icon>
              <span>Báo cáo tổng hợp</span>
            </el-menu-item>
          </el-menu-item-group>
 
          <!-- Hệ thống -->
          <el-menu-item-group title="HỆ THỐNG">
            <el-sub-menu index="audits">
              <template #title>
                <el-icon><Lock /></el-icon>
                <span>Bảo mật & Kiểm toán</span>
              </template>
              <el-menu-item index="/system/audit-dashboard">Tổng quan</el-menu-item>
              <el-menu-item index="/system/audit-logs">Nhật ký Kiểm toán</el-menu-item>
              <el-menu-item index="/system/audit-report">Báo cáo Kiểm toán</el-menu-item>
              <el-menu-item index="/system/user-login-logs">Nhật ký Đăng nhập</el-menu-item>
              <el-menu-item index="/system/security-logs">Nhật ký Bảo mật</el-menu-item>
            </el-sub-menu>
 
            <el-menu-item index="/system/settings/currency">
              <el-icon><Setting /></el-icon>
              <span>Cài đặt</span>
            </el-menu-item>
          </el-menu-item-group>
        </el-menu>
      </el-scrollbar>
 
      <!-- Sidebar footer -->
      <el-space
        direction="vertical"
        fill
        :size="6"
        style="padding: 10px 14px 14px; border-top: 1px solid rgba(255,255,255,0.06);"
      >
        <el-button type="danger" plain @click="logout" style="width: 100%;">
          <el-icon><SwitchButton /></el-icon>
          Đăng xuất
        </el-button>
      </el-space>
    </el-aside>
 
    <!-- ═══════════════════════════════════
         MAIN CONTENT & HEADER BAR
    ═══════════════════════════════════ -->
    <el-container style="margin-left: 256px; height: 100vh; display: flex; flex-direction: column; background: #f8fafc;">
      
      <!-- Top Header Bar -->
      <el-header
        height="64px"
        style="background: #ffffff; border-bottom: 1px solid var(--el-border-color-lighter);
               display: flex; align-items: center; justify-content: flex-end; padding: 0 40px;
               box-shadow: 0 1px 2px 0 rgba(0,0,0,0.03);"
      >
        <!-- Header Actions -->
        <el-space :size="20" align="center">
          
          <!-- Quản lý Thông báo (Chuông) -->
          <el-popover
            placement="bottom-end"
            :width="360"
            trigger="click"
            popper-style="padding: 0; border-radius: 8px; box-shadow: var(--el-box-shadow-light);"
          >
            <template #reference>
              <div style="position: relative; cursor: pointer; padding: 6px; border-radius: 8px;
                           display: flex; align-items: center; justify-content: center;
                           transition: background 0.2s;"
                   @mouseenter="$event.currentTarget.style.background='#f1f5f9'"
                   @mouseleave="$event.currentTarget.style.background='transparent'"
              >
                <el-icon :size="22" style="color: #475569;"><Bell /></el-icon>
                <span
                  v-if="unreadCount > 0"
                  style="position: absolute; top: 4px; right: 4px;
                         min-width: 16px; height: 16px; padding: 0 4px;
                         border-radius: 8px; background: #ef4444; border: 2px solid #fff;
                         color: #fff; font-size: 10px; font-weight: 700;
                         display: flex; align-items: center; justify-content: center; line-height: 1;"
                >
                  {{ unreadCount > 99 ? '99+' : unreadCount }}
                </span>
              </div>
            </template>

            <!-- Nội dung popover thông báo -->
            <div style="display: flex; flex-direction: column; max-height: 480px;">
              <!-- Header -->
              <div style="padding: 14px 16px; border-bottom: 1px solid var(--el-border-color-lighter); display: flex; justify-content: space-between; align-items: center;">
                <span style="font-weight: 700; color: #0f172a;">Yêu cầu trả hàng</span>
                <el-tag v-if="unreadCount > 0" type="success" size="small" effect="plain">{{ unreadCount }} mới</el-tag>
              </div>

              <!-- List -->
              <el-scrollbar style="max-height: 360px;">
                <div v-if="notifications.length === 0" style="padding: 30px 20px; text-align: center;">
                  <el-empty description="Không có thông báo mới nào" :image-size="60" />
                </div>
                <div v-else>
                  <div
                    v-for="notif in notifications"
                    :key="notif.id"
                    style="padding: 12px 16px; border-bottom: 1px solid #f1f5f9; cursor: pointer; transition: background 0.2s;"
                    @click="handleNotifClick(notif)"
                  >

                    <div style="display: flex; gap: 10px; padding-left: 6px;">
                      <!-- Icon -->
                      <div style="flex-shrink: 0; width: 34px; height: 34px; border-radius: 50%;
                                  background: #fff7ed; border: 1.5px solid #fed7aa;
                                  display: flex; align-items: center; justify-content: center;">
                        <span v-if="notif.icon" style="font-size: 16px; line-height: 1;">{{ notif.icon }}</span>
                        <el-icon v-else :size="17" style="color: #f97316;"><Bell /></el-icon>
                      </div>

                      <!-- Content -->
                      <div style="flex: 1; min-width: 0;">
                        <div style="font-size: 13px; font-weight: 600; color: #1e293b; margin-bottom: 3px; line-height: 1.3;">
                          {{ notif.title }}
                        </div>
                        <div
                          style="font-size: 12px; color: #64748b; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; margin-bottom: 4px;"
                          v-html="formatMessage(notif.message)"
                        ></div>
                        <div style="font-size: 10px; color: #94a3b8;">
                          {{ formatTimeAgo(notif.createdAt) }}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </el-scrollbar>

              <!-- Footer -->
              <div style="padding: 10px 16px; border-top: 1px solid var(--el-border-color-lighter); text-align: center; background: #f8fafc;">
                <el-link type="primary" :underline="false" style="font-size: 12px;" @click="showAllHistory = true">
                  Xem tất cả lịch sử
                </el-link>
              </div>
            </div>
          </el-popover>

          <!-- User Profile info -->
          <el-dropdown trigger="click">
            <div style="display: flex; align-items: center; gap: 8px; cursor: pointer;">
              <el-avatar :size="32" style="background: var(--el-color-primary); font-weight: 700;">
                {{ initials(auth.displayName) }}
              </el-avatar>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>Xin chào, {{ auth.displayName }}</el-dropdown-item>
                <el-dropdown-item divided @click="logout">
                  <el-icon><SwitchButton /></el-icon> Đăng xuất
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

        </el-space>
      </el-header>

      <!-- Scrollable sub-view -->
      <el-main style="flex: 1; overflow-y: auto; padding: 32px 40px 60px;">
        <router-view />
      </el-main>

    </el-container>

    <!-- ═══════════════════════════════════
         DIALOG: ALL NOTIFICATIONS HISTORY
    ═══════════════════════════════════ -->
    <el-dialog
      v-model="showAllHistory"
      title="Lịch sử yêu cầu trả hàng"
      width="640px"
      destroy-on-close
      class="profile-card"
      @open="onHistoryDialogOpen"
    >

      <el-scrollbar max-height="450px">
        <div v-if="notifications.length === 0" style="padding: 40px 0; text-align: center;">
          <el-empty description="Không có lịch sử thông báo nào" />
        </div>
        <div v-else style="display: flex; flex-direction: column; gap: 12px; padding-right: 8px;">
          <el-card
            v-for="notif in notifications"
            :key="notif.id"
            shadow="never"
            class="profile-card"
            style="border-color: #e2e8f0; transition: all 0.2s; cursor: pointer;"
            @click="handleNotifClick(notif)"
          >
            <div style="display: flex; gap: 12px;">
              <el-avatar
                :size="36"
                :style="{ background: getNotifIconBg(notif.type) }"
                style="flex-shrink: 0; display: flex; align-items: center; justify-content: center;"
              >
                <span v-if="notif.icon" style="font-size: 18px; line-height: 1; display: inline-block;">
                  {{ notif.icon }}
                </span>
                <el-icon v-else :size="18" style="color: #ffffff;">
                  <component :is="getNotifIcon(notif.type)" />
                </el-icon>
              </el-avatar>
              <div style="flex: 1; min-width: 0;">
                <div style="font-size: 14px; font-weight: 700; color: #1e293b; margin-bottom: 4px;">
                  {{ notif.title }}
                </div>
                <div
                  style="font-size: 13px; color: #475569; line-height: 1.4; margin-bottom: 6px;"
                  v-html="formatMessage(notif.message)"
                ></div>
                <div style="font-size: 11px; color: #94a3b8;">
                  {{ formatDateTime(notif.createdAt) }}
                </div>
              </div>
            </div>
          </el-card>
        </div>
      </el-scrollbar>
    </el-dialog>
 
  </el-container>
</template>
 
<script setup>
import {
  CreditCard, DataLine, Discount, Grid, List, Lock,
  Monitor, Money, RefreshLeft, Setting, SwitchButton, User, UserFilled,
  Bell, Calendar, ShoppingCart, StarFilled, Trophy, Timer, Opportunity, InfoFilled
} from "@element-plus/icons-vue";
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { confirmModal } from "../../ui/confirm";
import { toast } from "../../ui/toast";
import { ElNotification } from "element-plus";
import { notificationsApi } from "../../api/notification.api";
import {
  clearLastAuthResponse,
  clearSession,
  useAuthStore,
} from "../../stores/auth";
 
const route  = useRoute();
const router = useRouter();
const auth   = useAuthStore();
 
const activePath = computed(() => route.path);
 
const initials = (name) =>
  name ? name.split(" ").slice(-2).map(n => n[0]).join("").toUpperCase() : "SY";

// Tiêu đề trang dựa trên Meta title trong Router
const pageTitle = computed(() => route.meta?.title || "Hệ thống quản lý");

// ── QUẢN LÝ THÔNG BÁO ADMIN ──────────────────
// Admin chỉ xem thông báo RETURN_REQUEST từ endpoint chuyên biệt
// vì notification được gắn customer_id, admin không có customer_id
const unreadCount      = ref(0);
const notifications    = ref([]);  // Chỉ chứa RETURN_REQUEST
const showAllHistory   = ref(false);
const shownNotifIds    = new Set(); // Chặn việc hiển thị trùng lập Toast
let notifTimer         = null;

// Lấy icon tương ứng loại thông báo
function getNotifIcon(type) {
  return ({
    WELCOME:              User,
    BIRTHDAY:             Calendar,
    PURCHASE_REMINDER:    ShoppingCart,
    WINBACK:              StarFilled,
    VIP_TIER_UPGRADE:     Trophy,
    SPIN_EXPIRY_WARNING:  Timer,
    ORDER_NEW:            Opportunity, // Đơn hàng mới
    ORDER_PAID:           CreditCard,  // Đơn hàng thanh toán
    ORDER_UPDATE:         CreditCard,  // Cập nhật đơn hàng (Thanh toán thành công)
    ORDER_STATUS:         Opportunity, // Trạng thái đơn hàng (Đã giao, đã duyệt...)
    LOYALTY_POINTS:       StarFilled,  // Điểm thưởng tích lũy
    ORDER_RETURN:         RefreshLeft, // Yêu cầu trả hàng mới
    RETURN_REQUEST:       RefreshLeft, // Yêu cầu trả hàng mới
    RETURN_ORDER:         RefreshLeft, // Yêu cầu trả hàng mới
  }[type] ?? Bell);
}

// Lấy màu nền tương ứng loại thông báo
function getNotifIconBg(type) {
  return ({
    WELCOME:              "var(--el-color-primary)",
    BIRTHDAY:             "var(--el-color-danger)",
    PURCHASE_REMINDER:    "var(--el-color-warning)",
    WINBACK:              "var(--el-color-success)",
    VIP_TIER_UPGRADE:     "#8b5cf6", // Tím
    SPIN_EXPIRY_WARNING:  "#f59e0b", // Cam
    ORDER_NEW:            "#3b82f6", // Xanh dương
    ORDER_PAID:           "#10b981", // Xanh lá
    ORDER_UPDATE:         "#10b981", // Xanh lá
    ORDER_STATUS:         "#3b82f6", // Xanh dương
    LOYALTY_POINTS:       "#f59e0b", // Cam vàng
    ORDER_RETURN:         "#f43f5e", // Đỏ Rose khẩn cấp
    RETURN_REQUEST:       "#ffffff", // Đỏ Rose khẩn cấp
    RETURN_ORDER:         "#f43f5e", // Đỏ Rose khẩn cấp
  }[type] ?? "#64748b");
}

// Format message để có line break
function formatMessage(msg) {
  if (!msg) return "";
  return msg.replace(/\n/g, "<br>");
}

// Format thời gian cách đây bao lâu
function formatTimeAgo(dateStr) {
  if (!dateStr) return "";
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return "Vừa xong";
  if (mins < 60) return `${mins} phút trước`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours} giờ trước`;
  const days = Math.floor(hours / 24);
  return `${days} ngày trước`;
}

// Format ngày giờ đầy đủ
function formatDateTime(dateStr) {
  if (!dateStr) return "";
  const d = new Date(dateStr);
  return d.toLocaleString("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
    day: "2-digit",
    month: "2-digit",
    year: "numeric"
  });
}

// Phát âm thanh chuông "ting ting" thời gian thực không cần file download (dùng Web Audio API)
function playTingTing() {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)();
    
    // Nốt thứ nhất
    const osc1 = ctx.createOscillator();
    const gain1 = ctx.createGain();
    osc1.connect(gain1);
    gain1.connect(ctx.destination);
    osc1.type = "sine";
    osc1.frequency.setValueAtTime(587.33, ctx.currentTime); // D5
    gain1.gain.setValueAtTime(0.08, ctx.currentTime);
    osc1.start();
    gain1.gain.exponentialRampToValueAtTime(0.00001, ctx.currentTime + 0.15);
    osc1.stop(ctx.currentTime + 0.15);
    
    // Nốt thứ hai
    setTimeout(() => {
      const osc2 = ctx.createOscillator();
      const gain2 = ctx.createGain();
      osc2.connect(gain2);
      gain2.connect(ctx.destination);
      osc2.type = "sine";
      osc2.frequency.setValueAtTime(880, ctx.currentTime); // A5
      gain2.gain.setValueAtTime(0.08, ctx.currentTime);
      osc2.start();
      gain2.gain.exponentialRampToValueAtTime(0.00001, ctx.currentTime + 0.3);
      osc2.stop(ctx.currentTime + 0.3);
    }, 80);
  } catch (e) {
    console.warn("[Sound] Web Audio API blocked or not supported:", e);
  }
}

// Khi mở dialog lịch sử → reset badge về 0 (chỉ UI, không gọi API)
function onHistoryDialogOpen() {
  unreadCount.value = 0;
}

// Xử lý khi click vào item thông báo → luôn đi đến trang quản lý trả hàng
function handleNotifClick() {
  router.push("/system/return-manager");
}

// Quét thông báo RETURN_REQUEST từ server (chỉ dành cho Admin)
async function fetchNotificationsData() {
  try {
    // 1. Fetch danh sách tất cả RETURN_REQUEST từ endpoint Admin riêng
    const listRes = await notificationsApi.getReturnRequestNotifications();
    const newList = listRes.data || [];

    // 2. Tính số lượng chưa đọc
    const prevCount = unreadCount.value;
    const newCount = newList.filter(n => !n.isRead).length;
    unreadCount.value = newCount;
    notifications.value = newList;

    // 3. Nếu có thông báo mới chưa hiển thị → bắn toast + chuông
    if (newCount > prevCount || newList.some(n => !n.isRead && !shownNotifIds.has(n.id))) {
      const unreadList = newList.filter(n => !n.isRead);
      let hasNewToast = false;

      unreadList.forEach(notif => {
        if (!shownNotifIds.has(notif.id)) {
          shownNotifIds.add(notif.id);
          hasNewToast = true;

          ElNotification({
            title: notif.icon ? `${notif.icon} ${notif.title}` : notif.title,
            dangerouslyUseHTMLString: true,
            message: `<div style="font-size: 13px; color: #475569;">${formatMessage(notif.message)}</div>`,
            type: "warning",
            duration: 8000,
            position: "top-right",
          });
        }
      });

      if (hasNewToast) {
        playTingTing();
      }
    }
  } catch (err) {
    console.warn("[Notif] Bỏ qua lỗi fetch RETURN_REQUEST (Backend chưa xử lý xong):", err?.message);
  }
}

// Khởi tạo khi load trang — ghi nhớ các ID cũ để không spam toast
async function initNotifications() {
  try {
    const listRes = await notificationsApi.getReturnRequestNotifications();
    notifications.value = listRes.data || [];
    // Ghi nhớ toàn bộ ID cũ (kể cả chưa đọc) để không spam toast ngay khi mở trang
    notifications.value.forEach(n => shownNotifIds.add(n.id));
    unreadCount.value = notifications.value.filter(n => !n.isRead).length;
  } catch (err) {
    console.warn("[Notif] Backend chưa sẵn sàng hoặc chưa có endpoint return-requests.");
  }
}
 
onMounted(async () => {
  // Khởi tạo dữ liệu
  await initNotifications();

  // Bắt đầu short polling mỗi 15 giây
  notifTimer = setInterval(fetchNotificationsData, 15000);
});

onUnmounted(() => {
  if (notifTimer) clearInterval(notifTimer);
});

function go(path) {
  if (path && path !== route.path) router.push(path);
}
 
async function logout() {
  const ok = await confirmModal(
    "Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?",
    "Thông báo",
    "Đăng xuất",
    true
  );
  if (!ok) return;
  clearSession();
  clearLastAuthResponse();
  toast("Đã đăng xuất thành công.", "success");
  router.push("/system/login");
}
</script>
 
<style>
.el-header {
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.05);
}
</style>