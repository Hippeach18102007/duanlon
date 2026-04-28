<template>
  <div style="max-width: 1280px; margin: 0 auto; padding: 24px;">
    <el-card shadow="never">

      <!-- Header -->
      <el-row align="middle" justify="space-between" style="margin-bottom: 20px; flex-wrap: wrap; gap: 12px;">
        <el-space direction="vertical" :size="4">
          <el-text size="small" type="info" style="text-transform: uppercase; letter-spacing: 0.08em; font-weight: 800;">Khách hàng</el-text>
          <el-text tag="div" style="font-size: 22px; font-weight: 900; color: #0f172a; line-height: 1.2;">Đơn hàng của tôi</el-text>
        </el-space>
        <el-button :loading="loading" plain @click="loadOrders">
          <el-icon><Refresh /></el-icon> Tải lại
        </el-button>
      </el-row>

      <el-divider style="margin: 0 0 20px;" />

      <el-alert v-if="error" type="error" :title="error" show-icon style="margin-bottom: 20px;" />

      <!-- Stats row -->
      <el-row :gutter="10" style="margin-bottom: 24px; flex-wrap: wrap;">
        <el-col :xs="12" :sm="4" v-for="stat in stats" :key="stat.label">
          <el-card
            shadow="never"
            :body-style="{ padding: '12px 16px', textAlign: 'center' }"
            :style="`background: ${stat.bg}; border-color: ${stat.border}; margin-bottom: 10px;`"
          >
            <el-text :style="`font-size: 22px; font-weight: 900; color: ${stat.color}; display: block; line-height: 1.1;`">
              {{ stat.value }}
            </el-text>
            <el-text size="small" :style="`color: ${stat.subColor}; text-transform: uppercase; letter-spacing: 0.04em; font-weight: 600; margin-top: 3px; display: block;`">
              {{ stat.label }}
            </el-text>
          </el-card>
        </el-col>
      </el-row>

      <!-- Table -->
      <el-skeleton v-if="loading" :rows="6" animated />

      <el-empty
        v-else-if="!orders.length"
        description="Bạn chưa có đơn hàng nào"
        :image-size="80"
      />

      <el-table
        v-else
        :data="orders"
        border
        stripe
        style="width: 100%"
        :header-cell-style="{ background: 'var(--el-fill-color-light)', color: 'var(--el-text-color-secondary)', fontSize: '11px', fontWeight: '700', textTransform: 'uppercase', letterSpacing: '0.05em' }"
      >
        <el-table-column label="Mã đơn" min-width="170">
          <template #default="{ row }">
            <el-text style="font-weight: 700; font-size: 13px;">{{ row.orderNumber }}</el-text>
          </template>
        </el-table-column>

        <el-table-column label="Ngày đặt" width="170">
          <template #default="{ row }">
            <el-text size="small">{{ formatDate(row.createdAt) }}</el-text>
          </template>
        </el-table-column>

        <el-table-column label="Thanh toán" width="130">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain">{{ row.paymentMethod || '—' }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Kênh" width="110">
          <template #default="{ row }">
            <el-text size="small" type="info">{{ row.channel || '—' }}</el-text>
          </template>
        </el-table-column>

        <el-table-column label="Tổng tiền" width="160" align="right">
          <template #default="{ row }">
            <el-text type="primary" style="font-size: 14px; font-weight: 700;">{{ formatMoney(row.totalAmount) }}</el-text>
          </template>
        </el-table-column>

        <el-table-column label="Trạng thái" width="140">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="dark" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Thao tác" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-space :size="6" style="justify-content: center; flex-wrap: wrap;">
              <el-button size="small" type="primary" plain @click="$router.push(`/orders/${row.orderId}`)">
                <el-icon><View /></el-icon> Chi tiết
              </el-button>
              <el-button
                v-if="canCancel(row.status)"
                size="small"
                type="danger"
                plain
                :loading="cancelingId === row.orderId"
                @click="onCancel(row.orderId)"
              >
                Hủy
              </el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>

    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { Refresh, View } from "@element-plus/icons-vue";
import { ordersApi } from "../../api/orders.api";
import { ElMessageBox, ElMessage } from "element-plus";

const orders      = ref([]);
const loading     = ref(false);
const error       = ref("");
const cancelingId = ref(null);

onMounted(loadOrders);

async function loadOrders() {
  loading.value = true;
  error.value   = "";
  try {
    const res = await ordersApi.getMyOrders();
    orders.value = res?.data || [];
  } catch (e) {
    error.value = e?.response?.data?.message || "Không tải được đơn hàng";
  } finally {
    loading.value = false;
  }
}

async function onCancel(orderId) {
  try {
    await ElMessageBox.confirm(
      "Bạn có chắc muốn hủy đơn hàng này không?",
      "Xác nhận hủy đơn",
      { type: "warning", confirmButtonText: "Hủy đơn", cancelButtonText: "Giữ lại" }
    );
    cancelingId.value = orderId;
    await ordersApi.cancel(orderId);
    ElMessage.success("Đã hủy đơn hàng thành công");
    await loadOrders();
  } catch (e) {
    if (e?.response?.data?.message) ElMessage.error(e.response.data.message);
  } finally {
    cancelingId.value = null;
  }
}

const STATUS_LABELS = {
  PENDING:"Chờ xử lý", CONFIRMED:"Đã xác nhận", PROCESSING:"Đang xử lý",
  SHIPPING:"Đang giao", DELIVERED:"Đã giao", CANCELLED:"Đã hủy", PAID:"Đã thanh toán",
};

const STATUS_TYPES = {
  PENDING:"warning", CONFIRMED:"primary", PROCESSING:"primary",
  SHIPPING:"primary", DELIVERED:"success", CANCELLED:"danger", PAID:"success",
};

function statusLabel(s) { return STATUS_LABELS[s] || s; }
function statusType(s)  { return STATUS_TYPES[s]  || "info"; }
function canCancel(s)   { return ["PENDING","CONFIRMED"].includes(s); }
function countByStatus(s) { return orders.value.filter(o => o.status === s).length; }

const stats = computed(() => [
  { label:"Tổng đơn",   value: orders.value.length,                             color:"#2563eb", subColor:"#64748b", bg:"#eff6ff", border:"#bfdbfe" },
  { label:"Chờ xử lý",  value: countByStatus("PENDING"),                        color:"#d97706", subColor:"#92400e", bg:"#fffbeb", border:"#fde68a" },
  { label:"Đang xử lý", value: countByStatus("CONFIRMED")+countByStatus("PROCESSING"), color:"#7c3aed", subColor:"#5b21b6", bg:"#f5f3ff", border:"#ddd6fe" },
  { label:"Đã giao",    value: countByStatus("DELIVERED"),                       color:"#16a34a", subColor:"#14532d", bg:"#f0fdf4", border:"#bbf7d0" },
  { label:"Đã hủy",     value: countByStatus("CANCELLED"),                       color:"#dc2626", subColor:"#991b1b", bg:"#fff1f2", border:"#fecdd3" },
]);

function formatDate(date) {
  if (!date) return "—";
  return new Date(date).toLocaleString("vi-VN", { year:"numeric", month:"2-digit", day:"2-digit", hour:"2-digit", minute:"2-digit" });
}
function formatMoney(value) {
  if (value == null) return "—";
  return Number(value).toLocaleString("vi-VN") + " ₫";
}
</script>

<style></style>