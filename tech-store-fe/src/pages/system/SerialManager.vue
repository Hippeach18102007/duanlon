<template>
  <div style="background:#f6f7f9; min-height:100vh; padding:32px 40px 60px;">
    <div style="max-width:1440px; margin:0 auto; display:flex; flex-direction:column; gap:20px;">

      <!-- Header -->
      <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:20px; margin-bottom:22px; flex-wrap:wrap;">
        <div>
          <div style="font-size:11px; font-weight:700; letter-spacing:0.08em; text-transform:uppercase; color:var(--el-text-color-secondary); margin-bottom:6px; display:flex; align-items:center; gap:5px;">
            <el-icon><Discount /></el-icon> Kho & Sản phẩm
          </div>
          <div style="font-size:28px; font-weight:800; letter-spacing:-0.03em; margin-bottom:4px;">Danh sách <span style="color:var(--el-color-primary);">Số Seri</span></div>
          <div style="font-size:13px; color:var(--el-text-color-secondary);">Quản lý và theo dõi chi tiết từng sản phẩm trong hệ thống</div>
        </div>
        <div style="display:flex; align-items:center; gap:8px; flex-shrink:0; padding-top:4px; flex-wrap:wrap;">
          <el-button plain :loading="loading" @click="load">
            <el-icon><Refresh /></el-icon> Tải lại
          </el-button>
        </div>
      </div>

      <!-- Stats -->
      <el-row :gutter="14">
        <el-col :xs="12" :sm="6" v-for="stat in stats" :key="stat.label">
          <el-card shadow="never" :body-style="{ padding:'16px' }" :style="`border-top:3px solid ${stat.color};`">
            <div style="display:flex; align-items:center; gap:12px;">
              <el-icon :size="22" :style="`color:${stat.color};`"><component :is="stat.icon" /></el-icon>
              <div>
                <div :style="`font-size:22px; font-weight:800; color:${stat.color};`">{{ stat.value }}</div>
                <div style="font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:0.05em; color:var(--el-text-color-secondary);">{{ stat.label }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- Filter -->
      <el-card shadow="never">
        <div style="display:flex; align-items:center; flex-wrap:wrap; gap:12px;">
          <!-- Product dropdown -->
          <el-select
            v-model="filterProductId"
            placeholder="Lọc theo sản phẩm…"
            clearable
            filterable
            style="width:280px;"
            :loading="productsLoading"
            @change="page=1; load()"
            @clear="page=1; load()"
          >
            <template #prefix><el-icon><Box /></el-icon></template>
            <el-option
              v-for="p in productOptions"
              :key="p.value"
              :label="p.label"
              :value="p.value"
            />
          </el-select>

          <!-- Serial / IMEI search -->
          <el-input
            v-model="keyword"
            placeholder="Số Seri / IMEI…"
            clearable
            style="width:240px;"
            @keyup.enter="load"
            @clear="clearFilters"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>

          <!-- Status filter -->
          <el-radio-group v-model="filterStatus" @change="page=1; load()">
            <el-radio-button v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </el-radio-button>
          </el-radio-group>

          <el-button
            link
            type="danger"
            v-if="filterProductId || keyword || filterStatus"
            @click="clearFilters"
          >Xóa bộ lọc</el-button>
        </div>
      </el-card>

      <!-- Table -->
      <el-card shadow="never" :body-style="{ padding:0 }">
        <div style="display:flex; align-items:center; justify-content:space-between; padding:12px 20px; border-bottom:1px solid var(--el-border-color-lighter); background:var(--el-fill-color-light);">
          <div style="display:flex; align-items:center; gap:8px; font-size:12.5px; font-weight:700; color:var(--el-text-color-secondary);">
            <el-icon><CreditCard /></el-icon>
            Danh sách Số Seri
            <el-tag v-if="filterProductId" size="small" type="primary" closable @close="filterProductId=null; page=1; load()">
              {{ productOptions.find(p => p.value === filterProductId)?.label }}
            </el-tag>
            <el-tag v-if="filterStatus" size="small" closable @close="filterStatus=''; load()">
              {{ statusOptions.find(o => o.value === filterStatus)?.label }}
            </el-tag>
          </div>
          <div>
            <span style="font-size:18px; font-weight:800;">{{ total }}</span>
            <span style="font-size:12px; color:var(--el-text-color-placeholder); font-weight:600; margin-left:4px;">seri</span>
          </div>
        </div>

        <el-table
          :data="pagedGroupRows"
          v-loading="loading"
          row-key="key"
          stripe
          size="small"
        >
          <!-- Expand column -->
          <el-table-column type="expand" width="44">
            <template #default="{ row }">
              <div style="padding:0 20px 12px 48px;">
                <el-table :data="row.serials" size="small" :show-header="true" style="width:100%;">
                  <el-table-column label="ID" width="70" align="center">
                    <template #default="{ row: s }">
                      <span style="font-family:monospace; font-size:11px; color:var(--el-color-primary);">#{{ s.id }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="Số Seri / IMEI" min-width="200">
                    <template #default="{ row: s }">
                      <div style="display:flex; align-items:center; gap:8px;">
                        <span style="font-family:monospace; font-weight:600; font-size:13px;">{{ s.serialNumber }}</span>
                        <el-tooltip content="Sao chép">
                          <el-button link size="small" @click="copySerial(s.serialNumber)">
                            <el-icon :size="13"><CopyDocument /></el-icon>
                          </el-button>
                        </el-tooltip>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column label="Thông tin" min-width="200">
                    <template #default="{ row: s }">
                      <span style="color:var(--el-text-color-secondary); font-size:12px; font-style:italic;">↳ {{ row.variantName }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="Trạng thái" width="140" align="center">
                    <template #default="{ row: s }">
                      <el-tag :type="statusTagType(s.status)" size="small" effect="plain" round>
                        <span :style="`width:6px;height:6px;border-radius:50%;background:currentColor;display:inline-block;vertical-align:middle;margin-right:4px;`"></span>
                        {{ formatStatus(s.status) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="Thao tác" width="200" align="center">
                    <template #default="{ row: s }">
                      <el-space :size="4">
                        <el-button
                          v-for="opt in statusActionOptions(s.status)"
                          :key="opt.value"
                          size="small"
                          :type="opt.elType"
                          plain
                          @click="handleStatusUpdate(s, opt.value)"
                        >{{ opt.label }}</el-button>
                      </el-space>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </template>
          </el-table-column>

          <!-- Group info -->
          <el-table-column label="Sản phẩm" min-width="280">
            <template #default="{ row }">
              <div>
                <div style="font-weight:700; font-size:13.5px;">{{ row.productName }}</div>
                <div style="font-size:12px; color:var(--el-text-color-secondary); margin-top:2px; display:flex; align-items:center; gap:4px;">
                  <el-icon :size="10"><ChromeFilled /></el-icon>
                  {{ row.variantName }}
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="Tổng" width="80" align="center">
            <template #default="{ row }">
              <el-tag effect="plain" size="small">{{ row.serials.length }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Trong kho" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.inStock" type="success" effect="plain" size="small">{{ row.inStock }}</el-tag>
              <span v-else style="color:var(--el-text-color-placeholder);">—</span>
            </template>
          </el-table-column>
          <el-table-column label="Đã bán" width="90" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.sold" type="primary" effect="plain" size="small">{{ row.sold }}</el-tag>
              <span v-else style="color:var(--el-text-color-placeholder);">—</span>
            </template>
          </el-table-column>
          <el-table-column label="Lỗi" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.faulty" type="danger" effect="plain" size="small">{{ row.faulty }}</el-tag>
              <span v-else style="color:var(--el-text-color-placeholder);">—</span>
            </template>
          </el-table-column>
        </el-table>

        <!-- Khi không lọc sản phẩm: phân trang theo nhóm sản phẩm -->
        <div v-if="!filterProductId && groupedRows.length > 0" style="display:flex; align-items:center; justify-content:space-between; padding:12px 20px; border-top:1px solid var(--el-border-color-lighter); background:var(--el-fill-color-light); flex-wrap:wrap; gap:10px;">
          <span style="font-size:12.5px; color:var(--el-text-color-secondary);">
            Nhóm {{ (groupPage - 1) * GROUP_SIZE + 1 }}–{{ Math.min(groupPage * GROUP_SIZE, groupedRows.length) }}
            <span style="color:var(--el-text-color-placeholder);"> / {{ groupedRows.length }} nhóm · {{ total }} seri</span>
          </span>
          <el-pagination
            v-model:current-page="groupPage"
            :page-size="GROUP_SIZE"
            :total="groupedRows.length"
            layout="prev, pager, next"
            background
            small
          />
        </div>
        <!-- Khi lọc theo sản phẩm: hiện tóm tắt -->
        <div v-else-if="filterProductId && groupedRows.length > 0" style="padding:10px 20px; border-top:1px solid var(--el-border-color-lighter); background:var(--el-fill-color-light); font-size:12.5px; color:var(--el-text-color-secondary);">
          <span style="font-weight:600;">{{ groupedRows.length }}</span> biến thể
          <span style="margin:0 6px; color:var(--el-border-color);">·</span>
          <span style="font-weight:600;">{{ rows.length }}</span> seri của sản phẩm này
        </div>
      </el-card>

    </div>
  </div>
</template>

<script setup>
import { Box, ChromeFilled, CopyDocument, CreditCard, Refresh, Search } from "@element-plus/icons-vue";
import { ref, onMounted, computed } from 'vue';
import { serialsApi } from '../../api/serials.api';
import { productsApi } from '../../api/products.api';
import { toast } from '../../ui/toast';

const loading = ref(false);
const rows    = ref([]);
const total   = ref(0);
const page    = ref(1);
const keyword = ref('');
const filterStatus = ref('');
const filterProductId = ref(null);

// ── Products dropdown ────────────────────────────────────────
const productsLoading = ref(false);
const productOptions = ref([]);

async function loadProducts() {
  productsLoading.value = true;
  try {
    const PAGE_SIZE = 100;
    // Lấy trang đầu để biết tổng số
    const first = await productsApi.list({ page: 0, size: PAGE_SIZE });
    const firstData = first.data?.data || first.data;
    const totalElements = firstData?.totalElements ?? 0;
    const totalPages    = firstData?.totalPages ?? Math.ceil(totalElements / PAGE_SIZE);
    let allItems = [...(firstData?.content ?? [])];

    // Fetch các trang còn lại song song
    if (totalPages > 1) {
      const rest = await Promise.all(
        Array.from({ length: totalPages - 1 }, (_, i) =>
          productsApi.list({ page: i + 1, size: PAGE_SIZE })
        )
      );
      rest.forEach(r => {
        const d = r.data?.data || r.data;
        allItems = allItems.concat(d?.content ?? []);
      });
    }

    productOptions.value = allItems.map(p => ({ value: p.id, label: p.name }));
  } catch (e) {
    console.error('Lỗi tải sản phẩm', e);
  } finally {
    productsLoading.value = false;
  }
}

// ── Frontend group pagination ────────────────────────────────
const GROUP_SIZE = 20;
const groupPage  = ref(1);

const globalInStock = ref(0);
const globalSold    = ref(0);
const globalFaulty  = ref(0);
const globalTotal   = computed(() => globalInStock.value + globalSold.value + globalFaulty.value);

const stats = computed(() => [
  { label: 'Tổng Seri',   value: globalTotal.value,  color: 'var(--el-color-primary)', icon: 'CreditCard'  },
  { label: 'Trong kho',   value: globalInStock.value, color: 'var(--el-color-success)', icon: 'ShieldCheck' },
  { label: 'Đã bán',      value: globalSold.value,    color: '#7c3aed',                 icon: 'ShoppingCart'},
  { label: 'Lỗi / Hỏng',  value: globalFaulty.value,  color: 'var(--el-color-danger)',  icon: 'Warning'     },
]);

const statusOptions = [
  { value: '',         label: 'Tất cả'    },
  { value: 'IN_STOCK', label: 'Trong kho' },
  { value: 'SOLD',     label: 'Đã bán'    },
  { value: 'FAULTY',   label: 'Lỗi / Hỏng'},
];

const selectedProductName = computed(() => {
  if (!filterProductId.value) return null;
  return productOptions.value.find(p => p.value === filterProductId.value)?.label ?? null;
});

const groupedRows = computed(() => {
  const map = {};
  const sourceRows = selectedProductName.value
    ? rows.value.filter(row => row.productName === selectedProductName.value)
    : rows.value;

  sourceRows.forEach(row => {
    const key = `${row.productName} | ${row.variantName}`;
    if (!map[key]) {
      map[key] = { key, productName: row.productName, variantName: row.variantName, inStock: 0, sold: 0, faulty: 0, serials: [] };
    }
    map[key].serials.push(row);
    if (row.status === 'IN_STOCK') map[key].inStock++;
    if (row.status === 'SOLD')     map[key].sold++;
    if (row.status === 'FAULTY')   map[key].faulty++;
  });
  return Object.values(map);
});

const pagedGroupRows = computed(() => {
  const start = (groupPage.value - 1) * GROUP_SIZE;
  return groupedRows.value.slice(start, start + GROUP_SIZE);
});

async function loadStats() {
  try {
    const kw = keyword.value || undefined;
    const [r1, r2, r3] = await Promise.all([
      serialsApi.list({ page: 0, size: 1, keyword: kw, status: 'IN_STOCK' }),
      serialsApi.list({ page: 0, size: 1, keyword: kw, status: 'SOLD' }),
      serialsApi.list({ page: 0, size: 1, keyword: kw, status: 'FAULTY' }),
    ]);
    globalInStock.value = r1.data?.data?.totalElements ?? r1.data?.totalElements ?? 0;
    globalSold.value    = r2.data?.data?.totalElements ?? r2.data?.totalElements ?? 0;
    globalFaulty.value  = r3.data?.data?.totalElements ?? r3.data?.totalElements ?? 0;
  } catch (e) { console.error('Lỗi lấy thống kê', e); }
}

async function load() {
  loading.value = true;
  try {
    if (filterProductId.value) {
      // ── Chọn sản phẩm: dùng API hierarchy đúng ──────────────
      // 1. Lấy danh sách biến thể của sản phẩm
      const varRes  = await productsApi.getVariants(filterProductId.value);
      const variants = varRes.data?.data ?? varRes.data ?? [];

      const productName = productOptions.value.find(p => p.value === filterProductId.value)?.label ?? '';

      // 2. Lấy serials song song cho từng biến thể
      const varSerialResults = await Promise.all(
        variants.map(v => productsApi.getSerials(v.id).then(r => ({ variant: v, serials: r.data?.data ?? r.data ?? [] })))
      );

      // 3. Gộp thành shape giống rows gốc
      const allRows = [];
      for (const { variant, serials } of varSerialResults) {
        // filter theo status nếu đang chọn
        const filtered = filterStatus.value
          ? serials.filter(s => s.status === filterStatus.value)
          : serials;
        // filter theo keyword nếu có
        const keywordLow = (keyword.value || '').toLowerCase();
        for (const s of filtered) {
          if (keywordLow && !s.serialNumber?.toLowerCase().includes(keywordLow)) continue;
          allRows.push({
            id:          s.id,
            serialNumber: s.serialNumber,
            status:      s.status,
            productName,
            variantName: variant.name ?? variant.variantName ?? '',
          });
        }
      }

      rows.value  = allRows;
      total.value = allRows.length;
    } else {
      // ── Không lọc: load toàn bộ trang serial rồi phân trang nhóm ở frontend ──
      const BATCH = 100;
      const firstRes  = await serialsApi.list({ page: 0, size: BATCH, keyword: keyword.value || undefined, status: filterStatus.value || undefined });
      const firstData = firstRes.data?.data || firstRes.data;
      const totalElements = firstData?.totalElements ?? 0;
      const totalPages    = firstData?.totalPages    ?? Math.ceil(totalElements / BATCH);
      let allContent = [...(firstData?.content ?? [])];

      if (totalPages > 1) {
        const rest = await Promise.all(
          Array.from({ length: totalPages - 1 }, (_, i) =>
            serialsApi.list({ page: i + 1, size: BATCH, keyword: keyword.value || undefined, status: filterStatus.value || undefined })
          )
        );
        rest.forEach(r => { const d = r.data?.data || r.data; allContent = allContent.concat(d?.content ?? []); });
      }

      rows.value  = allContent;
      total.value = totalElements;
    }
    loadStats();
  } catch (e) {
    console.error(e);
    toast('Lỗi tải dữ liệu Seri', 'error');
  } finally {
    loading.value = false;
  }
}

async function handleStatusUpdate(row, newStatus) {
  try {
    await serialsApi.updateStatus(row.id, newStatus);
    toast(`Đã đổi trạng thái Seri sang ${formatStatus(newStatus)}`, 'success');
    load();
  } catch {
    toast('Cập nhật thất bại', 'error');
  }
}

function clearFilters() { keyword.value = ''; filterStatus.value = ''; filterProductId.value = null; page.value = 1; groupPage.value = 1; load(); }

function copySerial(serial) {
  navigator.clipboard?.writeText(serial);
  toast('Đã sao chép Seri', 'success');
}

function statusTagType(status) {
  return ({ IN_STOCK: 'success', SOLD: 'primary', FAULTY: 'danger' })[status] ?? 'info';
}
function formatStatus(status) {
  return ({ IN_STOCK: 'Trong kho', SOLD: 'Đã bán', FAULTY: 'Lỗi / Hỏng' })[status] ?? status;
}
function statusActionOptions(currentStatus) {
  return [
    { value: 'IN_STOCK', label: 'Nhập kho',  elType: 'success' },
    { value: 'FAULTY',   label: 'Đánh lỗi',  elType: 'danger'  },
    { value: 'SOLD',     label: 'Đã bán',     elType: 'primary' },
  ].filter(o => o.value !== currentStatus);
}

onMounted(() => { loadProducts(); load(); });
</script>

<style></style>