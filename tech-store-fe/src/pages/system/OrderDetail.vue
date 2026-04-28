<!-- \src\pages\system\OrderDetail.vue -->
<template>
  <div class="order-page">
    <el-card shadow="never">
      <!-- Header -->
      <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:20px; margin-bottom:22px; flex-wrap:wrap;">
        <div>
          <div style="font-size:11px; font-weight:700; letter-spacing:0.08em; text-transform:uppercase; color:var(--el-text-color-secondary); margin-bottom:6px; display:flex; align-items:center; gap:5px;">
            <el-icon><Tickets /></el-icon> Đơn hàng
          </div>
          <div style="font-size:28px; font-weight:800; letter-spacing:-0.03em; margin-bottom:4px;">{{ detail?.orderNumber || `#${orderId}` }}</div>
          <div style="font-size:13px; color:var(--el-text-color-secondary);">
            <el-space wrap>
              <el-tag :type="statusType" size="small">
                {{ formatOrderStatus(detail?.status) }}
              </el-tag>
              <el-tag :type="paymentStatusType" size="small">
                {{ formatPaymentStatus(detail?.paymentStatus) }}
              </el-tag>
              <el-tag v-if="isReturned(detail?.status)" type="warning" size="small">
                Hoàn trả
              </el-tag>
            </el-space>
          </div>
        </div>
        <div style="display:flex; align-items:center; gap:8px; flex-shrink:0; padding-top:4px; flex-wrap:wrap;">
          <el-button plain :loading="loading" @click="reload">
            <el-icon v-if="!loading"><Refresh /></el-icon>
            <span v-if="!loading">Reload</span>
          </el-button>

          <!-- Admin: Xác nhận đã giao hàng -->
          <el-button
            v-if="detail?.status === 'SHIPPING'"
            type="success"
            plain
            :loading="markDeliveredLoading"
            @click="handleMarkDelivered"
          >
            <el-icon><CircleCheck /></el-icon> Đánh dấu Đã Giao Hàng
          </el-button>

          <!-- Admin: Xác nhận thu tiền COD -->
          <el-button
            v-if="detail?.paymentMethod === 'CASH' && detail?.paymentStatus !== 'PAID' && ['SHIPPING','DELIVERED'].includes(detail?.status)"
            type="warning"
            plain
            :loading="confirmCodLoading"
            @click="handleConfirmCod"
          >
            <el-icon><Money /></el-icon> Xác nhận Thu Tiền Mặt
          </el-button>

          <!-- ✅ FIX Issue 1: Admin xác nhận chuyển khoản -->
          <el-button
            v-if="detail?.paymentMethod === 'TRANSFER' && detail?.paymentStatus !== 'PAID'"
            type="primary"
            plain
            :loading="confirmTransferLoading"
            @click="handleConfirmTransfer"
          >
            <el-icon><Wallet /></el-icon> Xác nhận Chuyển Khoản
          </el-button>
        </div>
      </div>

      <el-divider />

      <!-- Skeleton -->
      <el-skeleton v-if="loading" :rows="6" animated />

      <el-row v-else-if="detail" :gutter="24" class="section-row">
        <!-- Customer Info -->
        <el-col :xs="24" :md="12">
          <el-card shadow="never">
            <template #header>
              <el-space>
                <el-icon><User /></el-icon>
                <strong>Thông tin khách hàng</strong>
              </el-space>
            </template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="Tên">{{ detail.customerName }}</el-descriptions-item>
              <el-descriptions-item label="ID">
                <el-tag type="info" size="small">{{ detail.customerId }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="Email">{{ detail.customerEmail }}</el-descriptions-item>
              <el-descriptions-item label="Phone">{{ detail.customerPhone }}</el-descriptions-item>
              <el-descriptions-item label="Address">{{ detail.customerAddress }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>

        <!-- Payment & Delivery Info -->
        <el-col :xs="24" :md="12">
          <el-card shadow="never">
            <template #header>
              <el-space>
                <el-icon><Van /></el-icon>
                <strong>Thanh toán &amp; Giao hàng</strong>
              </el-space>
            </template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="Phương thức">{{ detail.paymentMethod }}</el-descriptions-item>
              <el-descriptions-item label="Kênh">{{ detail.channel }}</el-descriptions-item>
              <el-descriptions-item label="Ghi chú" :label-style="{ verticalAlign: 'top', paddingTop: '10px' }">
                <el-space v-if="detail.notes" direction="vertical" alignment="flex-start" :size="6" style="width: 100%">
                  <el-tag
                    v-for="(note, i) in categorizedNotes"
                    :key="i"
                    size="default"
                    :type="note.type === 'note-vip' ? '' : note.type === 'note-coupon' ? 'success' : note.type === 'note-ship' ? 'warning' : 'info'"
                  >{{ note.text }}</el-tag>
                </el-space>
                <span v-else>—</span>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>

        <!-- Discount Breakdown -->
        <el-col v-if="hasDiscount" :span="24">
          <el-card shadow="never">
            <template #header>
              <el-row justify="space-between" align="middle">
                <el-col :span="20">
                  <el-space>
                    <el-icon size="20"><Present /></el-icon>
                    <div>
                      <div><strong>Ưu đãi áp dụng</strong></div>
                      <div>Tổng tiết kiệm: <strong>{{ formatMoney(detail.discountTotal) }}</strong></div>
                    </div>
                  </el-space>
                </el-col>
                <el-col :span="4" style="text-align: right">
                  <el-tag type="danger" size="large">-{{ formatMoney(detail.discountTotal) }}</el-tag>
                </el-col>
              </el-row>
            </template>

            <div class="discount-grid">
              <!-- VIP % Discount -->
              <el-card
                v-if="detail.vipDiscountRate && detail.vipDiscountRate > 0 && vipPercentAmount > 0"
                shadow="never"
              >
                <el-space alignment="flex-start">
                  <el-icon size="22"><Star /></el-icon>
                  <div>
                    <el-space wrap>
                      <span>VIP {{ vipTierName }}</span>
                      <el-tag size="small">{{ detail.vipDiscountRate }}%</el-tag>
                    </el-space>
                    <div class="pill-amount">-{{ formatMoney(vipPercentAmount) }}</div>
                    <div class="pill-note">Giảm theo % hạng VIP</div>
                  </div>
                </el-space>
              </el-card>

              <!-- VIP Bonus -->
              <el-card v-if="parsedVipBonus" shadow="never">
                <el-space alignment="flex-start">
                  <el-icon size="22"><Trophy /></el-icon>
                  <div>
                    <el-space wrap>
                      <span>VIP {{ parsedVipBonus.tier }}</span>
                      <el-tag size="small">Đơn lớn</el-tag>
                    </el-space>
                    <div class="pill-amount">-{{ formatMoney(parsedVipBonus.amount) }}</div>
                    <div class="pill-note">Đơn từ {{ formatMoney(parsedVipBonus.threshold) }}</div>
                  </div>
                </el-space>
              </el-card>

              <!-- Spin Wheel -->
              <el-card
                v-if="detail.spinDiscount && detail.spinDiscount > 0"
                shadow="never"
              >
                <el-space alignment="flex-start">
                  <el-icon size="22"><Opportunity /></el-icon>
                  <div>
                    <el-space wrap>
                      <span>Vòng quay</span>
                      <el-tag size="small" type="warning">1 lần dùng</el-tag>
                    </el-space>
                    <div>{{ detail.spinDiscountRate }}%</div>
                    <div class="pill-amount">-{{ formatMoney(detail.spinDiscount) }}</div>
                  </div>
                </el-space>
              </el-card>

              <!-- Coupon -->
              <el-card v-if="effectiveCoupon" shadow="never">
                <el-space alignment="flex-start">
                  <el-icon size="22"><Ticket /></el-icon>
                  <div>
                    <el-space wrap>
                      <span>Mã giảm giá</span>
                      <el-tag size="small" type="success">{{ effectiveCoupon.code || "Đã áp dụng" }}</el-tag>
                    </el-space>
                    <div class="pill-amount">-{{ formatMoney(effectiveCoupon.amount) }}</div>
                    <div class="pill-note">Ưu đãi từ mã khuyến mãi</div>
                  </div>
                </el-space>
              </el-card>
            </div>
          </el-card>
        </el-col>

        <!-- Products Table -->
        <el-col :span="24">
          <el-card shadow="never">
            <template #header>
              <el-row justify="space-between" align="middle">
                <el-space>
                  <el-icon><Box /></el-icon>
                  <span>Chi tiết sản phẩm</span>
                </el-space>
                <el-tag>{{ detail.items?.length || 0 }} sản phẩm</el-tag>
              </el-row>
            </template>
            <el-table :data="detail.items">
              <el-table-column label="Sản phẩm" min-width="220">
                <template #default="{ row }">
                  <div>
                    <div><strong>{{ row.productName }}</strong></div>
                    <div>{{ row.variantName }}</div>
                    <div>SKU: {{ row.sku }}</div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="Số lượng" width="100" align="center">
                <template #default="{ row }">
                  <el-tag>{{ row.quantity }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="Đơn giá" width="160" align="right">
                <template #default="{ row }">
                  <strong>{{ formatMoney(row.price) }}</strong>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>

        <!-- Totals -->
        <el-col :span="24">
          <el-card shadow="never">
            <template #header>
              <el-space>
                <el-icon><Wallet /></el-icon>
                <strong>Tổng thanh toán</strong>
              </el-space>
            </template>

            <el-descriptions :column="1" border>
              <el-descriptions-item label="Tạm tính">{{ formatMoney(detail.subtotal) }}</el-descriptions-item>

              <el-descriptions-item
                v-if="detail.vipDiscountRate && detail.vipDiscountRate > 0 && vipPercentAmount > 0"
                label="Giảm VIP"
              >
                <el-text type="success">-{{ formatMoney(vipPercentAmount) }} ({{ detail.vipDiscountRate }}%)</el-text>
              </el-descriptions-item>

              <el-descriptions-item v-if="parsedVipBonus" label="Ưu đãi VIP">
                <el-text type="primary">
                  -{{ formatMoney(parsedVipBonus.amount) }}
                  (từ {{ formatMoney(parsedVipBonus.threshold) }})
                </el-text>
              </el-descriptions-item>

              <el-descriptions-item
                v-if="detail.spinDiscount && detail.spinDiscount > 0"
                label="Vòng quay"
              >
                <el-text type="warning">-{{ formatMoney(detail.spinDiscount) }} ({{ detail.spinDiscountRate }}%)</el-text>
              </el-descriptions-item>

              <el-descriptions-item v-if="effectiveCoupon" label="Mã giảm giá">
                <el-text type="success">
                  <strong>{{ effectiveCoupon.code }}</strong>&nbsp;-{{ formatMoney(effectiveCoupon.amount) }}
                </el-text>
              </el-descriptions-item>

              <!-- ✅ FIX Issue 2: Ẩn dòng phí giao hàng khi = 0 -->
              <el-descriptions-item v-if="detail.shippingFee > 0" label="Phí giao hàng">
                {{ formatMoney(detail.shippingFee) }}
              </el-descriptions-item>

              <el-descriptions-item label="Tổng cộng">
                <el-text type="primary"><strong class="total-amount">{{ formatMoney(detail.totalAmount) }}</strong></el-text>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { ordersApi } from "../../api/orders.api";
import { customersApi } from "../../api/customers.api";
import { paymentsApi } from "../../api/payments";
import { toast } from "../../ui/toast";
import { ElMessageBox } from "element-plus";
import {
  Refresh, User, Van, Present, Star, Trophy, Opportunity,
  Ticket, Box, Wallet, Money,
  CircleCheck, CircleClose, Timer, InfoFilled, RefreshLeft,
} from "@element-plus/icons-vue";

// NOTE: "Tickets" được dùng inline trong template qua el-icon — đã có global register
// nếu chưa, thêm vào import: import { ..., Tickets } from "@element-plus/icons-vue"

const route = useRoute();

const loading = ref(false);
const detail = ref(null);
const vipTierName = ref("");
const orderId = computed(() => route.params.orderId);

const markDeliveredLoading = ref(false);
const confirmCodLoading = ref(false);
const confirmTransferLoading = ref(false); // ✅ FIX Issue 1

const statusType = computed(() => {
  const s = detail.value?.status;
  if (s === "DELIVERED") return "success";
  if (s === "SHIPPING") return "warning";
  if (s === "CANCELLED") return "danger";
  if (s === "PAID") return "success";
  return "info";
});

const paymentStatusType = computed(() => {
  const ps = detail.value?.paymentStatus;
  if (ps === "PAID") return "success";
  if (ps === "PARTIAL") return "warning";
  return "info";
});

function formatOrderStatus(s) { return { DELIVERED: "Đã giao hàng", SHIPPING: "Đang vận chuyển", CANCELLED: "Đã hủy", PENDING: "Chờ xử lý", PAID: "Đã thanh toán", RETURNED: "Trả hàng", PARTIALLY_RETURNED: "Trả hàng một phần" }[s] || s; }
function formatPaymentStatus(s) { return { PAID: "Đã thanh toán", PARTIAL: "Thanh toán một phần", PENDING: "Chờ thanh toán", UNPAID: "Chưa thanh toán" }[s] || s; }

const vipPercentAmount = computed(() => {
  const total = detail.value?.vipDiscount || 0;
  const bonus = parsedVipBonus.value?.amount || 0;
  return Math.max(0, total - bonus);
});

const hasDiscount = computed(() => {
  return (
    vipPercentAmount.value > 0 ||
    !!parsedVipBonus.value ||
    (detail.value?.spinDiscount && detail.value.spinDiscount > 0) ||
    !!effectiveCoupon.value
  );
});

const parsedNotes = computed(() => {
  if (!detail.value?.notes) return [];
  return detail.value.notes
    .split("|")
    .map((n) => n.trim())
    .filter(Boolean);
});

const categorizedNotes = computed(() => {
  return parsedNotes.value.map((note) => {
    if (/^vip\s+/i.test(note)) {
      return { text: note, type: "note-vip", icon: "⭐" };
    }
    if (/^[Mm]ã[:\s]/.test(note)) {
      return { text: note, type: "note-coupon", icon: "🎟️" };
    }
    if (note.toLowerCase().includes("ship") || note.toLowerCase().includes("giao")) {
      return { text: note, type: "note-ship", icon: "🚚" };
    }
    return { text: note, type: "note-default", icon: "📌" };
  });
});

const parsedVipBonus = computed(() => {
  if (!detail.value?.notes) return null;
  const segments = detail.value.notes.split("|").map((s) => s.trim());
  for (const seg of segments) {
    const match = seg.match(
      /^VIP\s+([A-Za-zÀ-ỹ]+)[:\s]+([-\d,\.]+)\s*\(đơn từ\s*([\d,\.]+)\)/i,
    );
    if (match) {
      const tier = match[1].trim();
      const amount = Math.abs(parseInt(match[2].replace(/[,\.]/g, ""), 10));
      const threshold = parseInt(match[3].replace(/[,\.]/g, ""), 10);
      if (!isNaN(amount) && amount > 0) return { tier, amount, threshold };
    }
  }
  return null;
});

const parsedCoupon = computed(() => {
  if (!detail.value?.notes) return null;
  if (detail.value.couponDiscount && detail.value.couponDiscount > 0) return null;
  const segments = detail.value.notes.split("|").map((s) => s.trim());
  for (const seg of segments) {
    const match = seg.match(/^[Mm]ã[:\s]+([A-Za-z0-9_\-]+)[:\s]+([-\d,\.]+)/);
    if (match) {
      const code = match[1].trim();
      const rawNum = match[2].replace(/[,\.]/g, "").replace(/[^\d\-]/g, "");
      const amount = Math.abs(parseInt(rawNum, 10));
      if (!isNaN(amount) && amount > 0) return { code, amount };
    }
  }
  return null;
});

const effectiveCoupon = computed(() => {
  if (detail.value?.couponDiscount && detail.value.couponDiscount > 0) {
    return { code: detail.value.couponCode || "", amount: detail.value.couponDiscount, rate: detail.value.couponDiscountRate || null };
  }
  if (parsedCoupon.value) return { code: parsedCoupon.value.code, amount: parsedCoupon.value.amount, rate: null };
  return null;
});

function formatMoney(val) {
  if (!val) return "0 ₫";
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(val);
}

async function reload() {
  loading.value = true;
  try {
    const res = await ordersApi.getById(orderId.value);
    detail.value = res?.data?.data || res?.data;
    if (detail.value?.customerId) {
      try {
        const customerRes = await customersApi.getById(detail.value.customerId);
        const customer = customerRes?.data?.data || customerRes?.data;
        vipTierName.value = customer?.vipTierDisplay || "";
      } catch (e) {
        console.error("Error loading customer:", e);
      }
    }
  } catch (e) {
    toast("Không thể tải chi tiết đơn hàng", "error");
  } finally {
    loading.value = false;
  }
}

// Admin: Đánh dấu đã giao hàng
async function handleMarkDelivered() {
  try {
    await ElMessageBox.confirm(
      `Xác nhận đơn hàng <strong>${detail.value?.orderNumber}</strong> đã được giao thành công?<br/><br/>
      <ul style="margin: 0; padding-left: 16px; font-size: 13px; color: var(--el-text-color-secondary);">
        <li>Đơn hàng sẽ chuyển sang trạng thái <strong>DELIVERED</strong></li>
        <li>Khách hàng sẽ nhận được thông báo</li>
        <li style="color: var(--el-color-danger);">Hành động này không thể hoàn tác</li>
      </ul>`,
      '✅ Xác nhận Giao Hàng Thành Công',
      { confirmButtonText: 'Đánh dấu đã giao', cancelButtonText: 'Hủy', type: 'success', dangerouslyUseHTMLString: true }
    );
  } catch { return; }
  markDeliveredLoading.value = true;
  try {
    await ordersApi.markAsDelivered(orderId.value);
    toast('✅ Đã xác nhận giao hàng thành công', 'success');
    await reload();
  } catch {
    toast('Lỗi khi cập nhật trạng thái', 'error');
  } finally {
    markDeliveredLoading.value = false;
  }
}

// Admin: Xác nhận thu tiền mặt COD
async function handleConfirmCod() {
  const amount = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(detail.value?.totalAmount || 0);
  try {
    await ElMessageBox.confirm(
      `Xác nhận shipper đã thu tiền mặt cho đơn hàng <strong>${detail.value?.orderNumber}</strong>?<br/><br/>
      <div style="padding: 12px; background: var(--el-fill-color-light); border-radius: 6px; margin-bottom: 8px;">
        <span style="font-size: 22px; font-weight: 800; color: var(--el-color-warning-dark-2);">${amount}</span>
      </div>
      <ul style="margin: 0; padding-left: 16px; font-size: 13px; color: var(--el-text-color-secondary);">
        <li>Trạng thái thanh toán chuyển sang <strong>PAID</strong></li>
        <li>Điểm loyalty sẽ được cộng cho khách hàng</li>
        <li style="color: var(--el-color-danger);">Hành động này không thể hoàn tác</li>
      </ul>`,
      '💵 Xác nhận Thu Tiền Mặt COD',
      { confirmButtonText: 'Xác nhận đã thu tiền', cancelButtonText: 'Hủy', type: 'warning', dangerouslyUseHTMLString: true }
    );
  } catch { return; }
  confirmCodLoading.value = true;
  try {
    await paymentsApi.create({ orderId: Number(orderId.value), method: 'CASH', transactionRef: `COD-${Date.now()}` });
    toast('✅ Đã xác nhận thu tiền mặt thành công', 'success');
    await reload();
  } catch {
    toast('Lỗi khi xác nhận thanh toán', 'error');
  } finally {
    confirmCodLoading.value = false;
  }
}

// ✅ FIX Issue 1: Admin xác nhận chuyển khoản
async function handleConfirmTransfer() {
  const amount = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(detail.value?.totalAmount || 0);
  try {
    await ElMessageBox.confirm(
      `Xác nhận đã nhận tiền chuyển khoản cho đơn hàng <strong>${detail.value?.orderNumber}</strong>?<br/><br/>
      <div style="padding: 12px; background: var(--el-fill-color-light); border-radius: 6px; margin-bottom: 8px;">
        <span style="font-size: 22px; font-weight: 800; color: var(--el-color-primary);">${amount}</span>
      </div>
      <ul style="margin: 0; padding-left: 16px; font-size: 13px; color: var(--el-text-color-secondary);">
        <li>Trạng thái thanh toán chuyển sang <strong>PAID</strong></li>
        <li>Kho sẽ được xuất tự động</li>
        <li>Điểm loyalty sẽ được cộng cho khách hàng</li>
        <li style="color: var(--el-color-danger);">Hành động này không thể hoàn tác</li>
      </ul>`,
      '🏦 Xác nhận Thanh toán Chuyển khoản',
      { confirmButtonText: 'Xác nhận đã nhận tiền', cancelButtonText: 'Hủy', type: 'primary', dangerouslyUseHTMLString: true }
    );
  } catch { return; }
  confirmTransferLoading.value = true;
  try {
    await paymentsApi.create({
      orderId: Number(orderId.value),
      method: 'TRANSFER',
      transactionRef: `TRANSFER-${Date.now()}`,
    });
    toast('✅ Đã xác nhận thanh toán chuyển khoản thành công', 'success');
    await reload();
  } catch {
    toast('Lỗi khi xác nhận thanh toán', 'error');
  } finally {
    confirmTransferLoading.value = false;
  }
}

function isReturned(status) {
  return ["PARTIALLY_RETURNED", "RETURNED"].includes(status);
}

onMounted(() => reload());
</script>

<style scoped>
.order-page :deep(.el-tag) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: normal;
  vertical-align: middle;
}
.order-page :deep(.el-tag .el-icon) {
  display: none;
}
.order-page {
  padding: 0;
  min-height: 100vh;
  font-size: 16px;
}

.section-row > .el-col {
  margin-bottom: 16px;
}

.discount-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

.pill-amount {
  font-size: 18px;
  font-weight: 800;
  margin-top: 4px;
}

.pill-note {
  font-size: 13px;
  margin-top: 2px;
}

.total-amount {
  font-size: 20px;
}
</style>