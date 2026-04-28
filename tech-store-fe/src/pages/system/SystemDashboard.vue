<template>
  <div style="background: #f6f7f9; min-height: 100vh;">
    <div style="max-width: 1440px; margin: 0 auto; display: flex; flex-direction: column; gap: 20px;">

      <!-- Header -->
      <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:20px; margin-bottom:22px; flex-wrap:wrap;">
        <div>
          <div style="font-size:11px; font-weight:700; letter-spacing:0.08em; text-transform:uppercase; color:var(--el-text-color-secondary); margin-bottom:6px; display:flex; align-items:center; gap:5px;">
            <el-icon><Setting /></el-icon> Hệ thống
          </div>
          <div style="font-size:28px; font-weight:800; letter-spacing:-0.03em; margin-bottom:4px;">Admin <span style="color:var(--el-color-primary);">Dashboard</span></div>
          <div style="font-size:13px; color:var(--el-text-color-secondary);">Truy cập nhanh các module quản trị</div>
        </div>
        <div style="display:flex; align-items:center; gap:8px; flex-shrink:0; padding-top:4px; flex-wrap:wrap;">
          <el-button plain @click="reload">
            <el-icon><Refresh /></el-icon> Làm mới
          </el-button>
        </div>
      </div>

      <!-- Module cards -->
      <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 12px;">
        <el-card
          v-for="c in cards"
          :key="c.title"
          shadow="never"
          :body-style="{ padding: '16px 18px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '14px', transition: 'all 0.15s' }"
          class="module-card"
          @click="$router.push(c.to)"
        >
          <div :style="`width:44px;height:44px;border-radius:11px;background:${c.color}18;color:${c.color};display:flex;align-items:center;justify-content:center;font-size:22px;flex-shrink:0;`">
            <el-icon><component :is="c.icon" /></el-icon>
          </div>
          <div style="flex:1; min-width:0;">
            <el-text tag="div" style="font-size:13.5px; font-weight:700;">{{ c.title }}</el-text>
            <el-text size="small" type="info" style="white-space:nowrap; overflow:hidden; text-overflow:ellipsis; display:block;">{{ c.desc }}</el-text>
          </div>
          <el-icon style="color: var(--el-text-color-placeholder); flex-shrink:0;"><ArrowRight /></el-icon>
        </el-card>
      </div>

      <!-- Loyalty Reset -->
      <el-card shadow="never">
        <template #header>
          <el-row align="middle" justify="space-between" style="flex-wrap: wrap; gap: 12px;">
            <el-space direction="vertical" :size="4">
              <el-text size="small" type="info" style="text-transform: uppercase; letter-spacing: 0.08em; font-weight: 700;">Quản lý điểm thưởng</el-text>
              <el-text tag="div" style="font-size:18px; font-weight:800; letter-spacing:-0.02em;">Loyalty Reset</el-text>
              <el-text size="small" type="info">Thao tác hạ hạng &amp; reset điểm thủ công — chỉ dành cho Admin</el-text>
            </el-space>
            <el-tag type="danger" effect="plain" round>Admin only</el-tag>
          </el-row>
        </template>

        <el-row :gutter="14" style="margin-bottom: 16px;">
          <!-- Monthly -->
          <el-col :xs="24" :sm="12">
            <el-card
              shadow="never"
              :body-style="{ padding: '18px' }"
              style="background: #fffbeb; border-color: #fde68a;"
            >
              <el-space direction="vertical" fill :size="12" style="width:100%;">
                <div style="font-size:28px; line-height:1; color: var(--el-color-warning);">
                  <el-icon><Calendar /></el-icon>
                </div>
                <el-space direction="vertical" :size="6">
                  <el-text tag="div" style="font-size:14px; font-weight:700;">Kiểm tra không hoạt động tháng</el-text>
                  <el-text size="small" type="info" style="line-height:1.65;">
                    Hạ 1 bậc tier với khách hàng không có giao dịch trong <strong>30 ngày qua</strong>.
                  </el-text>
                </el-space>
                <el-space wrap :size="5">
                  <el-tag v-for="chip in monthlyChips" :key="chip.text"
                    size="small" effect="plain"
                    :type="chip.muted ? 'info' : ''"
                    :style="chip.muted ? 'opacity:0.5' : ''"
                  >{{ chip.text }}</el-tag>
                </el-space>
                <el-button
                  type="warning"
                  :loading="loadingMonthly"
                  @click="confirmMonthly"
                  style="align-self:flex-start;"
                >
                  <el-icon v-if="!loadingMonthly"><Refresh /></el-icon>
                  {{ loadingMonthly ? 'Đang chạy…' : 'Chạy ngay' }}
                </el-button>
              </el-space>
            </el-card>
          </el-col>

          <!-- Year-End -->
          <el-col :xs="24" :sm="12">
            <el-card
              shadow="never"
              :body-style="{ padding: '18px' }"
              style="background: #fff1f2; border-color: #fecdd3;"
            >
              <el-space direction="vertical" fill :size="12" style="width:100%;">
                <div style="font-size:28px; line-height:1; color: var(--el-color-danger);">
                  <el-icon><Aim /></el-icon>
                </div>
                <el-space direction="vertical" :size="6">
                  <el-text tag="div" style="font-size:14px; font-weight:700;">Reset cuối năm</el-text>
                  <el-text size="small" type="info" style="line-height:1.65;">
                    Hạ 1 bậc tier <strong>toàn bộ</strong> khách hàng VIP và reset điểm về mức tối thiểu của hạng mới.
                  </el-text>
                </el-space>
                <el-space wrap :size="5">
                  <el-tag v-for="chip in yearEndChips" :key="chip" size="small" effect="plain">{{ chip }}</el-tag>
                </el-space>
                <el-button
                  type="danger"
                  :loading="loadingYearEnd"
                  @click="confirmYearEnd"
                  style="align-self:flex-start;"
                >
                  <el-icon v-if="!loadingYearEnd"><Warning /></el-icon>
                  {{ loadingYearEnd ? 'Đang chạy…' : 'Reset cuối năm' }}
                </el-button>
              </el-space>
            </el-card>
          </el-col>
        </el-row>

        <!-- Result -->
        <transition name="fade">
          <el-alert
            v-if="resetResult"
            :type="resetResult.type === 'success' ? 'success' : 'error'"
            :title="resetResult.message"
            show-icon
            :closable="false"
          />
        </transition>
      </el-card>

    </div>
  </div>
</template>

<script setup>
import {
  ArrowRight,
  Refresh,
  Warning,
  User,
  UserFilled,
  Avatar,
  Collection,
  Box,
  Postcard,
  DataAnalysis,
  ShoppingCart,
  Operation,
  Star,
  Money,
  Present,
  Tickets,
  PieChart,
  Setting,
  Calendar,
  Aim,
  List,
  Filter,
  DataLine,
  CreditCard,
  Discount,
  RefreshLeft,
  Lock,
  Memo,
  Clock
} from "@element-plus/icons-vue";
import { onUnmounted } from "vue";
import { ref } from "vue";
import { customersApi } from "../../api/customers.api";
import { confirmModal } from "../../ui/confirm";
import { toast } from "../../ui/toast";

// ── Loyalty Reset ──────────────────────────────────────────────────────────
const loadingMonthly = ref(false);
const loadingYearEnd = ref(false);
const resetResult    = ref(null);
let resultTimer = null;

function showResult(type, message) {
  resetResult.value = { type, message };
  clearTimeout(resultTimer);
  resultTimer = setTimeout(() => (resetResult.value = null), 5000);
}

onUnmounted(() => clearTimeout(resultTimer));

async function runMonthlyReset() {
  loadingMonthly.value = true;
  resetResult.value = null;
  try {
    const res = await customersApi.triggerMonthlyReset();
    showResult("success", res.data?.message || "Kiểm tra inactivity tháng hoàn tất!");
    toast("Monthly reset hoàn tất.", "success");
  } catch (e) {
    const msg = e?.response?.data?.message || "Có lỗi xảy ra khi chạy monthly reset.";
    showResult("error", msg);
    toast(msg, "error");
  } finally {
    loadingMonthly.value = false;
  }
}

async function runYearEndReset() {
  loadingYearEnd.value = true;
  resetResult.value = null;
  try {
    const res = await customersApi.triggerYearEndReset();
    showResult("success", res.data?.message || "Reset cuối năm hoàn tất!");
    toast("Year-end reset hoàn tất.", "success");
  } catch (e) {
    const msg = e?.response?.data?.message || "Có lỗi xảy ra khi chạy year-end reset.";
    showResult("error", msg);
    toast(msg, "error");
  } finally {
    loadingYearEnd.value = false;
  }
}

async function confirmMonthly() {
  const ok = await confirmModal(
    "Tất cả khách hàng VIP không có giao dịch trong 30 ngày qua sẽ bị hạ 1 bậc tier và điểm sẽ được reset về mức tối thiểu của hạng mới.",
    "Xác nhận chạy Monthly Inactivity Check?",
    "Chạy ngay",
    false,
  );
  if (ok) runMonthlyReset();
}

async function confirmYearEnd() {
  const ok = await confirmModal(
    "TOÀN BỘ khách hàng có VIP tier sẽ bị hạ 1 bậc và điểm sẽ được reset. Đây là thao tác không thể hoàn tác!",
    "Xác nhận Reset Cuối Năm?",
    "Reset cuối năm",
    true,
  );
  if (ok) runYearEndReset();
}

function reload() {
  toast("Dashboard đã làm mới.", "info");
}

// ── Static data ────────────────────────────────────────────────────────────
const monthlyChips = [
  { text: "DIAMOND → PLATINUM" },
  { text: "PLATINUM → GOLD" },
  { text: "GOLD → SILVER" },
  { text: "SILVER → BRONZE" },
  { text: "BRONZE → giữ nguyên", muted: true },
];

const yearEndChips = [
  "DIAMOND → PLATINUM · 10.000đ",
  "PLATINUM → GOLD · 5.000đ",
  "GOLD → SILVER · 2.000đ",
  "SILVER → BRONZE · 1.000đ",
  "BRONZE → BRONZE · 1.000đ",
];

const cards = [
  // QUẢN LÝ
  { title: "Tài khoản",           desc: "Quản lý nhân viên & phân quyền",     to: "/system/users",               icon: UserFilled,    color: "#2563eb" },
  { title: "Khách hàng",         desc: "Danh sách & hạng thành viên",       to: "/system/customers",           icon: Avatar,        color: "#16a34a" },
  { title: "Top Khách hàng",      desc: "Khách hàng thân thiết nhất",         to: "/system/Toployalcustomers",   icon: Star,          color: "#ca8a04" },
  { title: "Tổng kết KH",         desc: "Báo cáo tổng hợp khách hàng",       to: "/system/Loyaltysummaryadmin", icon: DataAnalysis,  color: "#d97706" },
  { title: "Danh mục",           desc: "Cấu trúc danh mục sản phẩm",        to: "/system/categories",          icon: Collection,    color: "#4f46e5" },
  { title: "Sản phẩm",           desc: "Quản lý thông tin & kho hàng",      to: "/system/products",            icon: Box,           color: "#7c3aed" },
  { title: "Số Serial",          desc: "Quản lý mã serial sản phẩm",        to: "/system/serials",             icon: Postcard,      color: "#0891b2" },
  { title: "Báo cáo SP",         desc: "Thống kê hiệu quả sản phẩm",        to: "/system/dashboard-products",  icon: PieChart,      color: "#ea580c" },

  // ĐƠN HÀNG
  { title: "Tổng quan Đơn",      desc: "Dashboard thống kê đơn hàng",       to: "/system/order-dashboard",     icon: ShoppingCart,  color: "#0891b2" },
  { title: "Bộ lọc Đơn hàng",     desc: "Tra cứu đơn hàng nâng cao",         to: "/system/orders/filter",       icon: Filter,        color: "#6366f1" },

  // BÁO CÁO & TÀI CHÍNH
  { title: "Báo cáo Doanh thu",   desc: "Thống kê tài chính & bán hàng",      to: "/system/reports",             icon: DataLine,      color: "#16a34a" },
  { title: "Quản lý Giá",        desc: "Thiết lập & lịch sử giá",           to: "/system/pricing",             icon: Money,         color: "#10b981" },
  { title: "Thanh toán",         desc: "Lịch sử giao dịch chi tiết",        to: "/system/payments",            icon: CreditCard,    color: "#2563eb" },
  { title: "Khuyến mãi",         desc: "Mã giảm giá & chiến dịch",         to: "/system/promotions",          icon: Discount,      color: "#db2777" },
  { title: "Trả hàng",           desc: "Quản lý yêu cầu hoàn trả",          to: "/system/return-manager",      icon: RefreshLeft,   color: "#ef4444" },

  // BẢO MẬT
  { title: "Tổng quan Bảo mật",   desc: "Dashboard an toàn hệ thống",        to: "/system/audit-dashboard",     icon: Lock,          color: "#4b5563" },
  { title: "Nhật ký Hệ thống",    desc: "Xem logs & lịch sử đăng nhập",      to: "/system/audit-logs",          icon: Memo,          color: "#6b7280" },
];
</script>

<style scoped>
.module-card:hover {
  border-color: #bfdbfe !important;
  background: #f8faff !important;
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(37,99,235,0.08), 0 2px 6px rgba(0,0,0,0.04) !important;
}
.module-card:hover :deep(.el-icon.el-icon--right),
.module-card:hover :deep(svg) {
  color: #2563eb;
}

.fade-enter-active { transition: all 0.3s cubic-bezier(0.34,1.3,0.64,1); }
.fade-leave-active { transition: all 0.2s ease; }
.fade-enter-from   { opacity: 0; transform: translateY(-6px); }
.fade-leave-to     { opacity: 0; }

@media (max-width: 900px) {
  div[style*="padding: 32px 40px"] { padding: 20px 16px 40px !important; }
}
</style>