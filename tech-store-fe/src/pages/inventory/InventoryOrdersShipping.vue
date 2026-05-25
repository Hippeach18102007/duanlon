<template>
  <el-space direction="vertical" alignment="stretch" :size="20" style="width: 100%">
    <!-- PAGE HEADER -->
    <el-row type="flex" justify="space-between" align="bottom">
      <el-col :span="16">
        <el-space direction="vertical" alignment="start" :size="4">
          <el-text type="primary" size="small" tag="b">KHO HÀNG</el-text>
          <h2>Lịch sử xuất kho</h2>
          <el-text type="info">Danh sách các đơn hàng đã được giao cho đơn vị vận chuyển</el-text>
        </el-space>
      </el-col>
      <el-col :span="8" style="text-align: right;">
        <el-button plain @click="loadOrders">
          Làm mới
        </el-button>
      </el-col>
    </el-row>

    <!-- STATS ROW -->
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="never">
          <el-space>
            <el-avatar :size="40" class="stat-icon-blue" shape="square" style="background-color: var(--el-color-primary-light-9); color: var(--el-color-primary)">
              <el-icon :size="20"><Van /></el-icon>
            </el-avatar>
            <el-space direction="vertical" alignment="start" :size="0">
              <el-text tag="b" size="large">{{ orders.length }}</el-text>
              <el-text type="info" size="small">Đã xuất kho</el-text>
            </el-space>
          </el-space>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <el-space>
            <el-avatar :size="40" class="stat-icon-blue" shape="square" style="background-color: var(--el-color-primary-light-9); color: var(--el-color-primary)">
              <el-icon :size="20"><Money /></el-icon>
            </el-avatar>
            <el-space direction="vertical" alignment="start" :size="0">
              <el-text tag="b" size="large">{{ formatVND(totalAmount) }}</el-text>
              <el-text type="info" size="small">Tổng giá trị</el-text>
            </el-space>
          </el-space>
        </el-card>
      </el-col>
    </el-row>

    <!-- TABLE CARD -->
    <el-card shadow="never" class="table-card">
      <template #header>
        <el-row type="flex" justify="space-between" align="middle">
          <el-input
            v-model="search"
            placeholder="Tìm mã, tên, SĐT..."
            clearable
            class="search-input"
          />
          <el-tag type="primary" effect="light">
            {{ filteredOrders.length }} đơn hàng
          </el-tag>
        </el-row>
      </template>

      <el-table 
        :data="filteredOrders" 
        style="width: 100%" 
        empty-text="Không có đơn hàng nào"
      >
        <!-- Order number -->
        <el-table-column label="Mã đơn hàng" min-width="140">
          <template #default="{ row }">
            <el-text tag="b">#{{ row.orderNumber }}</el-text>
          </template>
        </el-table-column>

        <!-- Amount -->
        <el-table-column label="Giá trị" min-width="130" align="right">
          <template #default="{ row }">
            <el-text tag="b">{{ formatVND(row.totalAmount) }}</el-text>
          </template>
        </el-table-column>

        <!-- Date -->
        <el-table-column prop="shippedAt" label="Ngày xuất kho" width="180">
          <template #default="{ row }">
            <el-text type="info">{{ formatDateTime(row.shippedAt || row.createdAt) }}</el-text>
          </template>
        </el-table-column>

        <!-- Actions -->
        <el-table-column label="Thao tác" width="120" align="center">
          <template #default="{ row }">
            <el-space>
              <el-button
                size="small"
                plain
                @click="goDetail(row.orderId)"
              >
                Chi tiết
              </el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </el-space>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { ordersApi } from "../../api/orders.api";
import { ElMessage } from "element-plus";
import { Money, Van } from "@element-plus/icons-vue";
import { formatVND, formatDateTime, initials } from "../../utils/format";

const router = useRouter();
const orders = ref([]);
const search = ref("");

const filteredOrders = computed(() => {
  const q = search.value.toLowerCase();
  return orders.value.filter(o => 
    o.orderNumber?.toLowerCase().includes(q) ||
    o.customerName?.toLowerCase().includes(q) ||
    o.customerPhone?.includes(q)
  );
});

const totalAmount = computed(() =>
  orders.value.reduce((sum, o) => sum + (o.totalAmount || 0), 0)
);

const loadOrders = async (silent = false) => {
  try {
    const [shippingRes, deliveredRes] = await Promise.all([
      ordersApi.listShipping(),
      ordersApi.listDelivered()
    ]);
    const combined = [...(shippingRes.data || []), ...(deliveredRes.data || [])];
    combined.sort((a, b) => new Date(b.shippedAt || b.createdAt) - new Date(a.shippedAt || a.createdAt));
    orders.value = combined;
  } catch (error) {
    if (!silent) ElMessage.error("Không thể tải danh sách đơn hàng");
  }
};

const goDetail = (orderId) => router.push(`/inventory/orders/${orderId}`);

let timer;
onMounted(() => {
  loadOrders();
  timer = setInterval(() => loadOrders(true), 30000); // 30s
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
});
</script>

<style scoped>
h2 {
  margin: 0;
}
.search-input {
  width: 240px;
}
.table-card :deep(.el-card__body) {
  padding: 0;
}
</style>
