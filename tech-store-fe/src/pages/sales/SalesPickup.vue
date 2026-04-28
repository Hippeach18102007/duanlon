<template>
  <el-container class="pickup-container">
    <el-header class="page-header">
      <el-row align="middle" justify="space-between">
        <el-space direction="vertical" :size="2" align="start">
          <el-text tag="b" size="large">NHẬN HÀNG TẠI QUẦY</el-text>
          <el-text type="info" size="small">Nhập <b>đầy đủ</b> SĐT/Email để tìm chính xác — hoặc nhập một phần để xem gợi ý</el-text>
        </el-space>
        <!-- Auto-refresh indicator -->
        <el-space>
          <el-tag :type="autoRefreshActive ? 'success' : 'info'" size="small" effect="plain">
            {{ autoRefreshActive ? 'Đang làm mới...' : `Làm mới sau ${countdown}s` }}
          </el-tag>
          <el-button :icon="Refresh" size="small" @click="doSearch" :loading="loading" plain>Làm mới ngay</el-button>
        </el-space>
      </el-row>
    </el-header>

    <el-main class="main-content">
      <!-- Search Section -->
      <el-card shadow="never" class="mb-15">
        <el-row :gutter="10">
          <el-col :span="18">
            <el-input
              ref="searchInput"
              v-model="query"
              placeholder="Nhập SĐT hoặc email khách hàng..."
              @keyup.enter="doSearch"
              clearable
              size="large"
            >
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-col>
          <el-col :span="6">
            <el-button type="primary" size="large" style="width: 100%" :loading="loading" :disabled="!query.trim()" @click="doSearch">
              TÌM KIẾM
            </el-button>
          </el-col>
        </el-row>
        <el-alert v-if="errorMsg" :title="errorMsg" type="error" show-icon :closable="false" class="mt-10" />
      </el-card>

      <!-- Multiple candidates: show selection list -->
      <el-card v-if="candidates.length > 1" shadow="never" class="mb-15">
        <template #header>
          <el-text type="warning" tag="b"><el-icon><Warning /></el-icon> Tìm thấy {{ candidates.length }} khách hàng khớp — vui lòng chọn đúng người:</el-text>
        </template>
        <el-space direction="vertical" fill style="width:100%" :size="8">
          <el-card
            v-for="c in candidates"
            :key="c.id"
            shadow="hover"
            body-style="padding:12px"
            class="candidate-card"
            @click="selectCandidate(c)"
          >
            <el-row align="middle" justify="space-between">
              <el-space>
                <el-avatar :size="36" style="background:#409eff;flex-shrink:0">{{ initials(c.fullName || c.name) }}</el-avatar>
                <el-space direction="vertical" align="start" :size="0">
                  <el-text tag="b">{{ c.fullName || c.name }}</el-text>
                  <el-text type="info" size="small">{{ c.phone }} &nbsp;|&nbsp; {{ c.email }}</el-text>
                </el-space>
              </el-space>
              <el-button type="primary" size="small" plain>Chọn người này</el-button>
            </el-row>
          </el-card>
        </el-space>
      </el-card>

      <div v-if="customer">  
        <!-- Customer Info -->
        <el-card shadow="never" class="mb-15">
          <el-row align="middle" justify="space-between">
            <el-space>
              <el-avatar :size="44" style="background:#409eff">{{ initials(customer.fullName || customer.name) }}</el-avatar>
              <el-space direction="vertical" align="start" :size="2">
                <el-text tag="b" size="large">{{ customer.fullName || customer.name }}</el-text>
                <el-space :size="15">
                  <el-text v-if="customer.phone" type="info" size="small"><el-icon><Phone /></el-icon> {{ customer.phone }}</el-text>
                  <el-text v-if="customer.email" type="info" size="small"><el-icon><Message /></el-icon> {{ customer.email }}</el-text>
                </el-space>
              </el-space>
            </el-space>
            <el-space>
              <el-tag v-if="customer.vipTier" type="warning" plain>{{ customer.vipTier }}</el-tag>
              <el-tag type="primary" effect="plain">{{ paidOrders.length }} đơn chờ lấy</el-tag>
            </el-space>
          </el-row>
        </el-card>

        <!-- Bulk Action Bar -->
        <el-card v-if="paidOrders.length > 0" shadow="never" class="mb-15 bulk-bar">
          <el-row align="middle" justify="space-between">
            <el-space>
              <el-checkbox
                v-model="allSelected"
                :indeterminate="selectedKeys.length > 0 && selectedKeys.length < paidOrders.length"
                @change="toggleSelectAll"
              >Chọn tất cả ({{ paidOrders.length }})</el-checkbox>
              <el-text v-if="selectedKeys.length > 0" type="primary" size="small">Đã chọn {{ selectedKeys.length }} đơn</el-text>
            </el-space>
            <el-button
              v-if="selectedKeys.length > 0"
              type="success"
              :loading="bulkLoading"
              @click="confirmBulkPickup"
              plain
            >
              <el-icon><Check /></el-icon> Hoàn tất {{ selectedKeys.length }} đơn đã chọn
            </el-button>
          </el-row>
        </el-card>

        <!-- Orders List -->
        <el-empty v-if="paidOrders.length === 0" description="Không có đơn hàng nào đang chờ lấy" />

        <el-space v-else direction="vertical" fill style="width: 100%" :size="10">
          <el-card
            v-for="order in paidOrders"
            :key="orderKey(order)"
            shadow="hover"
            :class="['order-card', { 'is-selected': selectedKeys.includes(orderKey(order)) }]"
          >
            <!-- Card Header Row -->
            <div class="order-summary">
              <el-row align="middle" justify="space-between">
                <!-- Checkbox + Order Number -->
                <el-space>
                  <el-checkbox
                    :model-value="selectedKeys.includes(orderKey(order))"
                    @change="toggleSelect(order)"
                    @click.stop
                  />
                  <div @click="toggleExpand(order)" style="cursor:pointer">
                    <el-text tag="b" class="mono">{{ order.orderNumber || ('#' + order.id) }}</el-text>
                    <!-- Brief product summary -->
                    <div class="product-brief" v-if="briefItems[orderKey(order)]">
                      <el-text type="info" size="extra-small">
                        {{ briefItems[orderKey(order)] }}
                      </el-text>
                    </div>
                  </div>
                </el-space>

                <!-- Right: Status Tags + Amount + SLA + Arrow -->
                <el-space @click="toggleExpand(order)" style="cursor:pointer">
                  <!-- Payment Status Tag -->
                  <el-tag
                    :type="isOrderPaid(order) ? 'success' : 'danger'"
                    size="small"
                    effect="dark"
                  >
                    {{ isOrderPaid(order) ? 'ĐÃ THANH TOÁN' : 'CHƯA THANH TOÁN' }}
                  </el-tag>

                  <!-- SLA Waiting Time -->
                  <el-tag :type="getSlaType(order)" size="small" effect="plain">
                    {{ formatWaitingTime(order.createdAt) }}
                  </el-tag>

                  <el-space direction="vertical" align="end" :size="0">
                    <el-text tag="b">{{ formatMoney(order.totalAmount) }}</el-text>
                    <el-tag :type="getChannelType(order.channel)" size="small" plain>{{ channelLabel(order.channel) }}</el-tag>
                  </el-space>
                  <el-icon :class="{ rotated: expandedOrder === orderKey(order) }"><ArrowDown /></el-icon>
                </el-space>
              </el-row>
            </div>

            <!-- Expandable Detail -->
            <div v-if="expandedOrder === orderKey(order)" class="order-detail">
              <el-divider style="margin: 10px 0" />

              <el-table v-if="orderItems[orderKey(order)]" :data="orderItems[orderKey(order)]" size="small" border>
                <el-table-column label="Sản phẩm">
                  <template #default="{ row }">
                    <el-text tag="b" size="small">{{ row.productName || row.variant?.product?.name }}</el-text>
                    <div class="variant-text">{{ row.variantName || row.variant?.name }}</div>
                  </template>
                </el-table-column>
                <el-table-column label="SL" width="60" align="center" prop="quantity" />
                <el-table-column label="Thành tiền" width="130" align="right">
                  <template #default="{ row }">{{ formatMoney(row.lineTotal || (row.unitPrice * row.quantity)) }}</template>
                </el-table-column>
              </el-table>
              <div v-else class="loading-items"><el-icon class="is-loading"><Loading /></el-icon> Đang tải danh sách...</div>

              <el-alert v-if="order.notes" :title="'Ghi chú: ' + order.notes" type="info" :closable="false" class="mt-10" plain />

              <!-- Payment Warning (ràng buộc) -->
              <el-alert
                v-if="!isOrderPaid(order)"
                title="⚠️ Đơn hàng này CHƯA được thanh toán. Thu tiền từ khách trước khi giao máy!"
                type="error"
                :closable="false"
                show-icon
                class="mt-10"
              />

              <!-- Confirm Button -->
              <div class="confirm-row mt-15">
                <el-alert v-if="confirmError[orderKey(order)]" :title="confirmError[orderKey(order)]" type="error" show-icon :closable="false" class="mb-10" />
                <el-button
                  type="success"
                  style="width: 100%"
                  :loading="confirmLoading[orderKey(order)]"
                  :disabled="!isOrderPaid(order)"
                  @click="confirmPickup(order)"
                  plain
                >
                  {{ isOrderPaid(order) ? 'XÁC NHẬN ĐÃ GIAO HÀNG' : 'CHƯA THU TIỀN — KHÔNG THỂ GIAO' }}
                </el-button>
              </div>
            </div>
          </el-card>
        </el-space>
      </div>

      <el-empty v-else-if="!loading" description="Nhập SĐT hoặc email của khách để tra cứu đơn hàng" />
    </el-main>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from "vue";
import { User, Phone, Message, ArrowDown, Loading, Refresh, Check, Timer, Warning } from "@element-plus/icons-vue";
import { customersApi } from "../../api/customers.api";
import { ordersApi } from "../../api/orders.api";
import { paymentsApi } from "../../api/payments";
import { toast } from "../../ui/toast";
import { confirmModal } from "../../ui/confirm";

// ── State ──
const searchInput = ref(null);
const query = ref("");
const loading = ref(false);
const errorMsg = ref("");
const candidates = ref([]);   // Danh sách KH khớp khi có nhiều kết quả
const customer = ref(null);
const paidOrders = ref([]);
const expandedOrder = ref(null);
const orderItems = ref({});
const briefItems = ref({});     // Tóm tắt sản phẩm cho từng đơn
const confirmLoading = ref({});
const confirmError = ref({});

// ── Bulk Action ──
const selectedKeys = ref([]);
const bulkLoading = ref(false);
const allSelected = computed(() => paidOrders.value.length > 0 && selectedKeys.value.length === paidOrders.value.length);

function toggleSelectAll() {
  if (selectedKeys.value.length === paidOrders.value.length) {
    selectedKeys.value = [];
  } else {
    selectedKeys.value = paidOrders.value.map(o => orderKey(o));
  }
}
function toggleSelect(order) {
  const key = orderKey(order);
  if (selectedKeys.value.includes(key)) {
    selectedKeys.value = selectedKeys.value.filter(k => k !== key);
  } else {
    selectedKeys.value = [...selectedKeys.value, key];
  }
}

// ── Auto-refresh (Real-time Polling) ──
const REFRESH_INTERVAL = 30; // seconds
const countdown = ref(REFRESH_INTERVAL);
const autoRefreshActive = ref(false);
let refreshTimer = null;
let countdownTimer = null;

function startAutoRefresh() {
  stopAutoRefresh();
  countdownTimer = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0) {
      countdown.value = REFRESH_INTERVAL;
      if (customer.value) silentRefresh();
    }
  }, 1000);
}

function stopAutoRefresh() {
  clearInterval(refreshTimer);
  clearInterval(countdownTimer);
}

async function silentRefresh() {
  if (!customer.value) return;
  autoRefreshActive.value = true;
  try {
    const ordRes = await ordersApi.filter(customer.value.id, null, null, null, null);
    const rawOrders = ordRes.data?.data || ordRes.data?.content || ordRes.data || [];
    const fresh = (Array.isArray(rawOrders) ? rawOrders : []).filter(isPendingPickup);
    if (fresh.length !== paidOrders.value.length) {
      paidOrders.value = fresh;
      await loadAllBriefs();
      if (fresh.length > paidOrders.value.length) {
        toast("Có đơn hàng mới!", "success");
      }
    }
  } catch { /* silent */ }
  finally { autoRefreshActive.value = false; }
}

// ── Search ──
async function doSearch() {
  const q = query.value.trim().toLowerCase();
  if (!q) return;
  loading.value = true;
  errorMsg.value = "";
  customer.value = null;
  candidates.value = [];
  paidOrders.value = [];
  expandedOrder.value = null;
  orderItems.value = {};
  briefItems.value = {};
  selectedKeys.value = [];
  countdown.value = REFRESH_INTERVAL;
  try {
    const cusRes = await customersApi.listAll();
    const cusList = cusRes.data?.data || cusRes.data || [];

    // Dùng filter() thay vì find() để lấy TẤT CẢ khách hàng khớp
    const matched = cusList.filter(c =>
      (c.phone || "").includes(q) ||
      (c.email || "").toLowerCase().includes(q)
    );

    if (matched.length === 0) { errorMsg.value = "Không tìm thấy khách hàng."; return; }

    if (matched.length > 1) {
      // Nhiều kết quả → hiện danh sách để chọn, không tự chọn người đầu tiên
      candidates.value = matched;
      return;
    }

    // Chỉ 1 kết quả → load đơn hàng ngay
    await loadOrdersForCustomer(matched[0]);
  } catch (e) { errorMsg.value = "Lỗi tìm kiếm đơn hàng."; }
  finally { loading.value = false; }
}

async function selectCandidate(c) {
  candidates.value = [];
  loading.value = true;
  await loadOrdersForCustomer(c);
  loading.value = false;
}

async function loadOrdersForCustomer(c) {
  customer.value = c;
  try {
    const ordRes = await ordersApi.filter(c.id, null, null, null, null);
    const rawOrders = ordRes.data?.data || ordRes.data?.content || ordRes.data || [];
    paidOrders.value = (Array.isArray(rawOrders) ? rawOrders : []).filter(isPendingPickup);
    await loadAllBriefs();
  } catch { paidOrders.value = []; }
}

// ── Brief Product Summary ──
async function loadAllBriefs() {
  await Promise.allSettled(paidOrders.value.map(async (order) => {
    const key = orderKey(order);
    if (briefItems.value[key]) return;
    try {
      const oid = order.orderId || order.id;
      const res = await ordersApi.getOrderDetail(oid);
      const detail = res.data?.data || res.data;
      const items = detail?.orderItems || detail?.items || detail?.products || [];
      if (items.length === 0) return;
      const names = items.map(i => i.productName || i.variant?.product?.name || 'SP').filter(Boolean);
      const brief = names.slice(0, 2).join(', ') + (names.length > 2 ? ` (+${names.length - 2} sp)` : '');
      briefItems.value = { ...briefItems.value, [key]: brief };
      orderItems.value = { ...orderItems.value, [key]: items };
    } catch { /* ignore */ }
  }));
}

// ── Expand ──
async function toggleExpand(order) {
  const key = orderKey(order);
  if (expandedOrder.value === key) { expandedOrder.value = null; return; }
  expandedOrder.value = key;
  if (!orderItems.value[key] || orderItems.value[key].length === 0) {
    try {
      const oid = order.orderId || order.id;
      const res = await ordersApi.getOrderDetail(oid);
      const detail = res.data?.data || res.data;
      const items = detail?.orderItems || detail?.items || detail?.products || [];
      orderItems.value = { ...orderItems.value, [key]: items };
    } catch {
      orderItems.value = { ...orderItems.value, [key]: [] };
    }
  }
}

// ── Confirm Single Pickup (with payment guard) ──
async function confirmPickup(order) {
  if (!isOrderPaid(order)) {
    toast("Đơn chưa thanh toán, không thể xác nhận giao hàng!", "error");
    return;
  }

  const ok = await confirmModal(
    `Xác nhận đã thu đủ ${formatMoney(order.totalAmount)} và giao hàng cho khách?`,
    "Xác nhận giao hàng",
    "Hoàn tất giao",
    false
  );
  if (!ok) return;

  const key = orderKey(order);
  const oid = order.orderId || order.id;
  confirmLoading.value = { ...confirmLoading.value, [key]: true };
  confirmError.value = { ...confirmError.value, [key]: "" };

  try {
    // 1. Ghi nhận thanh toán cho đơn CASH (theo chuẩn hàm mẫu của bạn)
    const method = (order.paymentMethod || "").toUpperCase();
    const payStatus = (order.paymentStatus || "").toUpperCase();

    if (method === "CASH" && payStatus !== "PAID" && payStatus !== "COMPLETED") {
      await paymentsApi.create({
        orderId: Number(oid),
        amount: order.totalAmount,
        method: method,
        paymentStatus: "PAID",
        transactionRef: `TXN-PICKUP-${Date.now()}`
      });
    }

    // 2. Chuyển trạng thái đơn hàng
    await ordersApi.markAsProcessing(oid).catch(() => {});
    await ordersApi.markAsShipping(oid).catch(() => {});
    await ordersApi.markAsDelivered(oid);

    paidOrders.value = paidOrders.value.filter(o => orderKey(o) !== key);
    selectedKeys.value = selectedKeys.value.filter(k => k !== key);
    expandedOrder.value = null;
    toast(`✅ Đơn ${order.orderNumber || oid} đã hoàn tất!`, "success");
  } catch (e) {
    console.error("Confirm error:", e);
    const msg = e.response?.data?.message || "Xác nhận thất bại. Vui lòng thử lại.";
    toast(msg, "error");
    confirmError.value = { ...confirmError.value, [key]: msg };
  } finally {
    confirmLoading.value = { ...confirmLoading.value, [key]: false };
  }
}

// ── Bulk Confirm ──
async function confirmBulkPickup() {
  const ok = await confirmModal(`Xác nhận hoàn tất ${selectedKeys.value.length} đơn hàng?`, "Phê duyệt hàng loạt", "Xác nhận", false);
  if (!ok) return;
  bulkLoading.value = true;
  let successCount = 0;
  await Promise.allSettled(
    selectedKeys.value.map(async (key) => {
      const order = paidOrders.value.find(o => orderKey(o) === key);
      if (!order) return;
      const oid = order.orderId || order.id;
      try {
        const method = (order.paymentMethod || "").toUpperCase();
        const payStatus = (order.paymentStatus || "").toUpperCase();
        if (method === "CASH" && (payStatus !== "PAID" && payStatus !== "COMPLETED")) {
          await paymentsApi.create({ 
            orderId: oid, 
            amount: order.totalAmount, 
            method: "CASH", 
            paymentStatus: "PAID" 
          }).catch(() => {});
        }
        await ordersApi.markAsProcessing(oid).catch(() => {});
        await ordersApi.markAsShipping(oid).catch(() => {});
        await ordersApi.markAsDelivered(oid);
        successCount++;
      } catch { /* individual error */ }
    })
  );
  paidOrders.value = paidOrders.value.filter(o => !selectedKeys.value.includes(orderKey(o)));
  selectedKeys.value = [];
  bulkLoading.value = false;
  toast(`✅ Hoàn tất ${successCount} đơn hàng!`, "success");
}

// ── Helpers ──
const orderKey = (o) => o.orderNumber || String(o.id);

// Đơn cần hiện ở Pickup: OFFLINE và chưa hoàn tất / chưa hủy
const DONE_STATUSES = ["DELIVERED", "CANCELLED", "REFUNDED", "COMPLETED"];
function isPendingPickup(o) {
  const channel = (o.channel || "").toUpperCase();
  const status  = (o.status  || "").toUpperCase();
  return channel === "OFFLINE" && !DONE_STATUSES.includes(status);
}

// Nghiệp vụ thanh toán:
// - CASH + OFFLINE: khách trả tiền mặt tại quầy → luôn cho phép giao hàng
// - TRANSFER / CARD: đã TT online → phải kiểm tra trạng thái PAID trước
function isOrderPaid(order) {
  const method = (order.paymentMethod || "").toUpperCase();
  const channel = (order.channel || "").toUpperCase();

  // CASH tại quầy: chưa thu nhưng sẽ thu khi giao → không chặn
  if (method === "CASH" && channel === "OFFLINE") return true;

  // Online payment (TRANSFER / CARD): phải đã thanh toán
  const payStatus = (order.paymentStatus || "").toUpperCase();
  const status    = (order.status || "").toUpperCase();
  return payStatus === "PAID" || payStatus === "COMPLETED" || status === "PAID";
}

// SLA: how long the order has been waiting
function formatWaitingTime(createdAt) {
  if (!createdAt) return "—";
  const diffMs = Date.now() - new Date(createdAt).getTime();
  const mins = Math.floor(diffMs / 60000);
  if (mins < 60) return `${mins} phút`;
  const hrs = Math.floor(mins / 60);
  return `${hrs}h${mins % 60}p`;
}

function getSlaType(order) {
  if (!order.createdAt) return "info";
  const mins = Math.floor((Date.now() - new Date(order.createdAt).getTime()) / 60000);
  if (mins > 60) return "danger";   // Đã chờ hơn 1 tiếng
  if (mins > 30) return "warning";  // Đã chờ hơn 30 phút
  return "success";
}

function formatMoney(v) { return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(v || 0); }
function formatDate(d) { return d ? new Date(d).toLocaleString("vi-VN") : "—"; }
function initials(n = "") { return (n || "").split(" ").map(w => w[0]).filter(Boolean).slice(0, 2).join("").toUpperCase() || "KH"; }
function channelLabel(ch) { return { ONLINE: "Online", OFFLINE: "Tại quầy", PHONE: "Điện thoại" }[ch] || ch || "—"; }
function getChannelType(ch) { return ch === "OFFLINE" ? "warning" : ch === "ONLINE" ? "info" : "primary"; }

onMounted(() => {
  searchInput.value?.focus();
  startAutoRefresh();
});
onUnmounted(() => stopAutoRefresh());
</script>

<style scoped>
.pickup-container { background: #f0f2f5; min-height: 100vh; }
.page-header { padding: 15px 20px; height: auto; background: #fff; border-bottom: 1px solid #ebeef5; }
.main-content { padding: 0; }
.mb-15 { margin-bottom: 15px; }
.mt-10 { margin-top: 10px; }
.mt-15 { margin-top: 15px; }
.mb-10 { margin-bottom: 10px; }
.order-summary { cursor: pointer; padding: 5px 0; }
.order-detail { padding-top: 5px; }
.variant-text { font-size: 11px; color: #909399; }
.loading-items { padding: 15px; text-align: center; color: #909399; }
.mono { font-family: monospace; font-weight: bold; }
.rotated { transform: rotate(180deg); transition: transform 0.2s; }
.product-brief { margin-top: 2px; }
.order-card { transition: border 0.2s; }
.order-card.is-selected { border-color: #409eff; }
.bulk-bar { background: #f0f7ff; border: 1px solid #d9ecff; }
.candidate-card { cursor: pointer; transition: border 0.2s; }
.candidate-card:hover { border-color: #409eff; }

</style>
