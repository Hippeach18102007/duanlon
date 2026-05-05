<template>
  <div class="scm-layout">
    <!-- Header -->
    <div class="scm-header">
      <div class="scm-header-left">
        <div class="scm-title">Khách hàng tại quầy</div>
        <div class="scm-sub">Tìm kiếm, xem thông tin và đăng ký khách mới</div>
      </div>
      <el-button type="primary" @click="openCreateModal('')" :icon="Plus">Đăng ký khách mới</el-button>
    </div>

    <!-- Body: 2 columns -->
    <div class="scm-body">
      <!-- LEFT: Search panel -->
      <div class="search-panel">
        <!-- Search input -->
        <div class="search-box">
          <el-input
            ref="searchInput"
            v-model="query"
            placeholder="Tên / SĐT / Email..."
            @keyup.enter="doSearch"
            @input="onQueryInput"
            clearable
            size="large"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
            <template #append>
              <el-button @click="doSearch" :loading="searchLoading">Tìm</el-button>
            </template>
          </el-input>
          <el-alert v-if="searchError" :title="searchError" type="error" show-icon :closable="false" class="mt-10" />
        </div>

        <!-- Results -->
        <div class="result-area">
          <div v-if="searchLoading" class="result-loading">
            <el-icon class="is-loading" :size="20"><Loading /></el-icon>
            <span>Đang tìm kiếm...</span>
          </div>

          <div v-else-if="searchDone">
            <div v-if="searchResults.length" class="result-meta">
              Tìm thấy <b>{{ searchResults.length }}</b> kết quả cho "{{ lastQuery }}"
            </div>

            <div v-if="!searchResults.length" class="result-empty">
              <el-empty :description="`Không tìm thấy &quot;${lastQuery}&quot;`" :image-size="60">
                <el-button type="primary" @click="openCreateModal(lastQuery)" plain>Đăng ký ngay</el-button>
              </el-empty>
            </div>

            <div v-else class="result-list">
              <div
                v-for="cust in searchResults"
                :key="cust.id"
                class="result-item"
                :class="{ 'is-active': selected?.id === cust.id }"
                @click="selectCustomer(cust)"
              >
                <el-avatar :size="36" :class="cust.customerType === 'VIP' ? 'vip-avatar' : 'normal-avatar'">
                  {{ initials(cust.fullName) }}
                </el-avatar>
                <div class="result-item-info">
                  <div class="result-item-name">{{ cust.fullName }}</div>
                  <div class="result-item-meta">
                    <span v-if="cust.phone">{{ cust.phone }}</span>
                    <span v-if="cust.phone && cust.email">·</span>
                    <span v-if="cust.email">{{ cust.email }}</span>
                  </div>
                </div>
                <el-tag v-if="cust.customerType === 'VIP'" type="warning" size="small" effect="dark">VIP</el-tag>
              </div>
            </div>
          </div>

          <div v-else class="result-empty">
            <el-empty description="Nhập thông tin để tìm kiếm" :image-size="60" />
          </div>
        </div>
      </div>

      <!-- RIGHT: Detail panel -->
      <div class="detail-panel">
        <div v-if="selected" class="detail-inner">
          <!-- Customer header -->
          <div class="detail-header">
            <el-avatar :size="52" :class="selected.customerType === 'VIP' ? 'vip-avatar' : 'normal-avatar'">
              {{ initials(selected.fullName) }}
            </el-avatar>
            <div class="detail-header-info">
              <div class="detail-name">{{ selected.fullName }}</div>
              <div class="detail-tags">
                <el-tag v-if="selected.customerType === 'VIP'" type="warning" effect="dark" size="small">VIP</el-tag>
                <el-tag v-else type="info" effect="plain" size="small">Thường</el-tag>
                <span class="detail-id">ID #{{ selected.id }}</span>
              </div>
            </div>
            <el-button :icon="Close" circle size="small" @click="selected = null" plain />
          </div>

          <!-- Stats -->
          <div class="stats-row">
            <div class="stat-card">
              <div class="stat-value">{{ (selected.loyaltyPoints || 0).toLocaleString() }}</div>
              <div class="stat-label">Điểm tích lũy</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">{{ selected.totalOrders || '—' }}</div>
              <div class="stat-label">Đơn hàng</div>
            </div>
            <div class="stat-card">
              <div class="stat-value highlight">{{ formatMoney(selected.totalSpent) }}</div>
              <div class="stat-label">Tổng chi tiêu</div>
            </div>
          </div>

          <!-- Info table -->
          <el-descriptions :column="1" border size="small" class="info-desc">
            <el-descriptions-item label="SĐT">{{ selected.phone || '—' }}</el-descriptions-item>
            <el-descriptions-item label="Email">{{ selected.email || '—' }}</el-descriptions-item>
            <el-descriptions-item label="Ngày sinh">{{ formatDate(selected.dateOfBirth) }}</el-descriptions-item>
            <el-descriptions-item label="Địa chỉ">{{ selected.address || '—' }}</el-descriptions-item>
            <el-descriptions-item label="Đăng ký">{{ formatDate(selected.createdAt) }}</el-descriptions-item>
          </el-descriptions>

          <el-alert v-if="selected.vipNote" :title="'Ghi chú VIP: ' + selected.vipNote" type="warning" :closable="false" show-icon class="mt-15" />

          <!-- Actions -->
          <div class="detail-actions">
            <el-button type="primary" size="large" @click="linkToPos" :loading="posLinking">
              Dùng cho đơn POS
            </el-button>
            <el-button v-if="selected.phone" @click="copyPhone(selected.phone)" plain>
              <el-icon><CopyDocument /></el-icon> Copy SĐT
            </el-button>
          </div>
        </div>

        <div v-else class="detail-empty">
          <el-icon :size="52" color="#dcdfe6"><User /></el-icon>
          <p>Chọn khách hàng từ kết quả tìm kiếm</p>
        </div>
      </div>
    </div>

    <!-- Create Dialog -->
    <el-dialog v-model="showModal" title="Đăng ký khách hàng mới" width="500px">
      <el-form :model="form" label-position="top">
        <el-form-item label="Họ và tên" required :error="v.fullName">
          <el-input v-model="form.fullName" placeholder="Nguyễn Văn A" @input="v.fullName = ''" />
        </el-form-item>
        <el-row :gutter="15">
          <el-col :span="12">
            <el-form-item label="Số điện thoại" required :error="v.phone">
              <el-input v-model="form.phone" placeholder="09xxxxxxxx" @input="v.phone = ''" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Email" required :error="v.email">
              <el-input v-model="form.email" placeholder="example@email.com" @input="v.email = ''" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-collapse v-model="activeCollapse">
          <el-collapse-item title="Thông tin bổ sung" name="optional">
            <el-row :gutter="15">
              <el-col :span="12">
                <el-form-item label="Ngày sinh">
                  <el-date-picker v-model="form.dateOfBirth" type="date" placeholder="Chọn ngày" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="Giới tính">
                  <el-select v-model="form.gender" placeholder="Chọn" style="width: 100%">
                    <el-option label="Nam" value="MALE" />
                    <el-option label="Nữ" value="FEMALE" />
                    <el-option label="Khác" value="OTHER" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="Địa chỉ">
              <el-input v-model="form.address" placeholder="Số nhà, đường..." />
            </el-form-item>
          </el-collapse-item>
        </el-collapse>

        <el-alert v-if="formError" :title="formError" type="error" show-icon :closable="false" class="mt-15" />
        <el-text type="info" size="small" class="mt-10" style="display:block">
          Mật khẩu mặc định sẽ là số điện thoại của khách hàng.
        </el-text>
      </el-form>
      <template #footer>
        <el-button @click="showModal = false" plain>Hủy</el-button>
        <el-button type="primary" :loading="createLoading" @click="doCreate">Đăng ký</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import { Search, Plus, Close, Loading, User, CopyDocument } from "@element-plus/icons-vue";
import { customersApi } from "../../api/customers.api";
import { toast } from "../../ui/toast";

const router = useRouter();
const searchInput   = ref(null);
const query         = ref("");
const lastQuery     = ref("");
const searchLoading = ref(false);
const searchError   = ref("");
const searchResults = ref([]);
const searchDone    = ref(false);
const selected      = ref(null);
const showModal     = ref(false);
const createLoading = ref(false);
const formError     = ref("");
const posLinking    = ref(false);
const activeCollapse= ref([]);

const form = reactive({ fullName: "", phone: "", email: "", dateOfBirth: "", gender: "", address: "" });
const v    = reactive({ fullName: "", phone: "", email: "" });

function initials(name = "") { return name.split(" ").map(w => w[0]).filter(Boolean).slice(-2).join("").toUpperCase() || "KH"; }
function formatDate(str) { return str ? new Date(str).toLocaleDateString("vi-VN") : "—"; }
function formatMoney(val) { return val ? new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(val) : "—"; }

function onQueryInput() { if (!query.value.trim()) { searchDone.value = false; searchResults.value = []; } }

async function doSearch() {
  const q = query.value.trim();
  if (!q) return;
  lastQuery.value = q;
  searchLoading.value = true;
  searchError.value = "";
  searchDone.value = false;
  selected.value = null;
  try {
    const res = await customersApi.listAll();
    const all = res.data?.data || res.data || [];
    const lower = q.toLowerCase();
    searchResults.value = all.filter(c =>
      (c.fullName || "").toLowerCase().includes(lower) ||
      (c.phone    || "").includes(q) ||
      (c.email    || "").toLowerCase().includes(lower)
    );
    searchDone.value = true;
    if (!searchResults.value.length) toast(`Không tìm thấy "${q}"`, "warning");
  } catch { searchError.value = "Lỗi tìm kiếm."; }
  finally { searchLoading.value = false; }
}

function selectCustomer(cust) { selected.value = cust; }

function openCreateModal(prefill = "") {
  resetForm();
  if (prefill) {
    if (/^[0-9+\s-]{7,}$/.test(prefill)) form.phone = prefill;
    else if (prefill.includes("@")) form.email = prefill;
    else form.fullName = prefill;
  }
  showModal.value = true;
}
function resetForm() {
  Object.assign(form, { fullName: "", phone: "", email: "", dateOfBirth: "", gender: "", address: "" });
  Object.assign(v, { fullName: "", phone: "", email: "" });
  formError.value = ""; activeCollapse.value = [];
}

async function doCreate() {
  let ok = true;
  if (!form.fullName.trim()) { v.fullName = "Thiếu họ tên"; ok = false; }
  if (!form.phone.trim())    { v.phone    = "Thiếu SĐT";   ok = false; }
  if (!form.email.trim())    { v.email    = "Thiếu email";  ok = false; }
  if (!ok) return;
  createLoading.value = true;
  try {
    const res = await customersApi.create({ ...form, password: form.phone });
    selected.value = res.data?.data || res.data;
    showModal.value = false;
    toast("Đăng ký thành công", "success");
    if (searchDone.value) searchResults.value.unshift(selected.value);
  } catch (e) { formError.value = e.response?.data?.message || "Lỗi khi đăng ký."; }
  finally { createLoading.value = false; }
}

async function linkToPos() {
  if (!selected.value) return;
  posLinking.value = true;
  sessionStorage.setItem("pos_customer", JSON.stringify(selected.value));
  toast("Đã gán khách hàng, đang chuyển sang POS...", "success");
  setTimeout(() => router.push("/sales/pos"), 600);
}

async function copyPhone(p) {
  await navigator.clipboard.writeText(p);
  toast("Đã copy SĐT", "success");
}
</script>

<style scoped>
/* ── Layout ── */
.scm-layout { display: flex; flex-direction: column; height: 100%; min-height: 0; background: #f5f7fa; overflow: hidden; }

.scm-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 24px; background: #fff; border-bottom: 1px solid #ebeef5;
  flex-shrink: 0;
}
.scm-title { font-size: 16px; font-weight: 700; color: #1a1a2e; }
.scm-sub   { font-size: 12px; color: #909399; margin-top: 2px; }

.scm-body { display: flex; flex: 1; min-height: 0; overflow: hidden; }

/* ── Search panel ── */
.search-panel {
  width: 360px; flex-shrink: 0;
  background: #fff; border-right: 1px solid #ebeef5;
  display: flex; flex-direction: column; min-height: 0; overflow: hidden;
}
.search-box { padding: 16px; border-bottom: 1px solid #f5f7fa; flex-shrink: 0; }
.result-area { flex: 1; min-height: 0; overflow-y: auto; }
.result-meta { padding: 10px 16px; font-size: 12px; color: #909399; border-bottom: 1px solid #f5f7fa; }
.result-empty { padding: 30px 16px; }
.result-loading { display: flex; align-items: center; gap: 8px; padding: 20px 16px; color: #909399; font-size: 13px; }
.result-list { }

.result-item {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 16px; cursor: pointer;
  border-bottom: 1px solid #f5f7fa;
  transition: background 0.12s;
}
.result-item:hover  { background: #fafafa; }
.result-item.is-active { background: #fdf6ec; border-left: 3px solid #e6a23c; }
.result-item-info { flex: 1; min-width: 0; }
.result-item-name { font-size: 13px; font-weight: 600; color: #303133; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.result-item-meta { font-size: 11px; color: #909399; margin-top: 2px; display: flex; gap: 4px; }

/* ── Detail panel ── */
.detail-panel { flex: 1; min-width: 0; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.detail-inner { padding: 20px; overflow-y: auto; flex: 1; min-height: 0; }
.detail-empty { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #c0c4cc; font-size: 14px; gap: 12px; }

.detail-header { display: flex; align-items: center; gap: 14px; margin-bottom: 20px; }
.detail-header-info { flex: 1; }
.detail-name { font-size: 18px; font-weight: 700; color: #1a1a2e; }
.detail-tags { display: flex; align-items: center; gap: 8px; margin-top: 6px; }
.detail-id   { font-size: 12px; color: #909399; }

/* ── Stats ── */
.stats-row {
  display: flex; gap: 12px; margin-bottom: 20px;
  padding: 16px; background: #f8faff;
  border-radius: 8px; border: 1px solid #e8f0fe;
}
.stat-card { flex: 1; text-align: center; }
.stat-value { font-size: 18px; font-weight: 700; color: #303133; }
.stat-value.highlight { color: #f56c6c; font-size: 15px; }
.stat-label { font-size: 11px; color: #909399; margin-top: 4px; }

.info-desc { margin-bottom: 16px; }
.detail-actions { display: flex; gap: 10px; margin-top: 20px; padding-top: 16px; border-top: 1px solid #f0f0f0; }

/* ── Avatars ── */
.vip-avatar    { background: #e6a23c !important; color: #fff !important; }
.normal-avatar { background: #409eff !important; color: #fff !important; }

.mt-10 { margin-top: 10px; }
.mt-15 { margin-top: 15px; }
</style>