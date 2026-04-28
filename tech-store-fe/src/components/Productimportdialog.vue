<template>
  <el-dialog
    :model-value="open"
    width="520px"
    align-center
    :close-on-click-modal="true"
    @close="close"
  >
    <template #header>
      <el-space :size="8" align="center">
        <el-tag type="primary" effect="plain" size="small" style="font-size: 10px; letter-spacing: 0.12em; text-transform: uppercase;">
          Nhập hàng loạt
        </el-tag>
        <el-text tag="div" style="font-size: 20px; font-weight: 800; letter-spacing: -0.02em;">Import Excel</el-text>
      </el-space>
    </template>

    <el-space direction="vertical" fill :size="20" style="width: 100%;">

      <!-- Steps -->
      <el-steps :active="selectedFile ? 2 : 1" finish-status="success" simple>
        <el-step title="Tải file mẫu" />
        <el-step title="Upload file" />
      </el-steps>

      <!-- Step 1: Download template -->
      <el-card shadow="never" :body-style="{ padding: '16px' }" style="background: var(--el-fill-color-lighter);">
        <el-row align="middle" :gutter="14">
          <el-col flex="none">
            <el-tag type="primary" effect="plain" style="font-size: 10px; font-weight: 800; letter-spacing: 0.04em;">01</el-tag>
          </el-col>
          <el-col flex="1">
            <el-text tag="div" style="font-size: 13px; font-weight: 700; margin-bottom: 3px;">Tải file mẫu</el-text>
            <el-text size="small" type="info">Điền đúng cột theo template — dữ liệu bắt đầu từ dòng 3</el-text>
          </el-col>
        </el-row>
        <el-button plain size="small" style="margin-top: 10px;" @click="downloadTemplate">
          <el-icon><Download /></el-icon>
          product_import_template.xlsx
        </el-button>
      </el-card>

      <!-- Step 2: Upload -->
      <el-card shadow="never" :body-style="{ padding: '16px' }" style="background: var(--el-fill-color-lighter);">
        <el-row align="middle" :gutter="14" style="margin-bottom: 10px;">
          <el-col flex="none">
            <el-tag type="primary" effect="plain" style="font-size: 10px; font-weight: 800; letter-spacing: 0.04em;">02</el-tag>
          </el-col>
          <el-col flex="1">
            <el-text tag="div" style="font-size: 13px; font-weight: 700; margin-bottom: 3px;">Upload file</el-text>
            <el-text size="small" type="info">Kéo thả hoặc click để chọn — chỉ hỗ trợ .xlsx · tối đa 5MB</el-text>
          </el-col>
        </el-row>

        <el-upload
          drag
          :auto-upload="false"
          accept=".xlsx,.xls"
          :limit="1"
          :on-change="onFileChange"
          :on-remove="clearFile"
          :file-list="fileList"
        >
          <el-icon :size="28" style="margin-bottom: 8px; color: var(--el-text-color-placeholder);"><UploadFilled /></el-icon>
          <el-text tag="div" size="small">Kéo file vào đây hoặc <el-text type="primary" size="small">click để chọn</el-text></el-text>
        </el-upload>
      </el-card>

      <!-- Result -->
      <transition name="result-slide">
        <div v-if="result">
          <!-- Stats -->
          <el-row :gutter="10" style="margin-bottom: 12px;">
            <el-col :span="8">
              <el-statistic title="Tổng dòng" :value="result.totalRows" />
            </el-col>
            <el-col :span="8">
              <el-statistic title="Thành công" :value="result.successCount">
                <template #number><el-text type="success" style="font-size: 28px; font-weight: 800;">{{ result.successCount }}</el-text></template>
              </el-statistic>
            </el-col>
            <el-col :span="8">
              <el-statistic title="Thất bại" :value="result.failCount">
                <template #number><el-text :type="result.failCount > 0 ? 'danger' : 'success'" style="font-size: 28px; font-weight: 800;">{{ result.failCount }}</el-text></template>
              </el-statistic>
            </el-col>
          </el-row>

          <el-alert
            v-if="result.failCount === 0"
            type="success"
            title="Tất cả sản phẩm đã được nhập thành công"
            show-icon
            :closable="false"
            style="margin-bottom: 10px;"
          />

          <el-collapse v-if="result.errors?.length">
            <el-collapse-item>
              <template #title>
                <el-text type="danger" size="small">
                  <el-icon><Warning /></el-icon>
                  {{ result.errors.length }} lỗi cần xem lại
                </el-text>
              </template>
              <el-scrollbar max-height="140px">
                <el-space direction="vertical" fill :size="4" style="padding: 4px 0; width: 100%;">
                  <el-text
                    v-for="(e, i) in result.errors"
                    :key="i"
                    size="small"
                    type="danger"
                    style="display: flex; align-items: flex-start; gap: 8px;"
                  >
                    <span style="width: 4px; height: 4px; border-radius: 50%; background: #ef4444; flex-shrink: 0; margin-top: 6px;"></span>
                    {{ e }}
                  </el-text>
                </el-space>
              </el-scrollbar>
            </el-collapse-item>
          </el-collapse>
        </div>
      </transition>

    </el-space>

    <template #footer>
      <el-row justify="end">
        <el-space :size="8">
          <el-button plain @click="close">Đóng</el-button>
          <el-button
            type="primary"
            :loading="loading"
            :disabled="!selectedFile"
            @click="submit"
          >
            <el-icon v-if="!loading"><Upload /></el-icon>
            {{ loading ? 'Đang nhập…' : 'Bắt đầu nhập' }}
          </el-button>
        </el-space>
      </el-row>
    </template>
  </el-dialog>
</template>

<script setup>
import { Download, Upload, UploadFilled, Warning } from "@element-plus/icons-vue";
import { ref, computed } from 'vue';
import { productsApi } from '../api/products.api.js';

const props = defineProps({ open: Boolean });
const emit  = defineEmits(['close', 'imported']);

const selectedFile = ref(null);
const loading      = ref(false);
const result       = ref(null);

const fileList = computed(() =>
  selectedFile.value ? [{ name: selectedFile.value.name, size: selectedFile.value.size }] : []
);

function close() { emit('close'); clearFile(); result.value = null; }
function clearFile() { selectedFile.value = null; }
function onFileChange(file) {
  if (file?.raw) { selectedFile.value = file.raw; result.value = null; }
}

function formatBytes(b) {
  if (b < 1024) return b + ' B';
  if (b < 1048576) return (b/1024).toFixed(1) + ' KB';
  return (b/1048576).toFixed(1) + ' MB';
}

async function downloadTemplate() {
  try {
    const res = await productsApi.downloadTemplate();
    const url = URL.createObjectURL(res.data);
    const a = document.createElement('a');
    a.href = url; a.download = 'product_import_template.xlsx'; a.click();
    URL.revokeObjectURL(url);
  } catch { alert('Không thể tải file mẫu'); }
}

async function submit() {
  if (!selectedFile.value) return;
  loading.value = true; result.value = null;
  try {
    const res = await productsApi.importFromExcel(selectedFile.value);
    result.value = res.data;
    if (res.data.successCount > 0) emit('imported');
  } catch (e) {
    result.value = {
      totalRows: 0, successCount: 0, failCount: 0,
      errors: ['Lỗi kết nối server: ' + (e.response?.data?.message || e.message)]
    };
  } finally { loading.value = false; }
}
</script>

<style scoped>
.result-slide-enter-active { transition: all 0.25s cubic-bezier(0.34,1.2,0.64,1); }
.result-slide-leave-active { transition: all 0.15s ease; }
.result-slide-enter-from   { opacity: 0; transform: translateY(10px); }
.result-slide-leave-to     { opacity: 0; }
</style>