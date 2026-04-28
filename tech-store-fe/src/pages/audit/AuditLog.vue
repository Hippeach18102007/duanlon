<template>
  <el-space direction="vertical" fill :size="20" style="width: 100%">
    <!-- Header -->
    <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:20px; margin-bottom:22px; flex-wrap:wrap;">
      <div>
        <div style="font-size:11px; font-weight:700; letter-spacing:0.08em; text-transform:uppercase; color:var(--el-text-color-secondary); margin-bottom:6px; display:flex; align-items:center; gap:5px;">
            <el-icon><Document /></el-icon> Audit
          </div>
        <div style="font-size:28px; font-weight:800; letter-spacing:-0.03em; margin-bottom:4px;">Audit <span style="color:var(--el-color-primary);">Logs</span></div>
        <div style="font-size:13px; color:var(--el-text-color-secondary);">Lịch sử hoạt động và thay đổi toàn hệ thống</div>
      </div>
      <div style="display:flex; align-items:center; gap:8px; flex-shrink:0; padding-top:4px; flex-wrap:wrap;">
        <el-button type="primary" plain @click="exportCsv">
          <el-icon><Download /></el-icon> Export CSV
        </el-button>
      </div>
    </div>

    <!-- ── Filter Panel ── -->
    <el-card shadow="never">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <el-space :size="8">
            <el-icon><Filter /></el-icon>
            <span>Bộ lọc</span>
            <el-tag v-if="activeFilterCount > 0" size="small" effect="plain" type="primary">
              {{ activeFilterCount }} đang áp dụng
            </el-tag>
          </el-space>
          <el-button v-if="activeFilterCount > 0" type="danger" link @click="resetFilter">Xoá tất cả</el-button>
        </div>
      </template>

      <el-form :model="filter" label-position="top">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :md="4">
            <el-form-item label="Description">
              <el-input v-model="filter.keyword" placeholder="Từ khoá..." clearable @keyup.enter="searchLogs" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="4">
            <el-form-item label="User ID">
              <el-input v-model.number="filter.userId" type="number" placeholder="VD: 42" clearable @keyup.enter="searchLogs" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="4">
            <el-form-item label="Module">
              <el-select v-model="filter.module" placeholder="Tất cả module" clearable style="width: 100%">
                <el-option label="USER" value="USER" />
                <el-option label="CUSTOMER" value="CUSTOMER" />
                <el-option label="ORDER" value="ORDER" />
                <el-option label="PRODUCT" value="PRODUCT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="4">
            <el-form-item label="Action">
              <el-select v-model="filter.action" placeholder="Tất cả action" clearable style="width: 100%">
                <el-option label="CREATE" value="CREATE" />
                <el-option label="UPDATE" value="UPDATE" />
                <el-option label="DELETE" value="DELETE" />
                <el-option label="CHANGE_ROLE" value="CHANGE_ROLE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="8">
            <el-form-item label="Thời gian">
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="→"
                start-placeholder="Từ ngày"
                end-placeholder="Đến ngày"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        
        <div style="display: flex; gap: 8px; justify-content: flex-end; margin-top: 8px">
          <el-button type="primary" @click="searchLogs">
            <el-icon><Search /></el-icon>
            <span>Tìm kiếm</span>
          </el-button>
          <el-button type="warning" plain @click="loadThisWeek">
            <el-icon><Calendar /></el-icon>
            <span>Tuần này</span>
          </el-button>
          <el-button @click="resetFilter">
            <el-icon><Refresh /></el-icon>
            <span>Reset</span>
          </el-button>
        </div>
      </el-form>
    </el-card>

    <!-- ── Table Card ── -->
    <el-card shadow="never">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <el-space :size="4">
            <span style="font-size: 18px; font-weight: 800">{{ total }}</span>
            <span style="font-size: 12px; opacity: 0.6; font-weight: 600">bản ghi</span>
          </el-space>
        </div>
      </template>

      <el-table 
        v-loading="loading" 
        :data="logs" 
        style="width: 100%" 
        @sort-change="handleSortChange"
        :default-sort="{ prop: 'createdAt', order: 'descending' }"
      >
        <el-table-column prop="id" label="ID" width="80" sortable="custom">
          <template #default="{ row }">
            <span class="al-mono-id">{{ row.id }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="User" min-width="180">
          <template #default="{ row }">
            <div style="display: flex; flex-direction: column">
              <span style="font-weight: 600">{{ row.user?.username ?? '—' }}</span>
              <span v-if="row.user?.id" style="font-size: 10.5px; opacity: 0.5" class="al-mono">#{{ row.user.id }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="module" label="Module" width="130">
          <template #default="{ row }">
            <el-tag :type="getModuleTagType(row.module)" effect="light">{{ row.module ?? '—' }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="action" label="Action" width="140">
          <template #default="{ row }">
            <el-tag :type="getActionTagType(row.action)" effect="plain">{{ row.action ?? '—' }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="targetType" label="Target Type" width="130">
          <template #default="{ row }">
            <span class="al-mono" style="opacity: 0.7">{{ row.targetType ?? '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="targetId" label="Target ID" width="100">
          <template #default="{ row }">
            <span class="al-mono-id">{{ row.targetId ?? '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="ipAddress" label="IP Address" width="150">
          <template #default="{ row }">
            <span class="al-mono" style="opacity: 0.7">{{ row.ipAddress ?? '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="detailsJson" label="Details" min-width="260">
          <template #default="{ row }">
            <el-tooltip :content="row.detailsJson" placement="top" v-if="row.detailsJson">
              <span class="al-details">{{ row.detailsJson }}</span>
            </el-tooltip>
            <span v-else>—</span>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="Created At" width="180" sortable="custom">
          <template #default="{ row }">
            <span style="opacity: 0.7">{{ fmtDate(row.createdAt) }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 20px; display: flex; justify-content: space-between; align-items: center">
        <span style="font-size: 13px; opacity: 0.6">
          {{ (page - 1) * size + 1 }}–{{ Math.min(page * size, total) }} / {{ total }} bản ghi
        </span>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :page-sizes="[5, 10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </el-space>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from "vue";
import http from "../../api/http";
import { auditApi } from "../../api/audit.api";
import { 
  Document, Download, Filter, Search, 
  Refresh, Calendar 
} from "@element-plus/icons-vue";

/* =========================
   STATE
========================= */

const logs = ref([]);
const total = ref(0);
const page = ref(1);
const size = ref(10);
const loading = ref(false);

const sortBy = ref("createdAt");
const sortDir = ref("DESC");

const dateRange = ref([]);

const filter = reactive({
  userId: null,
  module: null,
  action: null,
  keyword: "",
  startDate: null,
  endDate: null,
});

/* =========================
   COMPUTED
========================= */

const activeFilterCount = computed(() =>
  [filter.keyword, filter.userId, filter.module, filter.action, dateRange.value?.length].filter(Boolean).length
);

/* =========================
   HELPERS
========================= */

function fmtDate(iso) {
  if (!iso) return '—';
  try { return new Date(iso).toLocaleString('vi-VN'); } catch { return iso; }
}

function getModuleTagType(m) {
  return { USER: 'primary', CUSTOMER: 'success', ORDER: 'warning', PRODUCT: 'danger' }[m] || 'info';
}

function getActionTagType(a) {
  return { CREATE: 'success', UPDATE: 'warning', DELETE: 'danger', CHANGE_ROLE: 'danger' }[a] || 'info';
}

/* =========================
   API CALL
========================= */

async function fetchLogs() {
  loading.value = true;

  if (dateRange.value?.length === 2) {
    filter.startDate = dateRange.value[0];
    filter.endDate = dateRange.value[1];
  } else {
    filter.startDate = null;
    filter.endDate = null;
  }

  const payload = {
    userId: filter.userId || null,
    modules: filter.module ? [filter.module] : null,
    actions: filter.action ? [filter.action] : null,
    keyword: filter.keyword || null,
    startDate: filter.startDate || null,
    endDate: filter.endDate || null
  };

  try {
    const res = await http.post("/api/auth/audit-log/search", payload, {
      params: {
        page: page.value - 1,
        size: size.value,
        sortBy: sortBy.value,
        sortDir: sortDir.value,
      },
    });

    logs.value = res.data.content;
    total.value = res.data.totalElements;
  } finally {
    loading.value = false;
  }
}

function getWeekRange() {
  const now = new Date();
  const day = now.getDay() || 7;
  const monday = new Date(now);
  monday.setDate(now.getDate() - day + 1);
  const sunday = new Date(monday);
  sunday.setDate(monday.getDate() + 6);
  const format = (d) => d.toISOString().split("T")[0];
  return [format(monday), format(sunday)];
}

function loadThisWeek() {
  const [start, end] = getWeekRange();
  dateRange.value = [start, end];
  filter.startDate = start;
  filter.endDate = end;
  page.value = 1;
  fetchLogs();
}

/* =========================
   EVENTS
========================= */

function searchLogs() {
  page.value = 1;
  fetchLogs();
}

function resetFilter() {
  filter.userId = null;
  filter.module = null;
  filter.action = null;
  filter.keyword = "";
  dateRange.value = [];
  searchLogs();
}

function handlePageChange(p) {
  page.value = p;
  fetchLogs();
}

function handleSizeChange(s) {
  size.value = s;
  page.value = 1;
  fetchLogs();
}

function handleSortChange({ prop, order }) {
  if (!order) {
    sortBy.value = "createdAt";
    sortDir.value = "DESC";
  } else {
    sortBy.value = prop;
    sortDir.value = order === "ascending" ? "ASC" : "DESC";
  }
  fetchLogs();
}

/* =========================
   EXPORT CSV
========================= */

async function exportCsv() {
  try {
    if (dateRange.value?.length === 2) {
      filter.startDate = dateRange.value[0];
      filter.endDate = dateRange.value[1];
    }

    const payload = {
      userId: filter.userId || null,
      modules: filter.module ? [filter.module] : null,
      actions: filter.action ? [filter.action] : null,
      keyword: filter.keyword || null,
      startDate: filter.startDate || null,
      endDate: filter.endDate || null
    };

    const res = await auditApi.exportAdvanced(payload);

    const url = window.URL.createObjectURL(new Blob([res.data]));
    const link = document.createElement("a");
    link.href = url;
    link.download = "audit_logs.csv";
    link.click();
  } catch (err) {
    console.error("Export failed", err);
  }
}

/* =========================
   INIT
========================= */

onMounted(fetchLogs);
</script>

<style scoped>
.al-eyebrow {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  opacity: 0.6;
}

.al-title {
  font-size: 28px;
  font-weight: 800;
  margin: 0 0 4px;
}

.al-title-accent {
  color: var(--el-color-primary);
}

.al-subtitle {
  font-size: 13.5px;
  opacity: 0.7;
  margin: 0;
}

.al-mono-id {
  font-family: inherit;
  font-weight: 600;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  padding: 2px 7px;
  border-radius: 4px;
}

.al-mono {
  font-family: inherit;
}

.al-details {
  display: block;
  font-size: 12px;
  opacity: 0.7;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 300px;
}
</style>