<template>
  <div class="pickup-layout">
    <!-- Header -->
    <div class="pickup-header">
      <div class="header-left">
        <div class="header-title">NHẬN HÀNG TẠI QUẦY</div>
        <div class="header-sub">
          {{ filteredGroups.length }} khách đang chờ ·
          {{ totalPendingOrders }} đơn
        </div>
      </div>
      <div class="header-right">
        <el-tag :type="autoRefreshActive ? 'success' : 'info'" size="small" effect="plain">
          {{ autoRefreshActive ? 'Đang làm mới...' : `Làm mới sau ${countdown}s` }}
        </el-tag>
        <el-button :icon="Refresh" size="small" @click="loadAllPickups(false)" :loading="loading" plain>
          Làm mới ngay
        </el-button>
      </div>
    </div>

    <div class="pickup-body">
      <!-- LEFT: Customer Queue -->
      <div class="queue-panel">
        <div class="queue-search">
          <el-input v-model="filterText" placeholder="Lọc theo tên / SĐT..." clearable size="small">
            <template #prefix><el-icon>
                <Search />
              </el-icon></template>
          </el-input>
        </div>

        <el-alert v-if="errorMsg" :title="errorMsg" type="error" show-icon :closable="false" class="mx-12 mt-10" />

        <div v-if="loading" class="queue-loading">
          <el-icon class="is-loading" :size="20">
            <Loading />
          </el-icon>
          <span>Đang tải danh sách...</span>
        </div>
        <el-empty v-else-if="!filteredGroups.length" description="Không có đơn chờ lấy" :image-size="60"
          class="mt-30" />

        <div v-else class="queue-list">
          <div v-for="group in filteredGroups" :key="group.customerId" class="queue-item"
            :class="{ 'is-active': selectedCustomerId === group.customerId, 'has-urgent': group.hasUrgent }"
            @click="selectGroup(group)">
            <el-avatar :size="38" :style="{ background: group.hasUrgent ? '#f56c6c' : '#409eff', flexShrink: 0 }">
              {{ initials(group.customerName) }}
            </el-avatar>
            <div class="queue-item-info">
              <div class="queue-item-name">{{ group.customerName }}</div>
              <div class="queue-item-phone">{{ group.customerPhone || '—' }}</div>
            </div>
            <div class="queue-item-badge">
              <el-tag :type="group.hasUrgent ? 'danger' : 'primary'" size="small" effect="dark">
                {{ group.orders.length }} đơn
              </el-tag>
              <div class="queue-sla" :class="'sla-' + group.slaType">{{ group.maxWaitTime }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- RIGHT: Order Detail -->
      <div class="order-panel">
        <div v-if="selectedGroup" class="order-panel-inner">
          <!-- Customer header -->
          <div class="customer-header">
            <el-avatar :size="44" style="background:#409eff; flex-shrink:0">
              {{ initials(selectedGroup.customerName) }}
            </el-avatar>
            <div class="customer-header-info">
              <div class="customer-header-name">{{ selectedGroup.customerName }}</div>
              <div class="customer-header-sub">
                <span v-if="selectedGroup.customerPhone">
                  <el-icon>
                    <Phone />
                  </el-icon> {{ selectedGroup.customerPhone }}
                </span>
                <el-tag type="primary" effect="light" size="small">
                  {{ selectedGroup.orders.length }} đơn chờ lấy
                </el-tag>
              </div>
            </div>
          </div>

          <!-- Orders scroll area -->
          <div class="orders-scroll">
            <div v-for="order in selectedGroup.orders" :key="orderKey(order)" class="order-card"
              :class="{ 'is-expanded': expandedOrder === orderKey(order) }">
              <!-- Summary row -->
              <div class="order-summary" @click="toggleExpand(order)">
                <div class="order-summary-left">
                  <span class="order-number mono">{{ order.orderNumber || ('#' + order.id) }}</span>
                  <div v-if="briefItems[orderKey(order)]" class="order-brief">
                    {{ briefItems[orderKey(order)] }}
                  </div>
                </div>
                <div class="order-summary-right">
                  <el-tag :type="getPaymentTagType(order)" size="small" effect="dark">
                    {{ getPaymentTagLabel(order) }}
                  </el-tag>
                  <el-tag :type="getSlaType(order)" size="small" effect="plain">
                    <template v-if="(order.status || '').toUpperCase() === 'SHIPPING'">
                      ⏳ {{ formatWaitingTime(order.shippedAt || order.updatedAt) }}
                    </template>
                    <template v-else>Đang chuẩn bị</template>
                  </el-tag>
                  <span class="order-amount">{{ formatMoney(order.totalAmount) }}</span>
                  <el-icon :class="{ rotated: expandedOrder === orderKey(order) }">
                    <ArrowDown />
                  </el-icon>
                </div>
              </div>

              <!-- Expanded detail -->
              <div v-if="expandedOrder === orderKey(order)" class="order-detail">
                <el-divider style="margin: 10px 0" />
                <el-table v-if="orderItems[orderKey(order)]" :data="orderItems[orderKey(order)]" size="small" border>
                  <el-table-column label="Sản phẩm">
                    <template #default="{ row }">
                      <el-text tag="b" size="small">{{ row.productName || row.variant?.product?.name }}</el-text>
                      <div class="variant-text">{{ row.variantName || row.variant?.name }}</div>
                    </template>
                  </el-table-column>
                  <el-table-column label="SL" width="52" align="center" prop="quantity" />
                  <el-table-column label="Serial" min-width="200">
                    <template #default="{ row }">
                      <!-- Serial đã gán + badge X/Y -->
                      <div style="display:flex; flex-wrap:wrap; gap:4px; margin-bottom:4px; align-items:center;">
                        <el-tag v-for="sn in (row.serialNumbers || [])" :key="sn" size="small" type="success"
                          effect="plain" closable style="font-family:monospace;"
                          @close="removeSerial(order, row, sn)">{{ sn }}</el-tag>
                        <el-text size="small"
                          :type="(row.serialNumbers?.length || 0) >= row.quantity ? 'success' : 'danger'"
                          style="font-weight:600;">
                          {{ row.serialNumbers?.length || 0 }}/{{ row.quantity }}
                        </el-text>
                      </div>
                      <!-- Autocomplete gán mới (chỉ hiện khi chưa đủ serial) -->
                      <el-space v-if="(row.serialNumbers?.length || 0) < row.quantity" :size="4">
                        <el-select v-model="serialInputs[orderKey(order)][row.id]"
                          :placeholder="`Serial ${(row.serialNumbers?.length || 0) + 1}…`" size="small" style="width:190px"
                          filterable allow-create clearable default-first-option
                          :loading="loadingVariantSerial[row.variantId || row.variant?.id]"
                          @focus="loadVariantSerials(row.variantId || row.variant?.id)"
                          @keyup.enter.native="doAssignSerial(order, row)">
                          <el-option
                            v-for="opt in (variantSerials[row.variantId || row.variant?.id] ?? []).filter(o => !(row.serialNumbers || []).includes(o.value))"
                            :key="opt.value" :label="opt.label" :value="opt.value">
                            <span style="font-family:monospace;font-size:12px">{{ opt.value }}</span>
                          </el-option>
                          <template v-if="!(variantSerials[row.variantId || row.variant?.id]?.length)" #empty>
                            <div style="padding:8px 12px;font-size:12px;color:var(--el-text-color-secondary)">
                              Không có serial gợi ý – nhập tay rồi nhấn Gán
                            </div>
                          </template>
                        </el-select>
                        <el-button type="primary" size="small" :loading="serialLoading[orderKey(order) + '_' + row.id]"
                          @click="doAssignSerial(order, row)">Gán</el-button>
                      </el-space>
                    </template>
                  </el-table-column>
                  <el-table-column label="Thành tiền" width="130" align="right">
                    <template #default="{ row }">{{ formatMoney(row.lineTotal || (row.unitPrice * row.quantity))
                      }}</template>
                  </el-table-column>
                </el-table>
                <div v-else class="loading-items"><el-icon class="is-loading">
                    <Loading />
                  </el-icon> Đang tải...</div>

                <el-alert v-if="order.notes" :title="'Ghi chú: ' + order.notes" type="info" :closable="false"
                  class="mt-10" plain />

                <!-- ── BƯỚC 1: Chưa SHIPPING → Chuẩn bị + báo khách ── -->
                <template v-if="(order.status || '').toUpperCase() !== 'SHIPPING'">
                  <el-alert v-if="!isAllSerialAssigned(order)"
                    title="Vui lòng gán đủ serial cho tất cả sản phẩm trước khi báo khách đến lấy." type="warning"
                    :closable="false" show-icon class="mt-10" />
                  <div class="confirm-row mt-10">
                    <el-alert v-if="confirmError[orderKey(order)]" :title="confirmError[orderKey(order)]" type="error"
                      show-icon :closable="false" class="mb-10" />
                    <el-button type="primary" style="width:100%" :loading="confirmLoading[orderKey(order)]"
                      :disabled="!isAllSerialAssigned(order)" @click="markReady(order)">
                      <el-icon>
                        <Check />
                      </el-icon>
                      HÀNG SẴN SÀNG — BÁO KHÁCH ĐẾN LẤY
                    </el-button>
                  </div>
                </template>

                <!-- ── BƯỚC 2: Đã SHIPPING → Đợi khách đến, thu tiền & bàn giao ── -->
                <template v-else>
                  <el-alert title="✅ Đã thông báo khách — Đang chờ khách đến nhận hàng" type="success" :closable="false"
                    class="mt-10" />
                  <!-- Checkbox thu tiền mặt -->
                  <div v-if="needsCashCollection(order)" class="mt-10">
                    <el-checkbox v-model="cashCollected[orderKey(order)]" border size="large" style="width:100%;">
                      Xác nhận đã thu đủ <strong style="color:var(--el-color-danger);">{{ formatMoney(order.totalAmount)
                        }}</strong> tiền mặt
                    </el-checkbox>
                  </div>
                  <div class="confirm-row mt-10">
                    <el-alert v-if="confirmError[orderKey(order)]" :title="confirmError[orderKey(order)]" type="error"
                      show-icon :closable="false" class="mb-10" />
                    <el-button type="success" style="width:100%" :loading="confirmLoading[orderKey(order)]"
                      @click="confirmDelivery(order)">
                      <el-icon>
                        <Check />
                      </el-icon>
                      XÁC NHẬN GIAO HÀNG{{ needsCashCollection(order) ? ' + THU ' + formatMoney(order.totalAmount) : ''
                      }}
                    </el-button>
                  </div>
                </template>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="order-panel-empty">
          <el-icon :size="52" color="#dcdfe6">
            <User />
          </el-icon>
          <p>Chọn khách hàng từ danh sách để xem đơn hàng</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from "vue";
import { User, Phone, ArrowDown, Loading, Refresh, Check, Search, CircleCheck } from "@element-plus/icons-vue";
import { ordersApi } from "../../api/orders.api";
import { paymentsApi } from "../../api/payments";
import { productsApi } from "../../api/products.api";
import { toast } from "../../ui/toast";
import { confirmModal } from "../../ui/confirm";

// ── State ──
const loading = ref(false);
const errorMsg = ref("");
const allOrders = ref([]);
const filterText = ref("");
const selectedCustomerId = ref(null);
const expandedOrder = ref(null);
const orderItems = ref({});
const briefItems = ref({});
const confirmLoading = ref({});
const confirmError = ref({});
const cashCollected = reactive({});
const serialInputs = reactive({});   // { [orderKey]: { [itemId]: '' } }
const serialLoading = reactive({});   // { [orderKey+itemId]: bool }
const serialSuggestions = reactive({});  // kept for compat
const variantSerials = reactive({});  // { [variantId]: [{ value, label }] }
const loadingVariantSerial = reactive({});  // { [variantId]: bool }
const bulkLoading = ref(false);

// ── Auto-refresh ──
const REFRESH_INTERVAL = 30;
const countdown = ref(REFRESH_INTERVAL);
const autoRefreshActive = ref(false);
let countdownTimer = null;

function startAutoRefresh() {
  countdownTimer = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0) {
      countdown.value = REFRESH_INTERVAL;
      silentRefresh();
    }
  }, 1000);
}
function stopAutoRefresh() { clearInterval(countdownTimer); }
async function silentRefresh() {
  autoRefreshActive.value = true;
  try { await loadAllPickups(true); } finally { autoRefreshActive.value = false; }
}

// ── Load all pending pickup orders ──
async function loadAllPickups(silent = false) {
  if (!silent) loading.value = true;
  errorMsg.value = "";
  try {
    // Thử load tất cả đơn OFFLINE không lọc customer
    const res = await ordersApi.filter(null, null, null, null, null);
    const rawOrders = res.data?.data || res.data?.content || res.data || [];
    const pending = (Array.isArray(rawOrders) ? rawOrders : []).filter(isPendingPickup);
    allOrders.value = pending;
    await loadAllBriefs(pending);
  } catch (e) {
    errorMsg.value = "Lỗi tải danh sách đơn hàng.";
  } finally {
    if (!silent) loading.value = false;
  }
}

// ── Group orders by customer ──
const customerGroups = computed(() => {
  const map = {};
  for (const order of allOrders.value) {
    const cid = order.customerId || order.customer?.id || `u_${order.id}`;
    const cname = order.customerName || order.customer?.fullName || order.customer?.name || 'Khách vãng lai';
    const cphone = order.customerPhone || order.customer?.phone || '';
    if (!map[cid]) map[cid] = { customerId: cid, customerName: cname, customerPhone: cphone, orders: [] };
    map[cid].orders.push(order);
  }
  return Object.values(map).map(g => {
    // Chỉ tính SLA dựa trên đơn đã SHIPPING (đã báo khách) — đơn chưa shipping không tính gấp
    const shippingOrders = g.orders.filter(o => (o.status || '').toUpperCase() === 'SHIPPING');
    const maxMins = shippingOrders.length
      ? Math.max(...shippingOrders.map(o => minutesSince(o.shippedAt || o.updatedAt)))
      : 0;
    const earliestShipping = shippingOrders.length
      ? shippingOrders.reduce((min, o) => {
        const t = o.shippedAt || o.updatedAt;
        return !min || new Date(t) < new Date(min) ? t : min;
      }, null)
      : null;
    return {
      ...g,
      hasUrgent: maxMins > 1200, // trên 20 giờ là báo đỏ
      slaType: maxMins > 1200 ? 'danger' : maxMins > 720 ? 'warning' : 'success', // trên 12 giờ là vàng
      maxWaitTime: earliestShipping ? formatWaitingTime(earliestShipping) : '—'
    };
  }).sort((a, b) => (b.hasUrgent ? 1 : 0) - (a.hasUrgent ? 1 : 0));
});

const filteredGroups = computed(() => {
  const f = filterText.value.toLowerCase();
  if (!f) return customerGroups.value;
  return customerGroups.value.filter(g =>
    g.customerName.toLowerCase().includes(f) || g.customerPhone.includes(f)
  );
});

const totalPendingOrders = computed(() => allOrders.value.length);
const selectedGroup = computed(() => customerGroups.value.find(g => g.customerId === selectedCustomerId.value) || null);

function selectGroup(group) {
  selectedCustomerId.value = group.customerId;
  expandedOrder.value = null;
}

// ── Brief Product Summary ──
async function loadAllBriefs(orders) {
  await Promise.allSettled(orders.map(async (order) => {
    const key = orderKey(order);
    if (briefItems.value[key]) return;
    try {
      const oid = order.orderId || order.id;
      const res = await ordersApi.getOrderDetail(oid);
      const detail = res.data?.data || res.data;
      const items = detail?.orderItems || detail?.items || detail?.products || [];
      if (!items.length) return;
      const names = items.map(i => i.productName || i.variant?.product?.name).filter(Boolean);
      briefItems.value = { ...briefItems.value, [key]: names.slice(0, 2).join(', ') + (names.length > 2 ? ` +${names.length - 2}` : '') };
      orderItems.value = { ...orderItems.value, [key]: items };
    } catch { /* ignore */ }
  }));
}

// ── Expand + load serial suggestions ──
async function toggleExpand(order) {
  const key = orderKey(order);
  if (expandedOrder.value === key) { expandedOrder.value = null; return; }
  expandedOrder.value = key;
  // Đảm bảo serialInputs[key] tồn tại cho el-select v-model
  if (!serialInputs[key]) serialInputs[key] = {};
  if (!orderItems.value[key]) {
    try {
      const oid = order.orderId || order.id;
      const res = await ordersApi.getOrderDetail(oid);
      const detail = res.data?.data || res.data;
      const items = detail?.orderItems || detail?.items || detail?.products || [];
      orderItems.value = { ...orderItems.value, [key]: items };
    } catch { orderItems.value = { ...orderItems.value, [key]: [] }; }
  }
}

// Load serial IN_STOCK của variant khi focus vào ô chọn
async function loadVariantSerials(variantId) {
  if (!variantId || variantSerials[variantId] !== undefined) return;
  loadingVariantSerial[variantId] = true;
  try {
    const res = await productsApi.getSerials(variantId);
    const list = res.data?.data || res.data || [];
    variantSerials[variantId] = list
      .filter(s => s.status === 'IN_STOCK')
      .map(s => ({ value: s.serialNumber, label: s.serialNumber }));
  } catch {
    variantSerials[variantId] = [];
  } finally {
    loadingVariantSerial[variantId] = false;
  }
}

// ── Helpers: Serial ──
function isAllSerialAssigned(order) {
  const key = orderKey(order);
  const items = orderItems.value[key];
  if (!items?.length) return false;
  return items.every(item => (item.serialNumbers?.length || 0) >= (item.quantity || 1));
}

function getSerialSuggestions(itemId, variantId, keyword, assignedSerials) {
  const vid = variantId;
  const all = serialSuggestions[vid] || [];
  const assigned = new Set(assignedSerials || []);
  const q = (keyword || '').toUpperCase();
  return all
    .filter(s => !assigned.has(s.value) && (!q || s.value.includes(q)))
    .slice(0, 20);
}

async function onSerialSelect(order, item, suggestion) {
  // Khi chọn từ dropdown autocomplete → gán luôn
  const oKey = orderKey(order);
  const itemId = item.id || item.orderItemId;
  if (!serialInputs[oKey]) serialInputs[oKey] = {};
  serialInputs[oKey][itemId] = suggestion.value;
  await doAssignSerial(order, item);
}

// ── Bước 1: Chuẩn bị hàng → markAsShipping (báo khách) ──
async function markReady(order) {
  const key = orderKey(order);
  const oid = order.orderId || order.id;
  if (!isAllSerialAssigned(order)) {
    toast("Vui lòng gán đủ serial trước!", "warning"); return;
  }
  const ok = await confirmModal(
    `Xác nhận hàng đã sẵn sàng và gửi thông báo cho khách đến lấy?`,
    "Báo khách đến lấy hàng", "Xác nhận", false
  );
  if (!ok) return;
  confirmLoading.value = { ...confirmLoading.value, [key]: true };
  confirmError.value = { ...confirmError.value, [key]: "" };
  try {
    await ordersApi.markAsProcessing(oid).catch(() => { });
    await ordersApi.markAsShipping(oid);
    // Cập nhật status trong allOrders để UI chuyển sang Bước 2
    const idx = allOrders.value.findIndex(o => orderKey(o) === key);
    if (idx !== -1) allOrders.value[idx] = { ...allOrders.value[idx], status: 'SHIPPING' };
    toast(`✅ Đã báo khách đến lấy đơn ${order.orderNumber || oid}!`, "success");
  } catch (e) {
    const msg = e.response?.data?.message || "Cập nhật thất bại.";
    toast(msg, "error");
    confirmError.value = { ...confirmError.value, [key]: msg };
  } finally {
    confirmLoading.value = { ...confirmLoading.value, [key]: false };
  }
}

// ── Bước 2: Khách đến → Thu tiền + markAsDelivered ──
async function confirmDelivery(order) {
  const key = orderKey(order);
  if (needsCashCollection(order) && !cashCollected[key]) {
    toast("Vui lòng xác nhận đã thu tiền mặt!", "error"); return;
  }
  const ok = await confirmModal(
    needsCashCollection(order)
      ? `Xác nhận đã thu ${formatMoney(order.totalAmount)} và hoàn tất giao hàng?`
      : `Xác nhận hoàn tất giao hàng?`,
    "Xác nhận giao hàng", "Hoàn tất", false
  );
  if (!ok) return;
  const oid = order.orderId || order.id;
  confirmLoading.value = { ...confirmLoading.value, [key]: true };
  confirmError.value = { ...confirmError.value, [key]: "" };
  try {
    if (needsCashCollection(order)) {
      const method = (order.paymentMethod || "CASH").toUpperCase();
      await paymentsApi.create({ orderId: Number(oid), amount: order.totalAmount, method, paymentStatus: "PAID", transactionRef: `TXN-PICKUP-${Date.now()}` });
    }
    await ordersApi.markAsDelivered(oid);
    allOrders.value = allOrders.value.filter(o => orderKey(o) !== key);
    expandedOrder.value = null;
    toast(`✅ Đơn ${order.orderNumber || oid} đã giao thành công!`, "success");
  } catch (e) {
    const msg = e.response?.data?.message || "Xác nhận thất bại.";
    toast(msg, "error");
    confirmError.value = { ...confirmError.value, [key]: msg };
  } finally {
    confirmLoading.value = { ...confirmLoading.value, [key]: false };
  }
}


// ── Bulk Confirm cho nhóm khách đang chọn ──
async function confirmBulkPickupForGroup() {
  if (!selectedGroup.value) return;
  const orders = selectedGroup.value.orders;
  const ok = await confirmModal(`Hoàn tất tất cả ${orders.length} đơn của ${selectedGroup.value.customerName}?`, "Phê duyệt hàng loạt", "Xác nhận", false);
  if (!ok) return;
  bulkLoading.value = true;
  let success = 0;
  await Promise.allSettled(orders.map(async (order) => {
    const oid = order.orderId || order.id;
    const key = orderKey(order);
    try {
      const method = (order.paymentMethod || "").toUpperCase();
      const payStatus = (order.paymentStatus || "").toUpperCase();
      if (method === "CASH" && payStatus !== "PAID" && payStatus !== "COMPLETED") {
        await paymentsApi.create({ orderId: oid, amount: order.totalAmount, method: "CASH", paymentStatus: "PAID" }).catch(() => { });
      }
      await ordersApi.markAsProcessing(oid).catch(() => { });
      await ordersApi.markAsShipping(oid).catch(() => { });
      await ordersApi.markAsDelivered(oid);
      success++;
    } catch { /* individual error */ }
  }));
  const doneKeys = orders.map(orderKey);
  allOrders.value = allOrders.value.filter(o => !doneKeys.includes(orderKey(o)));
  selectedCustomerId.value = null;
  bulkLoading.value = false;
  toast(`✅ Hoàn tất ${success}/${orders.length} đơn!`, "success");
}

// ── Helpers ──
const orderKey = (o) => o.orderNumber || String(o.id);
const DONE_STATUSES = ["DELIVERED", "CANCELLED", "REFUNDED", "COMPLETED"];
function isPendingPickup(o) {
  return (o.channel || "").toUpperCase() === "OFFLINE" && !DONE_STATUSES.includes((o.status || "").toUpperCase());
}
function isOrderPaid(order) {
  const method = (order.paymentMethod || "").toUpperCase();
  const payStatus = (order.paymentStatus || "").toUpperCase();
  // Đơn CASH chưa thu tiền thì không được coi là đã TT
  if (method === "CASH") return payStatus === "PAID" || payStatus === "COMPLETED";
  return payStatus === "PAID" || payStatus === "COMPLETED" || (order.status || "").toUpperCase() === "PAID";
}
// Tính riêng đơn CASH offline chưa thu tiền mặt (cần checkbox)
function needsCashCollection(order) {
  const method = (order.paymentMethod || "").toUpperCase();
  const payStatus = (order.paymentStatus || "").toUpperCase();
  return method === "CASH" && payStatus !== "PAID" && payStatus !== "COMPLETED";
}
function getPaymentTagType(order) {
  if (needsCashCollection(order)) return "warning";
  return isOrderPaid(order) ? "success" : "danger";
}
function getPaymentTagLabel(order) {
  if (needsCashCollection(order)) return "Thu COD";
  return isOrderPaid(order) ? "ĐÃ TT" : "CHƯA TT";
}

// ── Serial: Gán & Xóa ──
async function doAssignSerial(order, item) {
  const oKey = orderKey(order);
  const itemId = item.id || item.orderItemId;
  const oid = order.orderId || order.id;
  const sn = (serialInputs[oKey]?.[itemId] || "").trim().toUpperCase();
  if (!sn) { toast("Vui lòng nhập Serial Number", "warning"); return; }

  const loadKey = `${oKey}_${itemId}`;
  serialLoading[loadKey] = true;
  try {
    await ordersApi.assignSerial(oid, itemId, sn);
    if (!item.serialNumbers) item.serialNumbers = [];
    if (!item.serialNumbers.includes(sn)) item.serialNumbers.push(sn);
    serialInputs[oKey][itemId] = "";
    toast(`Đã gán serial ${sn}`, "success");
  } catch (e) {
    toast(e.response?.data?.message || "Gán serial thất bại", "error");
  } finally {
    serialLoading[loadKey] = false;
  }
}

async function removeSerial(order, item, sn) {
  const oKey = orderKey(order);
  const itemId = item.id || item.orderItemId;
  const oid = order.orderId || order.id;
  const loadKey = `${oKey}_${itemId}_rm`;
  serialLoading[loadKey] = true;
  try {
    await ordersApi.removeSerial(oid, itemId, sn);
    if (item.serialNumbers) item.serialNumbers = item.serialNumbers.filter(s => s !== sn);
    toast(`Đã xóa serial ${sn}`, "success");
  } catch (e) {
    toast(e.response?.data?.message || "Xóa serial thất bại", "error");
  } finally {
    serialLoading[loadKey] = false;
  }
}
function minutesSince(d) { return d ? Math.floor((Date.now() - new Date(d).getTime()) / 60000) : 0; }
function formatWaitingTime(d) {
  if (!d) return "—";
  const m = minutesSince(d);
  if (m >= 1440) return `${Math.floor(m / 1440)} ngày ${Math.floor((m % 1440) / 60)}h`;
  return m < 60 ? `${m} phút` : `${Math.floor(m / 60)}h${m % 60}p`;
}
function getSlaType(order) {
  if ((order.status || '').toUpperCase() !== 'SHIPPING') return 'info';
  const m = minutesSince(order.shippedAt || order.updatedAt);
  // Cảnh báo vàng sau 12h (720p), đỏ sau 20h (1200p)
  return m > 1200 ? "danger" : m > 720 ? "warning" : "success";
}
function formatMoney(v) { return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(v || 0); }
function initials(n = "") { return (n || "").split(" ").map(w => w[0]).filter(Boolean).slice(0, 2).join("").toUpperCase() || "KH"; }

onMounted(() => { loadAllPickups(false); startAutoRefresh(); });
onUnmounted(() => stopAutoRefresh());
</script>

<style scoped>
/* ── Layout ── */
.pickup-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: #f0f2f5;
  overflow: hidden;
}

.pickup-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  flex-shrink: 0;
}

.header-title {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a2e;
}

.header-sub {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pickup-body {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

/* ── Queue panel (left) ── */
.queue-panel {
  width: 300px;
  flex-shrink: 0;
  background: #fff;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.queue-search {
  padding: 12px;
  border-bottom: 1px solid #f5f7fa;
}

.queue-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 20px 16px;
  color: #909399;
  font-size: 13px;
}

.queue-list {
  flex: 1;
  overflow-y: auto;
}

.queue-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  cursor: pointer;
  border-bottom: 1px solid #f5f7fa;
  transition: background 0.12s;
}

.queue-item:hover {
  background: #f5f7fa;
}

.queue-item.is-active {
  background: #ecf5ff;
  border-left: 3px solid #409eff;
}

.queue-item.has-urgent {
  border-left: 3px solid #f56c6c;
}

.queue-item.has-urgent.is-active {
  background: #fef0f0;
}

.queue-item-info {
  flex: 1;
  min-width: 0;
}

.queue-item-name {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.queue-item-phone {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

.queue-item-badge {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.queue-sla {
  font-size: 10px;
  font-weight: 600;
}

.sla-danger {
  color: #f56c6c;
}

.sla-warning {
  color: #e6a23c;
}

.sla-success {
  color: #67c23a;
}

/* ── Order panel (right) ── */
.order-panel {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.order-panel-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.order-panel-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 14px;
  gap: 12px;
}

.customer-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  flex-shrink: 0;
}

.customer-header-info {
  flex: 1;
  min-width: 0;
}

.customer-header-name {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a2e;
}

.customer-header-sub {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 4px;
  font-size: 12px;
  color: #606266;
}

.orders-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* ── Order card ── */
.order-card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  overflow: hidden;
  transition: border-color 0.15s;
}

.order-card.is-expanded {
  border-color: #409eff;
}

.order-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  cursor: pointer;
}

.order-summary:hover {
  background: #fafafa;
}

.order-summary-left {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.order-summary-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.order-number {
  font-family: monospace;
  font-weight: 700;
  font-size: 13px;
  color: #303133;
}

.order-brief {
  font-size: 11px;
  color: #909399;
}

.order-amount {
  font-size: 13px;
  font-weight: 700;
  color: #303133;
  white-space: nowrap;
}

.order-detail {
  padding: 0 14px 14px;
}

.variant-text {
  font-size: 11px;
  color: #909399;
}

.loading-items {
  padding: 15px;
  text-align: center;
  color: #909399;
}

.confirm-row {
  margin-top: 10px;
}

.rotated {
  transform: rotate(180deg);
  transition: transform 0.2s;
}

.mt-10 {
  margin-top: 10px;
}

.mt-30 {
  margin-top: 30px;
}

.mb-10 {
  margin-bottom: 10px;
}

.mx-12 {
  margin-left: 12px;
  margin-right: 12px;
}

.mono {
  font-family: monospace;
}
</style>
