<!-- \src\pages\customer\OrderDetail.vue -->
<template>
  <div style="max-width: 1280px; margin: 0 auto; padding: 32px 24px 60px;">
    <div class="order-layout">

      <!-- LEFT COLUMN -->
      <div class="order-left">
        <el-card shadow="never">
          <!-- Header -->
          <el-row align="middle" justify="space-between" style="margin-bottom: 20px; flex-wrap: wrap; gap: 12px;">
            <el-space direction="vertical" :size="6">
              <el-text size="small" type="primary" style="text-transform: uppercase; letter-spacing: 0.08em; font-weight: 700;">Order</el-text>
              <el-text tag="div" style="font-size: 22px; font-weight: 700; letter-spacing: -0.02em;">
                {{ detail?.orderNumber || `#${orderId}` }}
              </el-text>
              <el-space :size="8" style="flex-wrap: wrap;">
                <el-tag :type="statusTagType(detail?.status)" effect="plain" round>{{ detail?.status }}</el-tag>
                <el-tag v-if="isReturned(detail?.status)" type="warning" effect="plain" round>Returned</el-tag>
              </el-space>
            </el-space>

            <el-space :size="8" style="flex-wrap: wrap;">
              <el-button plain :loading="loading" @click="reload">
                <el-icon><Refresh /></el-icon> Reload
              </el-button>

              <!-- Nút Thanh toán cho phương thức KHÔNG phải TRANSFER và KHÔNG phải CASH -->
              <el-button
                v-if="detail?.paymentStatus === 'UNPAID' && detail?.status === 'PENDING'
                      && detail?.paymentMethod !== 'CASH' && detail?.paymentMethod !== 'TRANSFER'"
                type="primary" plain @click="openPaymentDialog"
              >
                <el-icon><CreditCard /></el-icon> Thanh toán
              </el-button>

              <!-- TRANSFER → redirect sang cổng VNPay -->
              <el-button
                v-if="detail?.paymentStatus === 'UNPAID' && detail?.status === 'PENDING'
                      && detail?.paymentMethod === 'TRANSFER'"
                type="primary" plain :loading="vnpayLoading" @click="openVnPayPayment"
              >
                <el-icon><CreditCard /></el-icon> Thanh toán qua VNPay
              </el-button>

              <el-button
                v-if="detail?.status === 'PENDING' || detail?.status === 'PAID'"
                type="danger" plain @click="showCancelDialog = true"
              >
                <el-icon><Close /></el-icon> Hủy đơn
              </el-button>
            </el-space>
          </el-row>

          <!-- Timeline -->
          <el-steps
            v-if="detail"
            :active="timelineActiveIndex"
            :finish-status="timelineFinishStatus"
            style="margin-bottom: 28px;"
          >
            <el-step
              v-for="step in timelineSteps"
              :key="step.key"
              :title="step.label"
            />
          </el-steps>

          <!-- Skeleton -->
          <el-skeleton v-if="loading" :rows="6" animated />

          <template v-else-if="detail">
            <!-- Info grid -->
            <el-row :gutter="16" style="margin-bottom: 20px;">
              <el-col :xs="24" :sm="12">
                <el-card shadow="never" style="background: var(--el-fill-color-lighter); height: 100%;">
                  <template #header>
                    <el-space :size="8">
                      <el-icon><User /></el-icon>
                      <el-text tag="b" size="small">Thông tin khách hàng</el-text>
                    </el-space>
                  </template>
                  <el-descriptions :column="1" size="small" border>
                    <el-descriptions-item label="Tên">{{ detail.customerName }}</el-descriptions-item>
                    <el-descriptions-item label="Email">{{ detail.customerEmail }}</el-descriptions-item>
                    <el-descriptions-item label="Phone">{{ detail.customerPhone }}</el-descriptions-item>
                    <el-descriptions-item label="Address">{{ detail.customerAddress }}</el-descriptions-item>
                  </el-descriptions>
                </el-card>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-card shadow="never" style="background: var(--el-fill-color-lighter); height: 100%;">
                  <template #header>
                    <el-space :size="8">
                      <el-icon><CreditCard /></el-icon>
                      <el-text tag="b" size="small">Thanh toán &amp; Giao hàng</el-text>
                    </el-space>
                  </template>
                  <el-descriptions :column="1" size="small" border>
                    <el-descriptions-item label="Phương thức">
                      <el-tag size="small" effect="plain">{{ detail.paymentMethod }}</el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="Kênh">{{ detail.channel }}</el-descriptions-item>
                    <el-descriptions-item label="Giao hàng">
                      <el-tag :type="isHomeDelivery ? 'primary' : 'success'" size="small" effect="plain">
                        {{ isHomeDelivery ? '🚚 Giao tại nhà' : '🏬 Nhận tại cửa hàng' }}
                      </el-tag>
                    </el-descriptions-item>
                  </el-descriptions>
                  <!-- Notes -->
                  <el-space v-if="detail.notes" direction="vertical" fill :size="6" style="margin-top: 12px; width: 100%;">
                    <el-text size="small" type="info" style="font-weight: 600; text-transform: uppercase; letter-spacing: 0.06em;">Ghi chú</el-text>
                    <el-tag
                      v-for="(line, i) in parseNotes(detail.notes)"
                      :key="i"
                      :type="{ vip:'warning', discount:'primary', delivery:'success', payment:'info', default:'' }[line.type]"
                      effect="plain"
                      style="height: auto; padding: 4px 8px; white-space: normal; text-align: left; width: 100%;"
                    >
                      {{ line.icon }} {{ line.text }}
                    </el-tag>
                  </el-space>
                </el-card>
              </el-col>
            </el-row>

            <!-- Items table -->
            <el-space direction="vertical" :size="6" style="width: 100%; margin-bottom: 4px;">
              <el-row align="middle" justify="space-between">
                <el-text tag="b">Chi tiết sản phẩm</el-text>
                <el-tag type="info" effect="plain" size="small">{{ detail.items?.length }} sản phẩm</el-tag>
              </el-row>
            </el-space>
            <el-table :data="detail.items" border stripe size="small">
              <el-table-column type="index" width="48" align="center" />
              <el-table-column label="Sản phẩm" min-width="200">
                <template #default="{ row }">
                  <el-text tag="div" style="font-weight: 600;">{{ row.productName }}</el-text>
                  <el-text size="small" type="info">{{ row.variantName }}</el-text>
                  <el-text size="small" type="info" style="display: block;">{{ row.sku }}</el-text>
                </template>
              </el-table-column>
              <el-table-column label="SL" width="80" align="center">
                <template #default="{ row }">
                  <el-tag size="small">× {{ row.quantity }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="Đơn giá" width="140" align="right">
                <template #default="{ row }">
                  <el-text type="primary" style="font-weight: 600;">{{ formatMoney(row.price) }}</el-text>
                </template>
              </el-table-column>
              <el-table-column label="Serial" min-width="160">
                <template #default="{ row }">
                  <template v-if="row.serialNumbers && row.serialNumbers.length">
                    <el-tag
                      v-for="sn in row.serialNumbers"
                      :key="sn"
                      size="small"
                      type="info"
                      effect="plain"
                      style="margin: 2px 2px; font-family: monospace; letter-spacing: 0.04em;"
                    >
                      {{ sn }}
                    </el-tag>
                  </template>
                  <el-text v-else size="small" type="info">—</el-text>
                </template>
              </el-table-column>
            </el-table>
          </template>
        </el-card>
      </div>

      <!-- RIGHT COLUMN -->
      <div class="order-right" v-if="detail">

        <!-- Shipping Info Banner -->
        <el-alert
          v-if="detail?.status === 'SHIPPING'"
          type="success"
          :closable="false"
          show-icon
          style="margin-bottom: 16px;"
        >
          <template #title>
            <span style="font-weight: 700;">🚚 Đơn hàng đang được giao</span>
          </template>
          <template #default>
            <el-space direction="vertical" :size="4">
              <el-text size="small">Cửa hàng sẽ xác nhận giao hàng thành công sau khi kiểm tra thông tin với đơn vị vận chuyển.</el-text>
              <el-text size="small" type="info">Nếu không có phản hồi, đơn hàng sẽ được tự động xác nhận sau <strong>3 ngày</strong>.</el-text>
              <el-text size="small" type="danger">Nếu có vấn đề với đơn hàng, vui lòng liên hệ hỗ trợ để được giám sát.</el-text>
            </el-space>
          </template>
        </el-alert>

        <!-- COD Banner -->
        <el-alert
          v-if="detail?.paymentMethod === 'CASH' && detail?.paymentStatus !== 'PAID'"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 16px;"
        >
          <template #title>
            <span style="font-weight: 700;">💵 Thanh toán tiền mặt (COD)</span>
          </template>
          <template #default>
            <el-space direction="vertical" :size="4">
              <el-text size="small">Shipper sẽ thu tiền mặt trực tiếp khi giao hàng đến tay bạn.</el-text>
              <el-text size="small" type="info">Bạn <strong>không cần</strong> thao tác thanh toán online. Hệ thống sẽ tự cập nhật sau khi kế toán đối soát.</el-text>
            </el-space>
          </template>
        </el-alert>

        <!-- ✅ FIX Issue 1: Banner chờ xác nhận chuyển khoản -->
        <el-alert
          v-if="detail?.paymentMethod === 'TRANSFER' && detail?.paymentStatus !== 'PAID'"
          type="warning"
          :closable="false"
          show-icon
          style="margin-bottom: 16px;"
        >
          <template #title>
            <span style="font-weight: 700;">🏦 Chờ xác nhận chuyển khoản</span>
          </template>
          <template #default>
            <el-space direction="vertical" :size="4">
              <el-text size="small">Vui lòng chuyển khoản theo thông tin QR bên dưới.</el-text>
              <el-text size="small" type="info">Sau khi chuyển tiền, admin sẽ xác nhận thanh toán. Đơn hàng sẽ được cập nhật tự động.</el-text>
            </el-space>
          </template>
        </el-alert>

        <!-- Totals -->
        <el-card shadow="never" style="margin-bottom: 16px;">
          <template #header><el-text tag="b">Tóm tắt đơn hàng</el-text></template>
          <el-space direction="vertical" fill :size="10" style="width: 100%;">
            <el-row justify="space-between">
              <el-text size="small">Tạm tính</el-text>
              <el-text size="small">{{ formatMoney(detail.subtotal) }}</el-text>
            </el-row>
            <el-row v-if="detail.vipDiscount > 0" justify="space-between">
              <el-text size="small">👑 Giảm VIP</el-text>
              <el-text size="small" type="danger">−{{ formatMoney(detail.vipDiscount) }}</el-text>
            </el-row>
            <el-row v-if="detail.discountTotal - (detail.vipDiscount || 0) > 0" justify="space-between">
              <el-text size="small">🏷️ Mã giảm giá</el-text>
              <el-text size="small" type="danger">−{{ formatMoney(detail.discountTotal - (detail.vipDiscount || 0)) }}</el-text>
            </el-row>
            <!-- ✅ FIX Issue 2: Luôn hiển thị "Miễn phí" — bỏ hardcode 15000 -->
            <el-row justify="space-between" align="middle">
              <el-text size="small">{{ isHomeDelivery ? '🚚 Phí ship' : '🏬 Nhận tại cửa hàng' }}</el-text>
              <el-tag size="small" type="success" effect="plain">Miễn phí</el-tag>
            </el-row>
          </el-space>
          <el-divider style="margin: 12px 0;" />
          <el-row justify="space-between" align="middle">
            <el-text style="font-weight: 600;">Tổng cộng</el-text>
            <el-text type="primary" style="font-size: 20px; font-weight: 700;">{{ formatMoney(detail.totalAmount) }}</el-text>
          </el-row>
        </el-card>

        <!-- Timestamps -->
        <el-card shadow="never">
          <template #header>
            <el-space :size="8">
              <el-icon><Clock /></el-icon>
              <el-text tag="b">Thời gian đơn hàng</el-text>
            </el-space>
          </template>
          <el-timeline>
            <el-timeline-item type="primary" :timestamp="formatDateTime(detail.createdAt)" placement="top">
              Tạo đơn
            </el-timeline-item>
            <el-timeline-item v-if="detail.paidAt" type="success" :timestamp="formatDateTime(detail.paidAt)" placement="top">
              Thanh toán
            </el-timeline-item>
            <el-timeline-item
              v-if="!detail.cancelledAt"
              :type="detail.deliveredAt ? 'success' : ''"
              :timestamp="detail.deliveredAt ? formatDateTime(detail.deliveredAt) : 'Chưa giao'"
              placement="top"
            >
              Đã giao hàng
            </el-timeline-item>
            <el-timeline-item v-if="detail.cancelledAt" type="danger" :timestamp="formatDateTime(detail.cancelledAt)" placement="top">
              Đã hủy
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </div>
    </div>

    <!-- ── PAYMENT DIALOG ── -->
    <el-dialog v-model="showPaymentDialog" title="💳 Thanh toán đơn hàng" width="500px" align-center>

      <!-- Các phương thức non-TRANSFER -->
      <div v-if="spinStatus.loading" style="text-align: center; padding: 20px;">
        <el-icon class="is-loading" :size="28"><Loading /></el-icon>
        <el-text type="info" style="display: block; margin-top: 8px;">Đang kiểm tra ưu đãi vòng quay...</el-text>
      </div>

      <el-card
        v-if="!spinStatus.loading && spinStatus.hasActiveBonus"
        shadow="never"
        style="margin-bottom: 14px; background: var(--el-color-warning-light-9); border-color: var(--el-color-warning-light-5);"
      >
        <el-row align="middle" :gutter="12">
          <el-col flex="none"><span style="font-size: 28px;">🎡</span></el-col>
          <el-col flex="1">
            <el-text tag="b" style="display: block;">Ưu đãi vòng quay đã áp dụng!</el-text>
            <el-text size="small" type="warning">
              Giảm thêm <strong>{{ spinStatus.bonusRate }}%</strong>
              <span v-if="spinStatus.bonusExpiresAt"> · Hết hạn {{ formatExpiry(spinStatus.bonusExpiresAt) }}</span>
            </el-text>
          </el-col>
          <el-col flex="none">
            <el-tag type="warning" effect="dark">−{{ spinStatus.bonusRate }}%</el-tag>
          </el-col>
        </el-row>
      </el-card>

      <el-alert type="success" :closable="false" show-icon style="margin-bottom: 14px;">
        <template #default>
          <div>Sau khi thanh toán thành công:</div>
          <el-space direction="vertical" :size="4" style="margin-top: 6px;">
            <el-text size="small">✅ Đơn hàng chuyển sang <strong>PAID</strong></el-text>
            <el-text size="small">✅ Xuất kho tự động</el-text>
            <el-text size="small">✅ <strong>Cộng điểm loyalty</strong> cho khách hàng</el-text>
          </el-space>
        </template>
      </el-alert>

      <el-form label-position="top">
        <el-form-item label="Số tiền thanh toán">
          <el-text type="primary" style="font-size: 20px; font-weight: 700;">{{ formatMoney(detail?.totalAmount) }}</el-text>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button plain @click="showPaymentDialog = false">Đóng</el-button>
        <el-button
          type="primary"
          :loading="paymentLoading"
          @click="confirmPayment"
        >
          Xác nhận thanh toán
        </el-button>
      </template>
    </el-dialog>

    <!-- ── CANCEL DIALOG ── -->
    <el-dialog v-model="showCancelDialog" title="❌ Hủy đơn hàng" width="440px" align-center>
      <el-alert
        :type="detail?.paymentStatus === 'PAID' ? 'error' : 'warning'"
        :title="getCancelWarningTitle()"
        :closable="false"
        show-icon
        style="margin-bottom: 16px;"
      >
        <div v-html="getCancelWarningMessage()" />
      </el-alert>
      <el-form label-position="top">
        <el-form-item>
          <template #label>
            Lý do hủy <el-text size="small" type="info">(không bắt buộc)</el-text>
          </template>
          <el-input v-model="cancelReason" type="textarea" :rows="3" placeholder="Nhập lý do hủy đơn..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button plain @click="showCancelDialog = false">Đóng</el-button>
        <el-button type="danger" :loading="cancelLoading" @click="confirmCancel">Xác nhận hủy</el-button>
      </template>
    </el-dialog>

    <!-- ── DELIVERED DIALOG ── -->
    <el-dialog v-model="showDeliveredDialog" title="📦 Xác nhận đã nhận hàng" width="440px" align-center>
      <el-space direction="vertical" fill :size="16" style="width: 100%;">
        <el-row align="middle" :gutter="16">
          <el-col flex="none"><span style="font-size: 40px;">✅</span></el-col>
          <el-col flex="1">
            <el-text tag="div" style="font-weight: 700; margin-bottom: 4px;">Bạn đã nhận được hàng?</el-text>
            <el-text size="small" type="info">Hành động này không thể hoàn tác. Vui lòng kiểm tra hàng trước khi xác nhận.</el-text>
          </el-col>
        </el-row>
        <el-card shadow="never" style="background: var(--el-fill-color-lighter);" :body-style="{ padding: '14px 16px' }">
          <el-text tag="div" style="font-weight: 600; margin-bottom: 8px;">Sau khi xác nhận</el-text>
          <el-space direction="vertical" :size="6">
            <el-text size="small"><el-icon style="color: var(--el-color-success);"><CircleCheck /></el-icon> Đơn hàng chuyển sang trạng thái <strong>DELIVERED</strong></el-text>
            <el-text size="small"><el-icon style="color: var(--el-color-success);"><CircleCheck /></el-icon> Người bán nhận được thanh toán</el-text>
            <el-text size="small"><el-icon style="color: var(--el-color-warning);"><Warning /></el-icon> Bạn vẫn có thể gửi <strong>yêu cầu trả hàng</strong> nếu cần</el-text>
          </el-space>
        </el-card>
      </el-space>
      <template #footer>
        <el-button plain @click="showDeliveredDialog = false">Chưa nhận</el-button>
        <el-button type="success" :loading="deliveredLoading" @click="doConfirmDelivered">
          <el-icon><Check /></el-icon> Xác nhận nhận hàng
        </el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import {
  Check, CircleCheck, Clock, Close, CreditCard, Loading,
  Refresh, User, Warning,
} from "@element-plus/icons-vue";
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ordersApi } from "../../api/orders.api";
import { returnsApi } from "../../api/returns.api";
import { paymentsApi } from "../../api/payments";
import { spinWheelApi } from "../../api/spinWheel.api";
import { toast } from "../../ui/toast";

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const cancelLoading = ref(false);
const paymentLoading = ref(false);
const detail = ref(null);
const orderId = computed(() => route.params.orderId);

const showCancelDialog = ref(false);
const cancelReason = ref("");
const showReturnDialog = ref(false);
const showPaymentDialog = ref(false);

const spinStatus = reactive({
  loading: false, checked: false, hasActiveBonus: false,
  bonusRate: 0, bonusExpiresAt: null,
});

const qrCodeUrl = ref("");

const isHomeDelivery = computed(() => {
  if (!detail.value?.notes) return false;
  return detail.value.notes.toLowerCase().includes("giao tại nhà");
});

const timelineSteps = computed(() => {
  if (!detail.value) return [];
  const s = detail.value.status;
  const isCash = detail.value.paymentMethod === "CASH";
  const steps = [
    { key: "placed", label: "Đặt hàng", statuses: ["PENDING","PAID","PROCESSING","SHIPPING","DELIVERED"] },
  ];
  if (!isCash) steps.push({ key: "paid", label: "Thanh toán", statuses: ["PAID","PROCESSING","SHIPPING","DELIVERED"] });
  steps.push(
    { key: "processing", label: "Chuẩn bị", statuses: ["PROCESSING","SHIPPING","DELIVERED"] },
    { key: "shipping", label: "Đang chờ nhận", statuses: ["SHIPPING","DELIVERED"] },
    { key: "delivered", label: "Đã giao", statuses: ["DELIVERED"] },
  );
  return steps.map(step => ({ ...step, active: step.statuses.includes(s), current: step.statuses[0] === s }));
});

const timelineActiveIndex = computed(() => {
  const idx = timelineSteps.value.findIndex(s => s.current);
  return idx === -1 ? timelineSteps.value.filter(s => s.active).length : idx + 1;
});

const timelineFinishStatus = computed(() =>
  detail.value?.status === 'DELIVERED' ? 'success' : 'finish'
);

function statusTagType(status) {
  const map = { PENDING:'warning', PAID:'primary', PROCESSING:'', SHIPPING:'primary', DELIVERED:'success', CANCELLED:'danger' };
  return map[status] || 'info';
}

function parseNotes(notes) {
  if (!notes) return [];
  return notes.split("|").map(s => s.trim()).filter(Boolean).map(line => {
    if (line.toLowerCase().includes("vip")) return { type:"vip", icon:"👑", text:line };
    if (line.toLowerCase().includes("mã") || line.toLowerCase().includes("discount")) return { type:"discount", icon:"🏷️", text:line };
    if (line.toLowerCase().includes("giao") || line.toLowerCase().includes("nhận tại")) return { type:"delivery", icon:"📍", text:line };
    if (line.toLowerCase().includes("thanh toán")) return { type:"payment", icon:"💳", text:line };
    return { type:"default", icon:"📝", text:line };
  });
}

function formatDateTime(dateStr) {
  if (!dateStr) return "—";
  const d = new Date(dateStr);
  return `${d.toLocaleTimeString("vi-VN",{hour:"2-digit",minute:"2-digit"})} · ${d.toLocaleDateString("vi-VN",{day:"2-digit",month:"2-digit",year:"numeric"})}`;
}

function generateTransferQR() {
  const amount = detail.value?.totalAmount || 0;
  const order = detail.value?.orderNumber || orderId.value;
  qrCodeUrl.value = `https://img.vietqr.io/image/MB-0344269926-compact2.png?amount=${amount}&addInfo=ORDER${order}`;
}

async function openPaymentDialog() {
  showPaymentDialog.value = true;
  await fetchSpinStatus();
}

const vnpayLoading = ref(false);

async function openVnPayPayment() {
  vnpayLoading.value = true;
  try {
    const res = await paymentsApi.getVnPayUrl(orderId.value);
    const paymentUrl = res?.data?.paymentUrl;
    if (paymentUrl) {
      window.location.href = paymentUrl;
    } else {
      toast("Không lấy được link thanh toán VNPay", "error");
    }
  } catch (err) {
    toast(err?.response?.data?.message || "Lỗi khi tạo link VNPay", "error");
  } finally {
    vnpayLoading.value = false;
  }
}

const estimatedSpinDiscount = computed(() => {
  if (!detail.value?.subtotal || !spinStatus.bonusRate) return 0;
  if (detail.value?.spinDiscountRate > 0) return 0;
  return Math.round((detail.value.subtotal * spinStatus.bonusRate) / 100);
});

function formatMoney(val) {
  if (val === null || val === undefined) return "0 ₫";
  return new Intl.NumberFormat("vi-VN", { style:"currency", currency:"VND" }).format(val);
}

function formatExpiry(dateStr) {
  if (!dateStr) return "";
  return new Date(dateStr).toLocaleString("vi-VN", { day:"2-digit", month:"2-digit", year:"numeric", hour:"2-digit", minute:"2-digit" });
}

async function reload() {
  loading.value = true;
  try {
    const res = await ordersApi.getById(orderId.value);
    detail.value = res?.data?.data || res?.data;
  } catch { toast("Không thể tải chi tiết đơn hàng", "error"); }
  finally { loading.value = false; }
}

async function fetchSpinStatus() {
  const customerId = detail.value?.customerId;
  if (!customerId) return;
  spinStatus.loading = true; spinStatus.checked = false; spinStatus.hasActiveBonus = false;
  try {
    const res = await spinWheelApi.getStatus(customerId);
    const data = res?.data?.data ?? res?.data ?? res;
    const bonus = Number(data?.currentBonus ?? 0);
    if (bonus > 0) { spinStatus.hasActiveBonus = true; spinStatus.bonusRate = bonus; spinStatus.bonusExpiresAt = data?.bonusExpiresAt ?? null; }
  } catch (err) { console.warn("SpinWheel error:", err?.message); }
  finally { spinStatus.loading = false; spinStatus.checked = true; }
}

function getCancelWarningTitle() {
  return detail.value?.paymentStatus === "PAID" ? "⚠️ Cảnh báo: Hủy đơn đã thanh toán" : "Xác nhận hủy đơn";
}

function getCancelWarningMessage() {
  if (detail.value?.paymentStatus === "PAID") {
    const pts = Math.floor((detail.value.totalAmount || 0) / 10000);
    return `<p><strong>Nếu hủy đơn đã thanh toán:</strong></p><ul><li>❌ Trừ lại <strong>${pts} điểm</strong> loyalty</li><li>💰 Hoàn tiền cho khách</li><li>📦 Nhập lại kho</li></ul>`;
  }
  return `<p>Đơn chưa thanh toán sẽ được hủy miễn phí.</p>`;
}

// ✅ FIX Issue 1: confirmPayment chỉ dùng cho non-TRANSFER
async function confirmPayment() {
  paymentLoading.value = true;
  try {
    // Nếu khách chọn VNPay (hoặc bất kỳ tên gì mày đặt cho VNPay ở DB)
    if (detail.value.paymentMethod === 'TRANSFER') {
      const res = await paymentsApi.getVnPayUrl(orderId.value);
      const paymentUrl = res.data.paymentUrl;
      if (paymentUrl) {
        window.location.href = paymentUrl; // Đẩy khách sang VNPay
        return;
      }
    }

    // Nếu là các phương thức thanh toán khác (như ví nội bộ, tiền mặt tại quầy...)
    await paymentsApi.create({ 
      orderId: Number(orderId.value), 
      method: detail.value.paymentMethod, 
      transactionRef: `TXN-${Date.now()}` 
    });
    
    toast("Thanh toán thành công", "success");
    showPaymentDialog.value = false;
    await reload();
  } catch (err) { 
    toast(err.response?.data?.message || "Lỗi thanh toán", "error"); 
  } finally { 
    paymentLoading.value = false; 
  }
}

async function confirmCancel() {
  cancelLoading.value = true;
  try {
    await ordersApi.cancel(orderId.value, cancelReason.value);
    toast("✅ Đã hủy đơn hàng thành công.", "success");
    showCancelDialog.value = false;
    await reload();
  } catch { toast("Lỗi khi hủy đơn", "error"); }
  finally { cancelLoading.value = false; }
}

const deliveredLoading = ref(false);
const showDeliveredDialog = ref(false);

const doConfirmDelivered = async () => {
  deliveredLoading.value = true;
  showDeliveredDialog.value = false;
  try {
    await ordersApi.markAsDelivered(orderId.value);
    toast("Đã xác nhận nhận hàng", "success");
    await reload();
  } catch { toast("Không thể cập nhật trạng thái đơn hàng", "error"); }
  finally { deliveredLoading.value = false; }
};

function isReturned(status) {
  return ["PARTIALLY_RETURNED","RETURNED"].includes(status);
}

onMounted(() => reload());
</script>

<style scoped>
.order-layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 20px;
  align-items: start;
}

@media (max-width: 900px) {
  .order-layout { grid-template-columns: 1fr; }
}
</style>