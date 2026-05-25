<template>
  <div style="max-width: 1280px; margin: 0 auto; padding: 24px;">
    <el-card shadow="never" class="profile-card">

      <el-page-header @back="$router.back()" style="margin-bottom: 20px;">
        <template #content>
          <el-text tag="span" style="font-size: 20px; font-weight: 700;">Đơn hàng của tôi</el-text>
        </template>
        <template #extra>
          <el-button :loading="loading" @click="loadOrders">
            <el-icon><Refresh /></el-icon> Tải lại
          </el-button>
        </template>
      </el-page-header>

      <el-divider style="margin: 0 0 20px;" />

      <el-alert v-if="error" type="error" :title="error" show-icon style="margin-bottom: 20px;" />

      <!-- Stats row using el-statistic -->
      <el-row :gutter="12" style="margin-bottom: 24px;">
        <el-col :xs="12" :sm="4" v-for="stat in stats" :key="stat.label">
          <el-card
            shadow="never"
            class="profile-card hoverable"
            :body-style="{ padding: '12px 16px', textAlign: 'center' }"
            :style="`background: var(${stat.bgVar}); border-color: var(${stat.borderVar}); margin-bottom: 10px;`"
          >
            <el-statistic :value="stat.value">
              <template #number>
                <el-text :style="`color: var(${stat.colorVar}); font-size: 22px; font-weight: 900;`">{{ stat.value }}</el-text>
              </template>
              <template #title>
                <el-text size="small" :style="`color: var(${stat.colorVar}); text-transform: uppercase; letter-spacing: 0.04em; font-weight: 600;`">
                  {{ stat.label }}
                </el-text>
              </template>
            </el-statistic>
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
            <el-space :size="6" style="justify-content: center;">
              <el-button size="small" type="primary" plain @click="$router.push(`/orders/${row.orderId}`)">
                <el-icon><View /></el-icon> Chi tiết
              </el-button>
              <el-popconfirm
                v-if="canCancel(row.status)"
                title="Xác nhận hủy đơn hàng này?"
                confirm-button-text="Hủy đơn"
                cancel-button-text="Giữ lại"
                confirm-button-type="danger"
                @confirm="onCancel(row.orderId)"
              >
                <template #reference>
                  <el-button size="small" type="danger" plain :loading="cancelingId === row.orderId">
                    Hủy
                  </el-button>
                </template>
              </el-popconfirm>
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
import { ElMessage } from "element-plus";

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
  cancelingId.value = orderId;
  try {
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
  SHIPPING:"Đang giao", DELIVERED:"Đã giao", CANCELLED:"Đã hủy", PAID:"Đã thanh toán",RETURNED:"Đã trả hàng",
};

const STATUS_TYPES = {
  PENDING:"warning", CONFIRMED:"primary", PROCESSING:"primary",
  SHIPPING:"primary", DELIVERED:"success", CANCELLED:"danger", PAID:"success",RETURNED:"info",
};

function statusLabel(s) { return STATUS_LABELS[s] || s; }
function statusType(s)  { return STATUS_TYPES[s]  || "info"; }
function canCancel(s)   { return ["PENDING","CONFIRMED"].includes(s); }
function countByStatus(s) { return orders.value.filter(o => o.status === s).length; }

const stats = computed(() => [
  { label:"Tổng đơn",   value: orders.value.length,                                  colorVar:"--el-color-primary",        bgVar:"--el-color-primary-light-9",   borderVar:"--el-color-primary-light-5"  },
  { label:"Chờ xử lý",  value: countByStatus("PENDING"),                             colorVar:"--el-color-warning",        bgVar:"--el-color-warning-light-9",   borderVar:"--el-color-warning-light-5"  },
  { label:"Đang xử lý", value: countByStatus("CONFIRMED")+countByStatus("PROCESSING"),colorVar:"--el-color-primary",       bgVar:"--el-color-primary-light-9",   borderVar:"--el-color-primary-light-5"  },
  { label:"Đã giao",    value: countByStatus("DELIVERED"),                            colorVar:"--el-color-success",        bgVar:"--el-color-success-light-9",   borderVar:"--el-color-success-light-5"  },
  { label:"Đã hủy",     value: countByStatus("CANCELLED"),                            colorVar:"--el-color-danger",         bgVar:"--el-color-danger-light-9",    borderVar:"--el-color-danger-light-5"   },
  { label:"Trả hàng",     value: countByStatus("RETURNED"),                            colorVar:"--el-color-info",         bgVar:"--el-color-info-light-9",    borderVar:"--el-color-info-light-5"},
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