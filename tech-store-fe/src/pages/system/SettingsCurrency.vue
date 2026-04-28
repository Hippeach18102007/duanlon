<template>
  <div class="sc-page">

    <!-- Header -->
    <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:20px; margin-bottom:22px; flex-wrap:wrap;">
      <div>
        <div style="font-size:11px; font-weight:700; letter-spacing:0.08em; text-transform:uppercase; color:var(--el-text-color-secondary); margin-bottom:6px; display:flex; align-items:center; gap:5px;">
            <el-icon><Setting /></el-icon> Cài đặt Hệ thống
          </div>
        <div style="font-size:28px; font-weight:800; letter-spacing:-0.03em; margin-bottom:4px;">Settings — Default Currency</div>
        <div style="font-size:13px; color:var(--el-text-color-secondary);">Cấu hình đơn vị tiền tệ mặc định cho toàn hệ thống</div>
      </div>
      <div style="display:flex; align-items:center; gap:8px; flex-shrink:0; padding-top:4px; flex-wrap:wrap;">
        <el-button plain :loading="loading" @click="load" :disabled="loading">
          <template #icon><el-icon><Refresh /></el-icon></template>
          Reload
        </el-button>
      </div>
    </div>

    <!-- ── Main card ── -->
    <el-card shadow="never">
      <template #header>Đơn vị tiền tệ mặc định</template>

      <el-space direction="vertical" fill style="width:100%">
        <el-row align="bottom" :gutter="20" wrap>
          <!-- Select -->
          <el-col :span="12" :xs="24">
            <el-form-item label="Currency Code" required>
              <template #label>
                <span class="field-label">Currency Code <span class="required">*</span></span>
              </template>
              <el-select v-model="currencyCode" style="width:100%">
                <el-option value="VND" label="🇻🇳  VND — Việt Nam Đồng" />
                <el-option value="USD" label="🇺🇸  USD — US Dollar" />
                <el-option value="EUR" label="🇪🇺  EUR — Euro" />
                <el-option value="JPY" label="🇯🇵  JPY — Japanese Yen" />
                <el-option value="THB" label="🇹🇭  THB — Thai Baht" />
              </el-select>
              <el-text type="info" size="small">Áp dụng cho toàn bộ trang web và tính giá</el-text>
            </el-form-item>
          </el-col>

          <!-- Preview -->
          <el-col v-if="currencyCode" flex="none">
            <el-descriptions :column="1" border size="small" class="preview-chip">
              <el-descriptions-item label="Hiện tại">
                <strong>{{ currencyCode }}</strong>
              </el-descriptions-item>
            </el-descriptions>
          </el-col>
        </el-row>

        <el-row justify="end">
          <el-button type="primary" :loading="saving" @click="save" :disabled="saving">
            <template #icon><el-icon><DocumentChecked /></el-icon></template>
            Lưu thay đổi
          </el-button>
        </el-row>
      </el-space>

      <el-divider />

      <!-- Raw JSON -->
      <el-descriptions :column="1" border>
        <template #title>
          <el-space>
            <span>Raw Response</span>
            <el-tag type="info" effect="plain" size="small">JSON</el-tag>
          </el-space>
        </template>
      </el-descriptions>
      <pre class="json-block">{{ raw ? JSON.stringify(raw, null, 2) : 'Chưa tải dữ liệu...' }}</pre>
    </el-card>

  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { InfoFilled, Refresh, DocumentChecked } from "@element-plus/icons-vue";
import { settingsApi } from "../../api/settings.api";
import { toast } from "../../ui/toast";

const loading = ref(false);
const saving  = ref(false);

const currencyCode = ref("VND");
const raw          = ref(null);

function pickCurrency(payload) {
  const root = payload?.data ?? payload;
  return root?.currencyCode ?? root?.data?.currencyCode ?? root?.data?.data?.currencyCode ?? null;
}

async function load() {
  loading.value = true;
  try {
    const res = await settingsApi.getDefaultCurrency();
    raw.value = res?.data ?? null;
    const c = pickCurrency(res?.data);
    if (c) currencyCode.value = String(c);
  } catch {
    toast("Failed to load default currency.", "error");
  } finally {
    loading.value = false;
  }
}

async function save() {
  if (!currencyCode.value) return toast("currencyCode is required.", "warning");
  saving.value = true;
  try {
    await settingsApi.setDefaultCurrency(currencyCode.value);
    toast("Default currency updated.", "success");
    await load();
  } catch {
    toast("Update failed.", "error");
  } finally {
    saving.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.sc-page {
  max-width: 800px;
  padding: 32px 40px 60px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.label-upper {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin-bottom: 6px;
}

.page-title { font-size: 26px; font-weight: 800; margin: 0 0 4px; }
.mt-sm      { margin-top: 6px; }

.field-label { text-transform: uppercase; letter-spacing: 0.05em; font-size: 11px; }
.required    { color: var(--el-color-danger); margin-left: 2px; }

.preview-chip { width: auto; }

.json-block {
  margin: 12px 0 0;
  font-family: "JetBrains Mono", "Courier New", monospace;
  font-size: 12px;
  line-height: 1.7;
  padding: 16px 20px;
  overflow-x: auto;
  white-space: pre;
}
</style>