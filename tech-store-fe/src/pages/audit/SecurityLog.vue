<template>
  <el-space direction="vertical" fill :size="20" style="width: 100%">
    <!-- Header -->
    <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:20px; margin-bottom:22px; flex-wrap:wrap;">
      <div>
        <div style="font-size:11px; font-weight:700; letter-spacing:0.08em; text-transform:uppercase; color:var(--el-text-color-secondary); margin-bottom:6px; display:flex; align-items:center; gap:5px;">
            <el-icon><Lock /></el-icon> Security
          </div>
        <div style="font-size:28px; font-weight:800; letter-spacing:-0.03em; margin-bottom:4px;">Lịch sử <span style="color:var(--el-color-primary);">Bảo mật</span></div>
        <div style="font-size:13px; color:var(--el-text-color-secondary);">Lịch sử các sự kiện bảo mật và truy cập hệ thống</div>
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
            <el-form-item label="Tài khoản">
              <el-input v-model="filter.username" placeholder="VD: admin" clearable @keyup.enter="searchLogs" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="4">
            <el-form-item label="Địa chỉ IP">
              <el-input v-model="filter.ipAddress" placeholder="VD: 192.168.1.1" clearable @keyup.enter="searchLogs" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="4">
            <el-form-item label="Mô tả">
              <el-input v-model="filter.keyword" placeholder="Từ khoá..." clearable @keyup.enter="searchLogs" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="4">
            <el-form-item label="Mức độ">
              <el-select v-model="filter.severity" placeholder="Tất cả mức độ" clearable style="width: 100%">
                <el-option label="NGHIÊM TRỌNG" value="CRITICAL" />
                <el-option label="CAO" value="HIGH" />
                <el-option label="TRUNG BÌNH" value="MEDIUM" />
                <el-option label="THẤP" value="LOW" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="4">
            <el-form-item label="Trạng thái">
              <el-select v-model="filter.status" placeholder="Tất cả trạng thái" clearable style="width: 100%">
                <el-option label="THÀNH CÔNG" value="SUCCESS" />
                <el-option label="THẤT BẠI" value="FAILED" />
                <el-option label="BỊ CHẶN" value="BLOCKED" />
                <el-option label="401" value="401" />
                <el-option label="403" value="403" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="4">
            <el-form-item label="Thời gian">
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="→"
                start-placeholder="Từ"
                end-placeholder="Đến"
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
        :row-class-name="tableRowClassName"
        :default-sort="{ prop: 'createdAt', order: 'descending' }"
      >
        <el-table-column prop="id" label="ID" width="80" sortable="custom">
          <template #default="{ row }">
            <span class="sl-mono-id">{{ row.id }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="username" label="Tài khoản" width="150" />

        <el-table-column prop="actionType" label="Hành động" min-width="180">
          <template #default="{ row }">
            <span style="font-weight: 600">{{ row.actionType ?? '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="targetEntity" label="Đối tượng" width="140">
          <template #default="{ row }">
            <span class="sl-mono" style="opacity: 0.7">{{ row.targetEntity ?? '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="targetId" label="Mã đối tượng" width="110">
          <template #default="{ row }">
            <span class="sl-mono-id">{{ row.targetId ?? '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="ipAddress" label="Địa chỉ IP" width="150">
          <template #default="{ row }">
            <span class="sl-mono" style="opacity: 0.7">{{ row.ipAddress ?? '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="severity" label="Mức độ" width="120">
          <template #default="{ row }">
            <el-tag :type="severityTagType(row.severity)" effect="dark" size="small">
              {{ formatSeverity(row.severity) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="Trạng thái" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="plain" size="small">
              {{ formatStatus(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="description" label="Mô tả" min-width="220">
          <template #default="{ row }">
            <el-tooltip :content="row.description" placement="top" v-if="row.description">
              <span class="sl-desc">{{ row.description }}</span>
            </el-tooltip>
            <span v-else>—</span>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="Thời gian" width="180" sortable="custom">
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
import { 
  Lock, Filter, Search, Refresh 
} from "@element-plus/icons-vue";

/* Helpers for display */
function severityTagType(severity) {
  switch (severity) {
    case "CRITICAL": return "danger";
    case "HIGH":     return "warning";
    case "MEDIUM":   return "primary";
    case "LOW":      return "success";
    default:         return "info";
  }
}

function formatSeverity(severity) {
  return { CRITICAL: 'NGHIÊM TRỌNG', HIGH: 'CAO', MEDIUM: 'TRUNG BÌNH', LOW: 'THẤP' }[severity] || severity || '—';
}

function statusTagType(status) {
  if (status === 'SUCCESS' || status === 200) return "success";
  if (status === 'FAILED' || status === 401 || status === 403) return "danger";
  if (status === 'BLOCKED') return "warning";
  return "info";
}

function formatStatus(status) {
  return { SUCCESS: 'THÀNH CÔNG', FAILED: 'THẤT BẠI', BLOCKED: 'BỊ CHẶN' }[status] || status || '—';
}

function tableRowClassName({ row }) {
  if (row.severity === 'CRITICAL') return 'row-critical';
  if (row.severity === 'HIGH') return 'row-high';
  return '';
}

/* ================= STATE ================= */

const logs    = ref([]);
const total   = ref(0);
const page    = ref(1);
const size    = ref(10);
const loading = ref(false);

const sortBy  = ref("createdAt");
const sortDir = ref("DESC");

const dateRange = ref([]);

const filter = reactive({
  username:  null,
  ipAddress: null,
  keyword:   null,
  severity:  null,
  status:    null,
  startDate: null,
  endDate:   null,
});

/* ================= COMPUTED ================= */

const activeFilterCount = computed(() =>
  [filter.username, filter.ipAddress, filter.keyword, filter.severity, filter.status, dateRange.value?.length].filter(Boolean).length
);

/* ================= HELPERS ================= */

function fmtDate(iso) {
  if (!iso) return '—';
  try { return new Date(iso).toLocaleString('vi-VN'); } catch { return iso; }
}

/* ================= BUILD PAYLOAD ================= */

function buildPayload() {
  if (dateRange.value?.length === 2) {
    filter.startDate = dateRange.value[0] + "T00:00:00";
    filter.endDate   = dateRange.value[1] + "T23:59:59";
  } else {
    filter.startDate = null;
    filter.endDate   = null;
  }

  return {
    username:   filter.username  || null,
    ipAddress:  filter.ipAddress || null,
    keyword:    filter.keyword   || null,
    severities: filter.severity  ? [filter.severity] : null,
    statuses:   filter.status    ? [filter.status]   : null,
    startDate:  filter.startDate,
    endDate:    filter.endDate,
  };
}

/* ================= FETCH ================= */

async function fetchLogs() {
  loading.value = true;
  try {
    const payload = buildPayload();
    const res = await http.post("/api/auth/security-log/search", payload, {
      params: {
        page:    page.value - 1,
        size:    size.value,
        sortBy:  sortBy.value,
        sortDir: sortDir.value,
      },
    });
    logs.value  = res.data.content;
    total.value = res.data.totalElements;
  } catch (err) {
    console.error("Load failed:", err.response?.data || err);
  } finally {
    loading.value = false;
  }
}

/* ================= EVENTS ================= */

function searchLogs() { page.value = 1; fetchLogs(); }

function resetFilter() {
  filter.username  = null;
  filter.ipAddress = null;
  filter.keyword   = null;
  filter.severity  = null;
  filter.status    = null;
  dateRange.value  = [];
  page.value = 1;
  fetchLogs();
}

function handlePageChange(p) { page.value = p; fetchLogs(); }
function handleSizeChange(s) { size.value = s; page.value = 1; fetchLogs(); }

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

/* ================= INIT ================= */

onMounted(fetchLogs);
</script>

<style scoped>
.sl-eyebrow {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  opacity: 0.6;
}

.sl-title {
  font-size: 28px;
  font-weight: 800;
  margin: 0 0 4px;
}

.sl-title-accent {
  color: var(--el-color-danger);
}

.sl-subtitle {
  font-size: 13.5px;
  opacity: 0.7;
  margin: 0;
}

.sl-mono-id {
  font-family: inherit;
  font-weight: 600;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  padding: 2px 7px;
  border-radius: 4px;
}

.sl-mono {
  font-family: inherit;
}

.sl-desc {
  display: block;
  font-size: 12.5px;
  opacity: 0.7;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 260px;
}

:deep(.row-critical) {
  --el-table-tr-bg-color: var(--el-color-danger-light-9);
}
:deep(.row-high) {
  --el-table-tr-bg-color: var(--el-color-warning-light-9);
}
</style>