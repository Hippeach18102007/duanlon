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

            <!-- Bảng: mỗi slot serial = 1 dòng -->
            <el-table :data="tableRows" style="width:100%" :row-class-name="rowClass">
              <el-table-column label="Sản phẩm" min-width="180">
                <template #default="{ row }">
                  <template v-if="row.isFirst">
                    <el-space direction="vertical" alignment="start" :size="0">
                      <el-text tag="b">{{ row.productName }}</el-text>
                      <el-text type="info" size="small"
                        v-if="row.variantName && row.variantName !== row.productName">
                        {{ row.variantName }}
                      </el-text>
                      <el-text type="info" size="small" v-if="row.sku">SKU: {{ row.sku }}</el-text>
                    </el-space>
                  </template>
                </template>
              </el-table-column>

              <el-table-column label="Serial Number" width="270">
                <template #default="{ row }">
                  <el-space v-if="row.serial" :size="6">
                    <el-tag
                      type="success"
                      effect="light"
                      :closable="order.status === 'PAID' || order.status === 'PENDING'"
                      @close="handleRemoveSerial(row)"
                      style="font-family:monospace;font-size:13px"
                    >
                      {{ row.serial }}
                    </el-tag>
                  </el-space>

                  <el-space v-else-if="order.status === 'PAID' || (order.status === 'PENDING' && order.paymentMethod === 'CASH')" :size="4">
                    <el-input
                      v-model="serialInputs[row.slotKey]"
                      :placeholder="`Serial ${row.slotIndex + 1}...`"
                      size="small"
                      style="width:160px"
                      clearable
                      @keyup.enter="handleAssignSerial(row)"
                      :ref="el => { if (el) serialInputRefs[row.slotKey] = el }"
                    />
                    <el-button
                      type="primary"
                      size="small"
                      :loading="assigningSlot === row.slotKey"
                      @click="handleAssignSerial(row)"
                    >
                      Gán
                    </el-button>
                  </el-space>

                  <el-text v-else type="info" size="small">—</el-text>
                </template>
              </el-table-column>

              <el-table-column label="Đơn giá" width="130" align="right">
                <template #default="{ row }">
                  <el-text v-if="row.isFirst">{{ formatVND(row.unitPrice) }}</el-text>
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ordersApi } from "../../api/orders.api";
import { paymentsApi } from "../../api/payments";
import { ElMessage, ElMessageBox } from "element-plus";
import { formatVND, formatDateTime } from "../../utils/format";

const route   = useRoute();
const router  = useRouter();
const order   = ref(null);
const loading = ref(false);

const serialInputs    = ref({});
const serialInputRefs = ref({});
const assigningSlot   = ref(null);

const tableRows = computed(() => {
  if (!order.value?.items) return []
  const rows = []
  for (const item of order.value.items) {
    for (let i = 0; i < item.quantity; i++) {
      rows.push({
        itemId:      item.id,
        slotKey:     `${item.id}_${i}`,
        slotIndex:   i,
        isFirst:     i === 0,
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