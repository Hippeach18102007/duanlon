<template>
  <div v-if="order" class="order-detail-container">
    <!-- PRINT ONLY SECTION -->
    <div class="print-section">
      <div class="print-header">
        <h1>PHIẾU GIAO HÀNG</h1>
        <p>Tech Store</p>
      </div>
      <div class="print-info">
        <div class="print-info-left">
          <strong>Thông tin khách hàng:</strong><br>
          {{ order.customerName }}<br>
          {{ order.customerPhone }}<br>
          {{ order.customerAddress }}
        </div>
        <div class="print-info-right">
          <strong>Mã đơn hàng:</strong> #{{ order.orderNumber }}<br>
          <strong>Ngày tạo:</strong> {{ formatDateTime(order.createdAt) }}<br>
          <strong>Trạng thái:</strong> {{ statusLabel(order.status) }}
          
          <div v-if="['PROCESSING', 'SHIPPING'].includes(order.status)" style="margin-top: 15px;">
            <img :src="deliveryQrUrl" style="width: 100px; height: 100px; border: 1px solid #ccc; padding: 5px;" alt="QR Code" />
            <div style="font-size: 11px; margin-top: 4px; font-weight: bold;">Mã QR Dành Cho Shipper</div>
          </div>
        </div>
      </div>
      <table class="print-table">
        <thead>
          <tr>
            <th>STT</th>
            <th>Tên sản phẩm</th>
            <th style="text-align:center">Serial</th>
            <th style="text-align:center">SKU</th>
            <th style="text-align:right">Đơn giá</th>
            <th style="text-align:right">Thành tiền</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, index) in tableRows" :key="index">
            <td style="text-align:center">{{ index + 1 }}</td>
            <td>{{ row.productName }}<br><small>{{ row.variantName }}</small></td>
            <td style="text-align:center;font-family:monospace">{{ row.serial || '—' }}</td>
            <td style="text-align:center">{{ row.sku || '-' }}</td>
            <td style="text-align:right">{{ formatVND(row.unitPrice) }}</td>
            <td style="text-align:right">{{ row.isFirst ? formatVND(row.lineTotal) : '' }}</td>
          </tr>
        </tbody>
      </table>
      <div class="print-summary">
        <div class="print-summary-row">
          <span>Tạm tính:</span><span>{{ formatVND(order.subtotal) }}</span>
        </div>
        <div class="print-summary-row" v-if="order.discountTotal > 0">
          <span>Giảm giá:</span><span>-{{ formatVND(order.discountTotal) }}</span>
        </div>
        <div class="print-summary-row">
          <span>Phí vận chuyển:</span>
          <span>{{ order.shippingFee > 0 ? formatVND(order.shippingFee) : 'Miễn phí' }}</span>
        </div>
        <div class="print-summary-row print-total">
          <span>Tổng cộng:</span><span>{{ formatVND(order.totalAmount) }}</span>
        </div>
      </div>
      <div class="print-footer">
        Cảm ơn quý khách đã mua sắm tại Tech Store!<br>
        Mọi thắc mắc xin vui lòng liên hệ hotline hỗ trợ.
      </div>
    </div>

    <!-- MAIN UI APP CONTENT -->
    <el-space direction="vertical" alignment="stretch" :size="20" style="width: 100%" class="no-print">
      <!-- BACK LINK -->
      <el-button link @click="router.back()">
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none" class="icon-spacing">
          <path d="M9 2L4 7l5 5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        Quay lại danh sách
      </el-button>

      <!-- PAGE HEADER -->
      <el-row type="flex" justify="space-between" align="bottom">
        <el-col :span="16">
          <el-space direction="vertical" alignment="start" :size="4">
            <el-text type="info" size="small">Chi tiết đơn hàng</el-text>
            <el-space :size="12">
              <h2>#{{ order.orderNumber }}</h2>
              <el-tag :type="statusType(order.status)" effect="light" round>
                {{ statusLabel(order.status) }}
              </el-tag>
            </el-space>
            <el-text type="info">Kho hàng · Quản lý đơn hàng</el-text>
          </el-space>
        </el-col>

        <!-- INVENTORY ACTIONS -->
        <el-col :span="8" style="text-align: right;">
          <el-space>
            <!-- Tạo mã QR cho Shipper -->
            <el-button v-if="['SHIPPING'].includes(order.status)" type="primary" plain @click="showQrDialog = true">
              <el-icon class="icon-spacing"><FullScreen /></el-icon>
              QR Giao Hàng
            </el-button>

            <el-button @click="printOrder" plain>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" class="icon-spacing">
                <path d="M6 9V2h12v7M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M6 14h12v8H6z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              In phiếu
            </el-button>

            <el-dropdown
              v-if="order.status === 'PAID' || (order.status === 'PENDING' && order.paymentMethod === 'CASH' && order.channel === 'ONLINE')"
              @command="cancelOrder"
              trigger="click"
            >
              <el-button type="danger" plain :loading="cancelLoading">
                Hủy đơn <svg width="14" height="14" viewBox="0 0 24 24" fill="none" style="margin-left: 4px;">
                  <path d="M6 9l6 6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="Hàng hiện đã hết">Hàng hiện đã hết</el-dropdown-item>
                  <el-dropdown-item command="Hàng hiện đang bị lỗi">Hàng hiện đang bị lỗi</el-dropdown-item>
                  <el-dropdown-item command="Lý do khác" divided>Lý do khác...</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>

            <el-tooltip
              v-if="order.status === 'PAID' || (order.status === 'PENDING' && order.paymentMethod === 'CASH' && order.channel === 'ONLINE')"
              :content="allSerialsAssigned ? '' : `Cần gán đủ serial (${totalAssigned}/${totalRequired})`"
              placement="top"
              :disabled="allSerialsAssigned"
            >
              <span>
                <el-button
                  type="warning"
                  plain
                  :loading="loading"
                  :disabled="!allSerialsAssigned"
                  @click="markProcessing"
                >
                  Đóng gói
                </el-button>
              </span>
            </el-tooltip>

            <el-button
              v-if="order.status === 'PROCESSING'"
              type="primary"
              plain
              :loading="loading"
              @click="markShipping"
            >
              Chuyển giao vận
            </el-button>
          </el-space>
        </el-col>
      </el-row>

      <!-- CONTENT GRID -->
      <el-row :gutter="20">
        <!-- LEFT COLUMN: ORDER ITEMS TABLE -->
        <el-col :span="16">
          <el-card shadow="never">
            <template #header>
              <el-row justify="space-between" align="middle">
                <el-space>
                  <el-text tag="b">Sản phẩm đặt mua</el-text>
                  <el-text type="info" size="small">({{ order.items?.length || 0 }} sản phẩm)</el-text>
                </el-space>
                <el-tag
                  v-if="order.status === 'PAID' || (order.status === 'PENDING' && order.paymentMethod === 'CASH' && order.channel === 'ONLINE')"
                  :type="allSerialsAssigned ? 'success' : 'warning'"
                  effect="light"
                  round
                  size="small"
                >
                  {{ totalAssigned }}/{{ totalRequired }} serial đã gán
                </el-tag>
              </el-row>
            </template>

            <!-- Bảng: mỗi item = 1 dòng, hiện tất cả serial trong 1 ô -->
            <el-table :data="tableRows.filter(r => r.isFirst)" style="width:100%" :row-class-name="rowClass">
              <el-table-column label="Sản phẩm" min-width="180">
                <template #default="{ row }">
                  <el-space direction="vertical" alignment="start" :size="0">
                    <el-text tag="b">{{ row.productName }}</el-text>
                    <el-text type="info" size="small"
                      v-if="row.variantName && row.variantName !== row.productName">
                      {{ row.variantName }}
                    </el-text>
                    <el-text type="info" size="small" v-if="row.sku">SKU: {{ row.sku }}</el-text>
                  </el-space>
                </template>
              </el-table-column>

              <el-table-column label="SL" width="60" align="center">
                <template #default="{ row }">
                  <el-text tag="b">{{ row.quantity }}</el-text>
                </template>
              </el-table-column>

              <el-table-column label="Serial Number" min-width="270">
                <template #default="{ row }">
                  <!-- Serial đã gán + badge X/Y -->
                  <div style="display:flex; flex-wrap:wrap; gap:4px; margin-bottom:4px; align-items:center;">
                    <el-tag
                      v-for="sn in getItemSerials(row)"
                      :key="sn"
                      type="success" effect="light"
                      :closable="order.status === 'PAID' || order.status === 'PENDING' || order.status === 'PROCESSING'"
                      style="font-family:monospace;font-size:12px"
                      @close="handleRemoveSerialByItem(row, sn)"
                    >{{ sn }}</el-tag>
                    <el-text
                      size="small"
                      :type="getItemSerials(row).length >= row.quantity ? 'success' : 'danger'"
                      style="font-weight:600;"
                    >{{ getItemSerials(row).length }}/{{ row.quantity }}</el-text>
                  </div>
                  <!-- Select thêm serial mới (khi chưa đủ) -->
                  <el-space
                    v-if="(order.status === 'PAID' || order.status === 'PENDING' || order.status === 'PROCESSING') && getItemSerials(row).length < row.quantity"
                    :size="4"
                  >
                    <el-select
                      v-model="serialInputs[row.slotKey]"
                      :placeholder="`Serial ${getItemSerials(row).length + 1}…`"
                      size="small"
                      style="width:200px"
                      filterable
                      allow-create
                      clearable
                      default-first-option
                      :loading="loadingVariantSerial[row.variantId]"
                      @focus="loadVariantSerials(row.variantId)"
                      @keyup.enter.native="handleAssignSerial(row)"
                      :ref="el => { if (el) serialInputRefs[row.slotKey] = el }"
                    >
                      <template #prefix>
                        <el-icon style="color:var(--el-color-primary)"><CreditCard /></el-icon>
                      </template>
                      <el-option
                        v-for="opt in (variantSerials[row.variantId] ?? []).filter(o => !getItemSerials(row).includes(o.value))"
                        :key="opt.value"
                        :label="opt.label"
                        :value="opt.value"
                      >
                        <span style="font-family:monospace;font-size:12px">{{ opt.value }}</span>
                      </el-option>
                      <template v-if="!(variantSerials[row.variantId]?.length)" #empty>
                        <div style="padding:8px 12px;font-size:12px;color:var(--el-text-color-secondary)">
                          Không có serial gợi ý – nhập tay rồi nhấn Gán
                        </div>
                      </template>
                    </el-select>
                    <el-button
                      type="primary"
                      size="small"
                      :loading="assigningSlot === row.slotKey"
                      @click="handleAssignSerial(row)"
                    >Gán</el-button>
                  </el-space>
                </template>
              </el-table-column>

              <el-table-column label="Đơn giá" width="130" align="right">
                <template #default="{ row }">
                  <el-text>{{ formatVND(row.unitPrice) }}</el-text>
                </template>
              </el-table-column>

              <el-table-column label="Thành tiền" width="140" align="right">
                <template #default="{ row }">
                  <el-text tag="b" v-if="row.isFirst">{{ formatVND(row.unitPrice) }}</el-text>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>

        <!-- RIGHT COLUMN: SERIAL PROGRESS -->
        <el-col :span="8">
          <el-card shadow="never">
            <template #header><el-text tag="b">Tiến độ gán serial</el-text></template>
            <el-space direction="vertical" alignment="stretch" :size="16" style="width:100%">
              <el-progress
                :percentage="totalRequired ? Math.round((totalAssigned / totalRequired) * 100) : 0"
                :status="allSerialsAssigned ? 'success' : ''"
                :stroke-width="10"
              />
              <el-row justify="space-between">
                <el-text type="info">Đã gán</el-text>
                <el-text tag="b">{{ totalAssigned }} / {{ totalRequired }}</el-text>
              </el-row>
              <el-row justify="space-between">
                <el-text type="info">Còn lại</el-text>
                <el-text :type="allSerialsAssigned ? 'success' : 'warning'" tag="b">
                  {{ totalRequired - totalAssigned }} serial
                </el-text>
              </el-row>
            </el-space>
          </el-card>
        </el-col>
      </el-row>
    </el-space>

    <!-- Dialog lấy mã QR cho Shipper -->
    <el-dialog v-model="showQrDialog" title="Mã QR Giao Hàng (Dành cho Shipper)" width="400px" align-center class="no-print">
      <div style="text-align: center; padding: 10px 0;">
        <el-image
          :src="deliveryQrUrl"
          style="width: 200px; height: 200px; border: 1px solid var(--el-border-color-lighter); border-radius: 8px; padding: 10px;"
        >
          <template #error>
            <div class="image-slot">
              <el-icon><Picture /></el-icon>
            </div>
          </template>
        </el-image>
        <div style="margin-top: 16px;">
          <el-text type="info" size="small">
            In mã này và dán lên gói hàng. <br/>
            Shipper quét mã để tải ảnh bằng chứng giao hàng.
          </el-text>
        </div>
        <div style="margin-top: 12px;">
          <el-input :value="deliveryLink" readonly size="small">
            <template #append>
              <el-button icon="CopyDocument" @click="copyDeliveryLink" />
            </template>
          </el-input>
        </div>
      </div>
      <template #footer>
        <el-button @click="showQrDialog = false">Đóng</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { CreditCard, FullScreen, Picture, CopyDocument } from "@element-plus/icons-vue";
import { useRoute, useRouter } from "vue-router";
import { ordersApi } from "../../api/orders.api";
import { paymentsApi } from "../../api/payments";
import { productsApi } from "../../api/products.api";
import { ElMessage, ElMessageBox } from "element-plus";
import { formatVND, formatDateTime } from "../../utils/format";

const route   = useRoute();
const router  = useRouter();
const order   = ref(null);
const loading = ref(false);

const serialInputs    = ref({});
const serialInputRefs = ref({});
const assigningSlot   = ref(null);

// ── QR Code Logic ──────────────────────────────────────────────
const showQrDialog = ref(false);

const deliveryLink = computed(() => {
  if (!order.value?.orderId) return '';
  return `${window.location.origin}/delivery-confirm/${order.value.orderId}`;
});

const deliveryQrUrl = computed(() => {
  if (!deliveryLink.value) return '';
  return `https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${encodeURIComponent(deliveryLink.value)}`;
});

async function copyDeliveryLink() {
  try {
    await navigator.clipboard.writeText(deliveryLink.value);
    ElMessage.success('Đã copy link');
  } catch (err) {
    ElMessage.error('Không thể copy');
  }
}

// ── Serial suggestions per variant ───────────────────────────
const variantSerials      = ref({});   // { [variantId]: [{ value, label }] }
const loadingVariantSerial = ref({});  // { [variantId]: boolean }

async function loadVariantSerials(variantId) {
  if (!variantId || variantSerials.value[variantId] !== undefined) return;
  loadingVariantSerial.value[variantId] = true;
  try {
    const res = await productsApi.getSerials(variantId);
    const list = res.data?.data ?? res.data ?? [];
    // chỉ lấy serial còn IN_STOCK
    variantSerials.value[variantId] = list
      .filter(s => s.status === 'IN_STOCK')
      .map(s => ({ value: s.serialNumber, label: s.serialNumber }));
  } catch (e) {
    console.error('Không thể tải serial gợi ý', e);
    variantSerials.value[variantId] = [];
  } finally {
    loadingVariantSerial.value[variantId] = false;
  }
}

const tableRows = computed(() => {
  if (!order.value?.items) return []
  const rows = []
  for (const item of order.value.items) {
    for (let i = 0; i < item.quantity; i++) {
      rows.push({
        itemId:      item.id,
        variantId:   item.variantId,
        slotKey:     `${item.id}_${i}`,
        slotIndex:   i,
        isFirst:     i === 0,
        quantity:    item.quantity,
        productName: item.productName,
        variantName: item.variantName,
        sku:         item.sku,
        unitPrice:   item.unitPrice,
        lineTotal:   item.lineTotal,
        serial:      item.serialNumbers?.[i] ?? null,
      })
    }
  }
  return rows
})

const allSerialsAssigned = computed(() => {
  if (!order.value?.items?.length) return false
  if (order.value.channel === 'OFFLINE') return true
  return order.value.items.every(
    item => (item.serialNumbers?.length || 0) >= item.quantity
  )
})

const totalAssigned = computed(() =>
  order.value?.items?.reduce((s, i) => s + (i.serialNumbers?.length || 0), 0) ?? 0
)

const totalRequired = computed(() =>
  order.value?.items?.reduce((s, i) => s + i.quantity, 0) ?? 0
)

const rowClass = ({ row }) => row.isFirst ? '' : 'row-continuation'

const statusLabel = (s) => ({
  PAID: "Đã thanh toán", PROCESSING: "Đang xử lý",
  SHIPPING: "Đang giao", DELIVERED: "Đã giao", DONE: "Hoàn thành",
}[s] ?? s)

const statusType = (s) => ({
  PAID: "success", PROCESSING: "warning",
  SHIPPING: "primary", DELIVERED: "info", DONE: "info",
}[s] ?? "info")

const printOrder = () => window.print()

const load = async () => {
  try {
    const res = await ordersApi.getById(route.params.orderId)
    order.value = res.data
  } catch (e) {
    ElMessage.error(e.response?.data?.message || "Không thể tải đơn hàng")
  }
}

const handleAssignSerial = async (row) => {
  const sn = serialInputs.value[row.slotKey]?.trim()
  if (!sn) { ElMessage.warning("Vui lòng nhập serial number"); return }
  try {
    assigningSlot.value = row.slotKey
    await ordersApi.assignSerial(order.value.orderId, row.itemId, sn)
    ElMessage.success(`Đã gán serial: ${sn}`)
    serialInputs.value[row.slotKey] = ""
    await load()
    const next = tableRows.value.find(
      r => r.itemId === row.itemId && r.slotIndex === row.slotIndex + 1 && !r.serial
    )
    if (next) setTimeout(() => serialInputRefs.value[next.slotKey]?.focus(), 120)
  } catch (e) {
    ElMessage.error(e.response?.data?.message || "Serial không hợp lệ hoặc đã dùng")
  } finally {
    assigningSlot.value = null
  }
}

const handleRemoveSerial = async (row) => {
  try {
    await ElMessageBox.confirm(`Bỏ gán serial "${row.serial}"?`, "Xác nhận", {
      type: "warning", confirmButtonText: "Bỏ gán", cancelButtonText: "Hủy"
    })
    await ordersApi.removeSerial(order.value.orderId, row.itemId, row.serial)
    ElMessage.success("Đã bỏ gán serial")
    await load()
  } catch (e) {
    if (e !== "cancel") ElMessage.error(e.response?.data?.message || "Không thể xóa serial")
  }
}

// Lấy danh sách serial đã gán của item (dùng cho item-based view)
function getItemSerials(row) {
  const item = order.value?.items?.find(i => i.id === row.itemId);
  return item?.serialNumbers || [];
}

// Xóa serial theo item + serial number
const handleRemoveSerialByItem = async (row, sn) => {
  try {
    await ElMessageBox.confirm(`Bỏ gán serial "${sn}"?`, "Xác nhận", {
      type: "warning", confirmButtonText: "Bỏ gán", cancelButtonText: "Hủy"
    })
    await ordersApi.removeSerial(order.value.orderId, row.itemId, sn)
    ElMessage.success("Đã bỏ gán serial")
    await load()
  } catch (e) {
    if (e !== "cancel") ElMessage.error(e.response?.data?.message || "Không thể xóa serial")
  }
}

const markProcessing = async () => {
  if (!allSerialsAssigned.value) {
    ElMessage.warning(`Cần gán đủ serial (${totalAssigned.value}/${totalRequired.value})`)
    return
  }
  try {
    await ElMessageBox.confirm(
      'Bạn có chắc chắn muốn bắt đầu xử lý đơn hàng này?',
      'Xác nhận xử lý',
      { confirmButtonText: 'Đồng ý', cancelButtonText: 'Hủy', type: 'warning' }
    );
    loading.value = true;

    if (order.value.status === 'PENDING' && order.value.paymentMethod === 'CASH') {
      await paymentsApi.create({
        orderId: order.value.orderId,
        method: 'CASH',
        transactionRef: 'COD-DETAIL-CONFIRM'
      });
    }

    await ordersApi.markAsProcessing(order.value.orderId);
    ElMessage.success("Đã chuyển đơn hàng sang trạng thái Đang xử lý");
    await load();
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || "Có lỗi xảy ra khi cập nhật trạng thái");
    }
  } finally {
    loading.value = false
  }
}

const markShipping = async () => {
  try {
    await ElMessageBox.confirm("Chuyển đơn cho đơn vị vận chuyển?", "Xác nhận giao vận", {
      confirmButtonText: "Đồng ý", cancelButtonText: "Hủy", type: "primary"
    })
    loading.value = true
    await ordersApi.markAsShipping(order.value.orderId)
    ElMessage.success("Đã chuyển sang Đang giao hàng")
    await load()
  } catch (e) {
    if (e !== "cancel") ElMessage.error(e.response?.data?.message || "Có lỗi xảy ra")
  } finally {
    loading.value = false
  }
}

const cancelLoading = ref(false);
const cancelOrder = async (reason) => {
  let finalReason = reason;
  if (reason === 'Lý do khác') {
    try {
      const { value } = await ElMessageBox.prompt('Nhập lý do hủy đơn:', 'Hủy đơn hàng', {
        confirmButtonText: 'Đồng ý',
        cancelButtonText: 'Đóng',
        inputValidator: (val) => {
          if (!val || val.trim().length === 0) return 'Vui lòng nhập lý do';
          return true;
        }
      });
      finalReason = value.trim();
    } catch {
      return;
    }
  } else {
    try {
      await ElMessageBox.confirm(
        `Xác nhận hủy đơn hàng này với lý do: "${finalReason}"?`,
        'Hủy đơn hàng',
        { confirmButtonText: 'Hủy đơn ngay', cancelButtonText: 'Đóng', type: 'danger' }
      );
    } catch {
      return;
    }
  }

  cancelLoading.value = true;
  try {
    await ordersApi.cancel(order.value.orderId, finalReason);
    ElMessage.success("Đã hủy đơn hàng thành công");
    await load();
  } catch (error) {
    ElMessage.error(error.response?.data?.message || "Có lỗi xảy ra khi hủy đơn");
  } finally {
    cancelLoading.value = false;
  }
}

onMounted(load)
</script>

<style scoped>
h2 { margin: 0; }
.icon-spacing { margin-right: 6px; }

:deep(.row-continuation td) {
  border-top: none !important;
  padding-top: 2px !important;
}

@media screen { .print-section { display: none; } }

@media print {
  .no-print { display: none !important; }
  @page { size: A4; margin: 0; }
  .print-section { display: block; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; color: #000; padding: 15mm; }
  .print-header { text-align: center; margin-bottom: 30px; border-bottom: 2px solid #000; padding-bottom: 15px; }
  .print-header h1 { font-size: 24px; margin: 0 0 5px 0; text-transform: uppercase; }
  .print-info { display: flex; justify-content: space-between; margin-bottom: 30px; line-height: 1.5; }
  .print-table { width: 100%; border-collapse: collapse; margin-bottom: 30px; }
  .print-table th, .print-table td { padding: 10px; text-align: left; }
  .print-table th { background-color: #f9f9f9 !important; -webkit-print-color-adjust: exact; print-color-adjust: exact; border: 1px solid #000; }
  .print-table td { border-bottom: 1px dashed #ccc; }
  .print-summary { width: 300px; float: right; margin-bottom: 30px; }
  .print-summary-row { display: flex; justify-content: space-between; margin-bottom: 8px; }
  .print-total { font-weight: bold; font-size: 1.2em; border-top: 1px solid #000; padding-top: 8px; margin-top: 8px; }
  .print-footer { clear: both; text-align: center; margin-top: 50px; font-style: italic; }
}
</style>

<style>
@media print {
  .el-aside, .el-header, .el-footer { display: none !important; }
  .el-main { margin-left: 0 !important; padding: 0 !important; overflow: visible !important; height: auto !important; }
  .el-container { height: auto !important; overflow: visible !important; }
  body, html, #app { height: auto !important; overflow: visible !important; background: white !important; }
}
</style>