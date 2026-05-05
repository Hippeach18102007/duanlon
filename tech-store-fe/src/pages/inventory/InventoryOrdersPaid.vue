<template>
  <el-space direction="vertical" alignment="stretch" :size="20" style="width: 100%">
    <!-- PAGE HEADER -->
    <el-row type="flex" justify="space-between" align="bottom">
      <el-col :span="16">
        <el-space direction="vertical" alignment="start" :size="4">
          <el-text type="primary" size="small" tag="b">KHO HÀNG</el-text>
          <h2>Đơn đã thanh toán</h2>
          <el-text type="info">Các đơn hàng chờ kho xử lý đóng gói</el-text>
        </el-space>
      </el-col>
      <el-col :span="8" style="text-align: right;">
        <el-button plain @click="loadOrders">
          Làm mới
        </el-button>
      </el-col>
    </el-row>

    <!-- URGENT ALERT -->
    <el-alert
      v-if="urgentOrders.length > 0"
      title="CẢNH BÁO ĐƠN HÀNG TỒN ĐỌNG"
      type="error"
      :description="`Có ${urgentOrders.length} đơn hàng đã thanh toán hơn 2 giờ nhưng chưa được xử lý. Vui lòng ưu tiên soạn hàng!`"
      show-icon
      :closable="false"
      class="priority-alert"
    />

    <!-- STATS ROW -->
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="never">
          <el-space>
            <el-avatar :size="40" class="stat-icon-green" shape="square">
              <el-icon :size="20"><DocumentChecked /></el-icon>
            </el-avatar>
            <el-space direction="vertical" alignment="start" :size="0">
              <el-text tag="b" size="large">{{ orders.length }}</el-text>
              <el-text type="info" size="small">Đơn chờ xử lý</el-text>
            </el-space>
          </el-space>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <el-space>
            <el-avatar :size="40" class="stat-icon-blue" shape="square">
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
          <el-space>
            <el-input
              v-model="search"
              placeholder="Tìm mã, tên, SĐT..."
              clearable
              class="search-input"
            />
            <el-button 
              v-if="selectedOrders.length > 0"
              type="warning" 
              @click="bulkMarkProcessing"
            >
              Xử lý {{ selectedOrders.length }} đơn đã chọn
            </el-button>
          </el-space>
          <el-tag type="warning" effect="light">
            {{ filteredOrders.length }} đơn hàng
          </el-tag>
        </el-row>
      </template>

      <el-table 
        :data="filteredOrders" 
        style="width: 100%" 
        empty-text="Không có đơn hàng nào"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="45" />

        <!-- Order number -->
        <el-table-column label="Mã đơn hàng" min-width="140">
          <template #default="{ row }">
            <el-space>
              <el-text tag="b">#{{ row.orderNumber }}</el-text>
              <el-tag v-if="isUrgent(row.createdAt)" type="danger" size="small" effect="dark" class="pulse-tag">
                RẤT GẤP
              </el-tag>
            </el-space>
          </template>
        </el-table-column>

        <!-- Customer -->
        <!-- Amount -->
        <el-table-column label="Giá trị" min-width="130" align="right">
          <template #default="{ row }">
            <el-text tag="b">{{ formatVND(row.totalAmount) }}</el-text>
          </template>
        </el-table-column>

        <!-- Status -->
        <el-table-column label="Trạng thái" width="140" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'PAID'" type="success" effect="light" round>
              Đã thanh toán
            </el-tag>
            <el-tag v-else-if="row.status === 'PENDING'" type="primary" effect="light" round>
              Thu hộ (COD)
            </el-tag>
            <el-tag v-else type="info" effect="light" round>
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- Date -->
        <el-table-column prop="createdAt" label="Ngày tạo" width="140">
          <template #default="{ row }">
            <el-text type="info">{{ formatDate(row.createdAt) }}</el-text>
          </template>
        </el-table-column>

        <!-- Actions -->
        <el-table-column label="Thao tác" width="200" align="center">
          <template #default="{ row }">
            <el-space>
              <el-button
                size="small"
                type="warning"
                plain
                @click="markProcessing(row.orderId)"
                :loading="loadingId === row.orderId"
              >
                Xử lý ngay
              </el-button>
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
import { paymentsApi } from "../../api/payments";
import { ElMessage, ElMessageBox } from "element-plus";
import { DocumentChecked, Money } from "@element-plus/icons-vue";
import { formatVND, formatDate, initials, parseSafeDate } from "../../utils/format";

const router = useRouter();
const orders = ref([]);
const search = ref("");
const loadingId = ref(null);
const selectedOrders = ref([]);

const isUrgent = (createdAt) => {
  if (!createdAt) return false;
  const date = parseSafeDate(createdAt);
  if (!date) return false;
  const twoHoursAgo = new Date(Date.now() - 2 * 60 * 60 * 1000);
  return date < twoHoursAgo;
};

const urgentOrders = computed(() => {
  return orders.value.filter(o => isUrgent(o.createdAt));
});

const filteredOrders = computed(() => {
  const q = search.value.toLowerCase();
  const res = orders.value.filter(o => 
    o.orderNumber?.toLowerCase().includes(q) ||
    o.customerName?.toLowerCase().includes(q) ||
    o.customerPhone?.includes(q)
  );
  // Sort: Oldest first (FIFO)
  return res.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
});

const totalAmount = computed(() =>
  orders.value.reduce((sum, o) => sum + (o.totalAmount || 0), 0)
);

const handleSelectionChange = (val) => {
  selectedOrders.value = val;
};

const loadOrders = async (silent = false) => {
  try {
    const [paidRes, newRes] = await Promise.all([
      ordersApi.listPaid(),
      ordersApi.listNew()
    ]);
    
    const paidList = paidRes.data || [];
    const codList = (newRes.data || []).filter(o => 
      o.paymentMethod === 'CASH' && o.channel === 'ONLINE'
    );

    orders.value = [...paidList, ...codList];
  } catch (error) {
    if (!silent) ElMessage.error("Không thể tải danh sách đơn hàng");
  }
};

const markProcessing = async (orderId) => {
  try {
    const order = orders.value.find(o => o.orderId === orderId);
    await ElMessageBox.confirm('Xác nhận xử lý đơn hàng này?', 'Xác nhận', { type: 'warning' });
    loadingId.value = orderId;
    
    // Bridge: Auto-pay for CASH orders that are still PENDING
    if (order && order.status === 'PENDING' && order.paymentMethod === 'CASH') {
      await paymentsApi.create({ orderId, method: 'CASH', transactionRef: 'COD-CONFIRMED' });
    }
    
    await ordersApi.markAsProcessing(orderId);
    ElMessage.success("Đã cập nhật trạng thái đơn hàng");
    await loadOrders(true);
  } catch (error) {
    if (error !== 'cancel') {
         ElMessage.error(error.response?.data?.message || "Có lỗi xảy ra");
    }
  } finally {
    loadingId.value = null;
  }
};

const bulkMarkProcessing = async () => {
  if (selectedOrders.value.length === 0) return;
  try {
    await ElMessageBox.confirm(
      `Xác nhận xử lý ${selectedOrders.value.length} đơn hàng đã chọn?`,
      'Xử lý hàng loạt',
      { type: 'warning' }
    );
    
    // Bridge: Auto-pay for CASH orders during bulk processing
    for (const order of selectedOrders.value) {
      if (order.status === 'PENDING' && order.paymentMethod === 'CASH' ) {
          await paymentsApi.create({ orderId: order.orderId, method: 'CASH', transactionRef: 'COD-BULK-CONFIRMED' });
      }
    }
    
    await Promise.all(selectedOrders.value.map(o => ordersApi.markAsProcessing(o.orderId)));
    ElMessage.success(`Đã xử lý ${selectedOrders.value.length} đơn hàng`);
    await loadOrders();
  } catch (error) {
    if (error !== 'cancel') ElMessage.error("Có lỗi xảy ra khi xử lý hàng loạt");
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
.priority-alert {
  border: 1px solid var(--el-color-error-light-3);
  box-shadow: 0 2px 12px 0 rgba(245, 108, 108, 0.1);
  margin-bottom: 5px;
}
.pulse-tag {
  animation: pulse-red 2s infinite;
}
@keyframes pulse-red {
  0% { transform: scale(1); box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.7); }
  70% { transform: scale(1.05); box-shadow: 0 0 0 6px rgba(245, 108, 108, 0); }
  100% { transform: scale(1); box-shadow: 0 0 0 0 rgba(245, 108, 108, 0); }
}
</style>