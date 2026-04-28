<template>
  <el-space direction="vertical" fill :size="20" style="width: 100%">
    <!-- Header -->
    <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:20px; margin-bottom:22px; flex-wrap:wrap;">
      <div>
        <div style="font-size:11px; font-weight:700; letter-spacing:0.08em; text-transform:uppercase; color:var(--el-text-color-secondary); margin-bottom:6px; display:flex; align-items:center; gap:5px;">
            <el-icon><Key /></el-icon> Security
          </div>
        <div style="font-size:28px; font-weight:800; letter-spacing:-0.03em; margin-bottom:4px;">Lịch sử <span style="color:var(--el-color-primary);">Đăng nhập</span></div>
        <div style="font-size:13px; color:var(--el-text-color-secondary);">Theo dõi hoạt động đăng nhập hệ thống</div>
      </div>
      <div style="display:flex; align-items:center; gap:8px; flex-shrink:0; padding-top:4px; flex-wrap:wrap;">
        <el-button type="primary" plain @click="exportCsv">
          <el-icon><Download /></el-icon> Xuất CSV
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
          <el-col :xs="24" :sm="12" :md="5">
            <el-form-item label="Username">
              <el-input v-model="filter.username" placeholder="VD: admin" clearable @keyup.enter="searchLogs" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="5">
            <el-form-item label="IP Address">
              <el-input v-model="filter.ipAddress" placeholder="192.168..." clearable @keyup.enter="searchLogs" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="4">
            <el-form-item label="Status">
              <el-select v-model="filter.success" placeholder="Tất cả" clearable style="width: 100%">
                <el-option label="THÀNH CÔNG" :value="true" />
                <el-option label="THẤT BẠI" :value="false" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="5">
            <el-form-item label="Từ ngày">
              <el-date-picker v-model="filter.fromDate" type="date" value-format="YYYY-MM-DD" placeholder="Từ ngày" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="5">
            <el-form-item label="Đến ngày">
              <el-date-picker v-model="filter.toDate" type="date" value-format="YYYY-MM-DD" placeholder="Đến ngày" style="width: 100%" />
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

      <el-table v-loading="loading" :data="logs" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80">
          <template #default="{ row }">
            <span class="al-mono-id">{{ row.id }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="User" min-width="150">
          <template #default="{ row }">
            <div style="display: flex; flex-direction: column">
              <span style="font-weight: 600">{{ row.username }}</span>
              <span style="font-size: 10.5px; opacity: 0.5">#{{ row.userId }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="success" label="Status" width="120">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'" effect="plain">
              {{ row.success ? "THÀNH CÔNG" : "THẤT BẠI" }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="ipAddress" label="IP" width="150">
          <template #default="{ row }">
            <span class="al-mono" style="opacity: 0.7">{{ row.ipAddress }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="userAgent" label="Thiết bị" min-width="250">
          <template #default="{ row }">
            <el-tooltip :content="row.userAgent" placement="top" v-if="row.userAgent">
              <span class="al-details">{{ row.userAgent }}</span>
            </el-tooltip>
            <span v-else>—</span>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="Thời gian" width="180">
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
import { ref, computed, onMounted } from "vue";
import { userLoginApi } from "../../api/userLogin.api";
import { 
  Document, Download, Filter, Search, 
  Refresh 
} from "@element-plus/icons-vue";

/* ===============================
   STATE
================================ */
const logs = ref([]);
const loading = ref(false);

const page = ref(1);
const size = ref(10);
const total = ref(0);
const totalPages = ref(1);

const filter = ref({
  username: "",
  success: "",
  ipAddress: "",
  fromDate: "",
  toDate: "",
});

/* ===============================
   LOAD DATA
================================ */
async function loadLogs() {
  loading.value = true;

  try {
    const res = await userLoginApi.filter(
      filter.value,
      page.value - 1,
      size.value,
    );

    logs.value = res.data.content;
    total.value = res.data.totalElements;
    totalPages.value = res.data.totalPages;
  } finally {
    loading.value = false;
  }
}

onMounted(loadLogs);

/* ===============================
   SEARCH
================================ */
function searchLogs() {
  page.value = 1;
  loadLogs();
}

function resetFilter() {
  filter.value = {
    username: "",
    success: "",
    ipAddress: "",
    fromDate: "",
    toDate: "",
  };
  searchLogs();
}

/* ===============================
   PAGINATION
================================ */
function handlePageChange(p) {
  page.value = p;
  loadLogs();
}

function handleSizeChange(val) {
  size.value = val;
  page.value = 1;
  loadLogs();
}

/* ===============================
   EXPORT CSV
================================ */
async function exportCsv() {
  const res = await userLoginApi.exportCsv(filter.value);
  const url = window.URL.createObjectURL(new Blob([res.data]));
  const a = document.createElement("a");
  a.href = url;
  a.download = "user_login_logs.csv";
  a.click();
}

/* ===============================
   HELPERS
================================ */
function fmtDate(date) {
  if (!date) return "";
  return new Date(date).toLocaleString();
}

const activeFilterCount = computed(
  () => Object.values(filter.value).filter((v) => v !== "").length,
);
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
