<template>
  <div class="container-xl">
    <!-- Header -->
    <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:20px; margin-bottom:22px; flex-wrap:wrap;">
      <div>
        <div style="font-size:11px; font-weight:700; letter-spacing:0.08em; text-transform:uppercase; color:var(--el-text-color-secondary); margin-bottom:6px; display:flex; align-items:center; gap:5px;">
          <el-icon><Tickets /></el-icon> Đơn hàng
        </div>
        <div style="font-size:28px; font-weight:800; letter-spacing:-0.03em; margin-bottom:4px;">Tra cứu Đơn hàng</div>
        <div style="font-size:13px; color:var(--el-text-color-secondary);">Tra cứu và lọc đơn hàng theo nhiều tiêu chí</div>
      </div>
    </div>

    <!-- ── Filter Panel ── -->
    <el-card shadow="never" :body-style="{ padding: 0 }">
      <template #header>
        <el-row justify="space-between" align="middle">
          <el-space>
            <el-icon><Filter /></el-icon>
            <span>Bộ lọc</span>
            <el-tag v-if="activeFilterCount > 0" type="primary" effect="plain" size="small" round>
              {{ activeFilterCount }} đang áp dụng
            </el-tag>
          </el-space>
          <el-button v-if="activeFilterCount > 0" link type="danger" @click="clearAll">
            Xoá tất cả
          </el-button>
        </el-row>
      </template>

      <div class="fp-fields">
        <div class="fp-field">
          <span class="fp-field-label">Thời gian</span>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            start-placeholder="Từ ngày"
            end-placeholder="Đến ngày"
            value-format="YYYY-MM-DD"
            class="fp-datepicker"
          />
        </div>

        <div class="fp-field">
          <span class="fp-field-label">Khách hàng</span>
          <el-select v-model="customerId" placeholder="Tất cả khách hàng" clearable filterable class="fp-select">
            <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </div>

        <div class="fp-field">
          <span class="fp-field-label">Kênh bán</span>
          <el-select v-model="channel" placeholder="Tất cả kênh" clearable class="fp-select">
            <el-option label="Online"  value="ONLINE"  />
            <el-option label="Offline" value="OFFLINE" />
          </el-select>
        </div>

        <div class="fp-field">
          <span class="fp-field-label">Trạng thái</span>
          <el-select v-model="orderStatus" placeholder="Tất cả trạng thái" clearable class="fp-select">
            <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </div>

        <div class="fp-action">
          <el-button type="primary" @click="handleFilter">
            <template #icon><el-icon><Search /></el-icon></template>
            Lọc
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- ── Result table ── -->
    <div class="mt-4">
      <OrderListBase
        :key="tableKey"
        title="Kết quả lọc đơn hàng"
        :loader="loadOrders"
        endpoint="/api/orders"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { Filter, Search } from "@element-plus/icons-vue";
import { ordersApi } from "../../api/orders.api";
import { customersApi } from "../../api/customers.api";
import OrderListBase from "./_OrderListBase.vue";

const dateRange  = ref([]);
const customerId = ref(null);
const channel    = ref(null);
const orderStatus = ref(null);
const customers  = ref([]);
const tableKey   = ref(0);

const statusOptions = [
  { value: "PENDING",    label: "Chờ xử lý" },
  { value: "PROCESSING", label: "Đang xử lý" },
  { value: "SHIPPING",   label: "Đang giao" },
  { value: "DELIVERED",  label: "Đã giao" },
  { value: "PAID",       label: "Đã thanh toán" },
  { value: "CANCELLED",  label: "Đã huỷ" },
];

const activeFilterCount = computed(() =>
  [dateRange.value?.length, customerId.value, channel.value, orderStatus.value].filter(Boolean).length
);

const clearAll = () => {
  dateRange.value  = [];
  customerId.value = null;
  channel.value    = null;
  orderStatus.value = null;
  tableKey.value++;
};

const handleFilter = () => { tableKey.value++; };

const loadCustomers = async () => {
  const res = await customersApi.listAll();
  customers.value = res.data || [];
};

const loadOrders = async () => {
  try {
    const from = dateRange.value?.[0] ? dateRange.value[0] + "T00:00:00Z" : null;
    const to   = dateRange.value?.[1] ? dateRange.value[1] + "T23:59:59Z" : null;
    return await ordersApi.filter(customerId.value || null, from, to, channel.value || null, orderStatus.value || null);
  } catch (err) {
    console.error(err);
    return { data: [] };
  }
};

onMounted(loadCustomers);
</script>

<style scoped>
/* Fields row */
.fp-fields {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr auto;
  align-items: center;
}

.fp-field {
  padding: 12px 16px;
  border-right: 1px solid var(--el-border-color-light);
}

.fp-field-label {
  display: block;
  font-size: 11px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-bottom: 6px;
}

.fp-datepicker,
.fp-select { width: 100%; }

:deep(.fp-datepicker .el-input__wrapper),
:deep(.fp-select .el-select__wrapper) {
  box-shadow: none !important;
  border: none !important;
  padding: 0;
  background: transparent;
  font-size: 13px;
}

.fp-action { padding: 0 16px; }

@media (max-width: 900px) {
  .fp-fields { grid-template-columns: 1fr 1fr; }
  .fp-field  { border-bottom: 1px solid var(--el-border-color-light); }
  .fp-action { padding: 12px 16px; grid-column: span 2; }
}
</style>