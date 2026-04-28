<template>
  <div class="pa-page">
    <div class="pa-inner">

      <!-- Header -->
      <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:20px; margin-bottom:22px; flex-wrap:wrap;">
        <div>
          <div style="font-size:11px; font-weight:700; letter-spacing:0.08em; text-transform:uppercase; color:var(--el-text-color-secondary); margin-bottom:6px; display:flex; align-items:center; gap:5px;">
            <el-icon><DataAnalysis /></el-icon> Analytics Overview
          </div>
          <div style="font-size:28px; font-weight:800; letter-spacing:-0.03em; margin-bottom:4px;">Báo Cáo Tổng Quan Sản Phẩm</div>
          <div style="font-size:13px; color:var(--el-text-color-secondary);">Dữ liệu được cập nhật theo thời gian thực từ hệ thống kho hàng và đơn hàng</div>
        </div>
        <div style="display:flex; align-items:center; gap:8px; flex-shrink:0; padding-top:4px; flex-wrap:wrap;">
          <el-button plain :loading="loading" @click="loadData" :disabled="loading">
            <template #icon><el-icon><Refresh /></el-icon></template>
            {{ loading ? 'Đang tải…' : 'Làm mới' }}
          </el-button>
          <el-button type="primary" :loading="exporting" @click="downloadExcel" :disabled="exporting">
            <template #icon><el-icon><Download /></el-icon></template>
            {{ exporting ? 'Đang xuất…' : 'Xuất Excel' }}
          </el-button>
        </div>
      </div>

      <!-- ── Stat cards ── -->
      <el-row :gutter="14">
        <el-col :span="6" :xs="12">
          <el-card shadow="never">
            <el-statistic title="Sản phẩm đang kinh doanh" :value="stats.totalActiveProducts">
              <template #prefix><el-icon><Grid /></el-icon></template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :span="6" :xs="12">
          <el-card shadow="never">
            <el-statistic title="Tổng số máy trong kho" :value="stats.totalSerialsInStock">
              <template #prefix><el-icon><Box /></el-icon></template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :span="6" :xs="12">
          <el-card shadow="never">
            <el-statistic title="Biến thể báo hết hàng" :value="stats.outOfStockVariants">
              <template #prefix><el-icon><Warning /></el-icon></template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :span="6" :xs="12">
          <el-card shadow="never">
            <el-statistic title="Máy bị lỗi (Faulty)" :value="stats.totalSerialsFaulty">
              <template #prefix><el-icon><CircleClose /></el-icon></template>
            </el-statistic>
          </el-card>
        </el-col>
      </el-row>

      <!-- ── Top selling table ── -->
      <el-card shadow="never">
        <template #header>
          <el-row justify="space-between" align="middle">
            <el-space>
              <el-icon><Trophy /></el-icon>
              <span>Top 10 Sản Phẩm Bán Chạy Nhất</span>
            </el-space>
            <el-tag type="info" effect="plain" size="small" round>Xếp hạng theo số lượng</el-tag>
          </el-row>
        </template>

        <el-table
          :data="stats.topSellingProducts"
          v-loading="loading"
          border
          stripe
        >
          <!-- Rank -->
          <el-table-column label="Hạng" width="80" align="center">
            <template #default="{ $index }">
              <span v-if="$index === 0" class="rank-emoji">🥇</span>
              <span v-else-if="$index === 1" class="rank-emoji">🥈</span>
              <span v-else-if="$index === 2" class="rank-emoji">🥉</span>
              <el-text v-else type="info">{{ $index + 1 }}</el-text>
            </template>
          </el-table-column>

          <!-- Product name -->
          <el-table-column label="Tên sản phẩm" min-width="320">
            <template #default="{ row }">
              <strong>{{ row.productName }}</strong>
            </template>
          </el-table-column>

          <!-- Sold -->
          <el-table-column label="Đã bán" width="180" align="center">
            <template #default="{ row }">
              <el-tag type="success" effect="plain">
                <strong>{{ row.totalSold }}</strong> chiếc
              </el-tag>
            </template>
          </el-table-column>

          <!-- Revenue -->
          <el-table-column label="Tổng doanh thu" width="240" align="right">
            <template #default="{ row }">
              <el-text type="success">{{ formatCurrency(row.totalRevenue) }}</el-text>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="!loading && stats.topSellingProducts.length === 0" description="Chưa có dữ liệu thống kê" :image-size="60" />
      </el-card>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { DataAnalysis, Refresh, Download, Grid, Box, Warning, CircleClose, Trophy } from "@element-plus/icons-vue";
import { productsApi } from '../../api/products.api';
import { toast }       from '../../ui/toast';

const loading  = ref(false);
const exporting = ref(false);

const stats = ref({
  totalActiveProducts: 0,
  totalSerialsInStock: 0,
  outOfStockVariants:  0,
  totalSerialsFaulty:  0,
  topSellingProducts:  [],
});

function formatCurrency(val) {
  if (!val) return '0 ₫';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
}

async function loadData() {
  loading.value = true;
  try {
    const res = await productsApi.getDashboardStats();
    stats.value = res.data;
  } catch {
    toast('Không thể tải dữ liệu thống kê', 'error');
  } finally {
    loading.value = false;
  }
}

async function downloadExcel() {
  exporting.value = true;
  try {
    const res  = await productsApi.exportReport();
    const url  = window.URL.createObjectURL(new Blob([res.data]));
    const link = document.createElement('a');
    link.href  = url;
    link.setAttribute('download', `Bao_Cao_San_Pham_${Date.now()}.xlsx`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
    toast('Xuất báo cáo Excel thành công!', 'success');
  } catch {
    toast('Lỗi khi xuất file Excel', 'error');
  } finally {
    exporting.value = false;
  }
}

onMounted(loadData);
</script>

<style scoped>
.pa-page  { min-height: 100vh; padding: 32px 40px 60px; }
.pa-inner { max-width: 1440px; margin: 0 auto; display: flex; flex-direction: column; gap: 20px; }

.label-upper {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin-bottom: 6px;
}

.page-title    { font-size: 28px; font-weight: 800; margin: 0 0 4px; }
.header-actions { padding-top: 6px; }

.rank-emoji { font-size: 20px; }

@media (max-width: 900px) {
  .pa-page { padding: 20px 16px 40px; }
}
</style>