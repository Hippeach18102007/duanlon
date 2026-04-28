<template>
  <div>
    <el-card shadow="never">
      <!-- Header -->
      <el-row justify="space-between" align="bottom">
        <el-col :span="12">
          <div class="kicker">Admin · Quản lý đơn hàng</div>
          <div class="title">{{ title }}</div>
        </el-col>
        <el-col :span="12">
          <el-row justify="end" align="middle">
            <el-space>
              <el-tag v-if="rows.length" type="info" effect="plain">
                {{ rows.length }} đơn
              </el-tag>
              <el-button plain :loading="loading" @click="load">
                <el-icon><Refresh /></el-icon>
                Làm mới
              </el-button>
            </el-space>
          </el-row>
        </el-col>
      </el-row>

      <el-divider class="divider" />

      <!-- Table -->
      <el-table
        :data="pagedRows"
        v-loading="loading"
        border
        stripe
        @row-click="goDetail"
        row-class-name="order-row"
      >
        <el-table-column prop="id" label="Mã đơn" width="90" align="center">
          <template #default="{ row }">
            #{{ row.id }}
          </template>
        </el-table-column>

        <el-table-column label="Số đơn" width="150">
          <template #default="{ row }">
            {{ row.orderNumber }}
          </template>
        </el-table-column>

        <el-table-column label="Trạng thái" width="155" align="center">
          <template #default="{ row }">
            <el-tag
              :type="statusMap[row.status]?.type ?? 'info'"
              effect="light"
              round
            >
              {{ statusMap[row.status]?.label ?? row.status }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Khách hàng" width="130" align="center">
          <template #default="{ row }">
            {{ row.customerId }}
          </template>
        </el-table-column>

        <el-table-column label="Thanh toán" width="160">
          <template #default="{ row }">
            <el-space :size="6">
              <el-icon><component :is="paymentIcon(row.paymentMethod)" /></el-icon>
              <span>{{ formatPayment(row.paymentMethod) }}</span>
            </el-space>
          </template>
        </el-table-column>

        <el-table-column label="Kênh" width="115" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.channel === 'ONLINE' ? 'primary' : 'warning'"
              effect="plain"
              size="small"
            >
              {{ row.channel === "ONLINE" ? "Online" : "Offline" }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Thời gian tạo" min-width="160">
          <template #default="{ row }">
            <template v-if="row.createdAtDate">
              <el-space direction="vertical" :size="2" alignment="flex-start">
                <span>{{ row.createdAtDate }}</span>
                <el-text type="info" size="small">{{ row.createdAtTime }}</el-text>
              </el-space>
            </template>
            <span v-else>—</span>
          </template>
        </el-table-column>

        <el-table-column label="Tổng tiền" width="150" align="right">
          <template #default="{ row }">
            {{ formatCurrency(row.totalAmount) }}
          </template>
        </el-table-column>

        <el-table-column label="Giảm giá" width="130" align="right">
          <template #default="{ row }">
            <el-text v-if="row.discountTotal" type="danger">
              -{{ formatCurrency(row.discountTotal) }}
            </el-text>
            <span v-else>—</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-if="!loading && rows.length === 0"
        description="Không có đơn hàng nào"
        :image-size="80"
        class="empty"
      />

      <!-- Pagination -->
      <el-row
        v-if="rows.length > 0"
        justify="space-between"
        align="middle"
        class="pagination-bar"
      >
        <el-text type="info" size="small">
          Hiển thị {{ pageStart }}–{{ pageEnd }} / {{ rows.length }} đơn
        </el-text>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="rows.length"
          :page-sizes="[10, 20, 50, 100]"
          layout="sizes, prev, pager, next, jumper"
          background
          @size-change="onSizeChange"
          @current-change="onPageChange"
        />
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { 
  Refresh, 
  Wallet, 
  CreditCard, 
  Postcard, 
  Box, 
  Money, 
  ChromeFilled, 
  House,
  Timer,
  Loading,
  Van,
  CircleCheck,
  CircleClose,
  RefreshLeft,
  Check
} from "@element-plus/icons-vue";
import { toast } from "../../ui/toast";

const props = defineProps({
  title: { type: String, required: true },
  endpoint: { type: String, required: true },
  loader: { type: Function, required: true },
});

const router = useRouter();
const loading = ref(false);
const rows = ref([]);

// ── Pagination state ───────────────────────────────────
const currentPage = ref(1);
const pageSize = ref(20);

const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return rows.value.slice(start, start + pageSize.value);
});

const pageStart = computed(() =>
  rows.value.length === 0 ? 0 : (currentPage.value - 1) * pageSize.value + 1,
);
const pageEnd = computed(() =>
  Math.min(currentPage.value * pageSize.value, rows.value.length),
);

function onSizeChange(val) {
  pageSize.value = val;
  currentPage.value = 1;
}
function onPageChange(val) {
  currentPage.value = val;
}

// ── Status config ──────────────────────────────────────
const statusMap = {
  PENDING:    { label: "Chờ xử lý", type: "warning", icon: "Timer" },
  PROCESSING: { label: "Đang xử lý", type: "",        icon: "Loading" },
  SHIPPING:   { label: "Đang giao",  type: "warning", icon: "Van" },
  DELIVERED:  { label: "Đã giao",   type: "success", icon: "CircleCheck" },
  PAID:       { label: "Đã T.Toán", type: "success", icon: "Check" },
  CANCELLED:  { label: "Đã hủy",    type: "danger",  icon: "CircleClose" },
  RETURNED:   { label: "Trả hàng",  type: "info",    icon: "RefreshLeft" },
};

// ── Format helpers ─────────────────────────────────────
function parseDate(val) {
  if (!val) return null;
  if (Array.isArray(val)) {
    const [y, mo, d, h = 0, min = 0] = val;
    return new Date(y, mo - 1, d, h, min);
  }
  const d = new Date(val);
  return isNaN(d.getTime()) ? null : d;
}

function formatCurrency(val) {
  if (val == null) return "—";
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(val);
}

function formatDate(val) {
  const d = parseDate(val);
  if (!d) return null;
  return d.toLocaleDateString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

function formatTime(val) {
  const d = parseDate(val);
  if (!d) return "";
  return d.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
}

function formatPayment(val) {
  const map = {
    CASH: "Tiền mặt",
    CARD: "Thẻ",
    TRANSFER: "Chuyển khoản",
    COD: "COD",
  };
  return map[val] ?? val ?? "—";
}

function paymentIcon(val) {
  const map = { CASH: Wallet, CARD: CreditCard, TRANSFER: Postcard, COD: Box };
  return map[val] ?? Money;
}

// ── Data helpers ───────────────────────────────────────
function extractList(payload) {
  if (!payload) return [];
  if (Array.isArray(payload)) return payload;
  const root = payload?.data ?? payload;
  if (Array.isArray(root)) return root;
  for (const k of ["content", "items", "results", "rows", "list"]) {
    if (Array.isArray(root?.[k])) return root[k];
    if (Array.isArray(root?.data?.[k])) return root.data[k];
  }
  return [];
}

function normalize(list) {
  return (list || [])
    .map((o, idx) => {
      const raw = o?.createdAt ?? o?.createdDate ?? null;
      return {
        id: o?.id ?? o?.orderId ?? idx + 1,
        orderNumber: o?.orderNumber ?? o?.order_number ?? "—",
        status: o?.status ?? "—",
        customerId: o?.customerId ?? o?.customer?.id ?? "—",
        paymentMethod: o?.paymentMethod ?? "—",
        channel: o?.channel ?? "—",
        totalAmount: o?.totalAmount ?? o?.total_amount ?? null,
        discountTotal: o?.discountTotal ?? o?.discount_total ?? null,
        createdAtDate: formatDate(raw),
        createdAtTime: formatTime(raw),
        _sortKey: parseDate(raw)?.getTime() ?? 0,
      };
    })
    .sort((a, b) => b._sortKey - a._sortKey);
}

// ── Load ───────────────────────────────────────────────
async function load() {
  loading.value = true;
  try {
    const res = await props.loader();
    rows.value = normalize(extractList(res?.data));
    currentPage.value = 1;
  } catch {
    rows.value = [];
    toast("Không thể tải danh sách đơn hàng.", "error");
  } finally {
    loading.value = false;
  }
}

function goDetail(row) {
  const id = row?.id;
  if (!id) return;
  router.push(`/system/orders/${id}`);
}

defineExpose({ load });
onMounted(load);
</script>

<style scoped>
:deep(.el-tag) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: normal;
  height: 28px;
  vertical-align: middle;
}
:deep(.el-tag .el-icon) {
  display: none;
}
.kicker {
  text-transform: uppercase;
  letter-spacing: 0.8px;
}

.divider {
  margin: 12px 0;
}

:deep(.order-row) {
  cursor: pointer;
}

.empty {
  margin-top: 16px;
}

.pagination-bar {
  margin-top: 18px;
  padding-top: 14px;
}
</style>