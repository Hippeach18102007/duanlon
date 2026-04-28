<template>
  <el-container class="scm-container">
    <el-header class="header">
      <el-row justify="space-between" align="middle" style="width: 100%">
        <el-space direction="vertical" align="start" :size="0">
          <el-text type="warning" tag="b" size="small">QUẢN LÝ KHÁCH HÀNG</el-text>
          <el-text tag="h2" style="margin: 0">Khách hàng tại quầy</el-text>
          <el-text type="info" size="small">Tìm khách hàng hiện có hoặc đăng ký mới khi mua tại cửa hàng</el-text>
        </el-space>
        <el-button type="primary" @click="openCreateModal('')" :icon="Plus" plain>Đăng ký khách mới</el-button>
      </el-row>
    </el-header>

    <el-main>
      <el-row :gutter="20" class="h-full">
        <!-- Left: Search & Results -->
        <el-col :span="10" class="flex-col">
          <el-card shadow="never" class="mb-20">
            <template #header><el-text tag="b">Tìm khách hàng</el-text></template>
            <el-input
              ref="searchInput"
              v-model="query"
              placeholder="Tên / SĐT / Email..."
              @keyup.enter="doSearch"
              @input="onQueryInput"
              clearable
            >
              <template #prefix><el-icon><Search /></el-icon></template>
              <template #append>
                <el-button @click="doSearch" :loading="searchLoading" plain>Tìm</el-button>
              </template>
            </el-input>
            <el-alert v-if="searchError" :title="searchError" type="error" show-icon :closable="false" class="mt-10" />
          </el-card>

          <el-card shadow="never" class="flex-1 scroll-y">
            <div v-if="searchDone">
              <el-text v-if="searchResults.length" size="small" type="info" class="mb-10" block>
                Tìm thấy {{ searchResults.length }} kết quả cho "{{ lastQuery }}"
              </el-text>
              
              <div v-if="!searchResults.length && !searchLoading" class="text-center py-20">
                <el-empty :description="'Không tìm thấy khách hàng: ' + lastQuery">
                  <el-button type="primary" @click="openCreateModal(lastQuery)" plain>Đăng ký ngay</el-button>
                </el-empty>
              </div>

              <div v-else class="result-list">
                <div
                  v-for="cust in searchResults"
                  :key="cust.id"
                  class="result-item"
                  :class="{ 'active': selected?.id === cust.id }"
                  @click="selectCustomer(cust)"
                >
                  <el-row align="middle" :gutter="12">
                    <el-col :span="4">
                      <el-avatar :size="40" :class="{ 'vip-avatar': cust.customerType === 'VIP' }">
                        {{ initials(cust.fullName) }}
                      </el-avatar>
                    </el-col>
                    <el-col :span="14">
                      <el-text tag="b" block>{{ cust.fullName }}</el-text>
                      <el-space :size="10">
                        <el-text size="small" type="info" v-if="cust.phone">{{ cust.phone }}</el-text>
                        <el-text size="small" type="info" v-if="cust.email">{{ cust.email }}</el-text>
                      </el-space>
                    </el-col>
                    <el-col :span="6" class="text-right">
                      <el-tag v-if="cust.customerType === 'VIP'" type="warning" size="small" plain>VIP</el-tag>
                      <el-text size="small" type="info" block>{{ cust.loyaltyPoints || 0 }} pts</el-text>
                    </el-col>
                  </el-row>
                </div>
              </div>
            </div>
            <el-empty v-else description="Nhập thông tin để tìm kiếm" />
          </el-card>
        </el-col>

        <!-- Right: Detail -->
        <el-col :span="14">
          <el-card v-if="selected" shadow="never" class="h-full">
            <template #header>
              <el-row justify="space-between" align="middle">
                <el-space>
                  <el-avatar :size="50" :class="{ 'vip-avatar': selected.customerType === 'VIP' }">
                    {{ initials(selected.fullName) }}
                  </el-avatar>
                  <el-space direction="vertical" align="start" :size="0">
                    <el-text tag="b" size="large">{{ selected.fullName }}</el-text>
                    <el-space>
                      <el-tag v-if="selected.customerType === 'VIP'" type="warning" plain>VIP</el-tag>
                      <el-tag v-else type="info" plain>Thường</el-tag>
                      <el-text type="info" size="small">ID #{{ selected.id }}</el-text>
                    </el-space>
                  </el-space>
                </el-space>
                <el-button :icon="Close" circle @click="selected = null" plain />
              </el-row>
            </template>

            <el-row class="stats-row mb-20" :gutter="20">
              <el-col :span="8">
                <div class="stat-item">
                  <el-text tag="b" size="large">{{ (selected.loyaltyPoints || 0).toLocaleString() }}</el-text>
                  <el-text size="small" type="info">Điểm tích lũy</el-text>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="stat-item">
                  <el-text tag="b" size="large">{{ selected.totalOrders || '—' }}</el-text>
                  <el-text size="small" type="info">Đơn hàng</el-text>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="stat-item">
                  <el-text tag="b" size="large">{{ formatMoney(selected.totalSpent) }}</el-text>
                  <el-text size="small" type="info">Tổng chi tiêu</el-text>
                </div>
              </el-col>
            </el-row>

            <el-descriptions :column="1" border>
              <el-descriptions-item label="Số điện thoại">{{ selected.phone || '—' }}</el-descriptions-item>
              <el-descriptions-item label="Email">{{ selected.email || '—' }}</el-descriptions-item>
              <el-descriptions-item label="Ngày sinh">{{ formatDate(selected.dateOfBirth) }}</el-descriptions-item>
              <el-descriptions-item label="Địa chỉ">{{ selected.address || '—' }}</el-descriptions-item>
              <el-descriptions-item label="Ngày đăng ký">{{ formatDate(selected.createdAt) }}</el-descriptions-item>
            </el-descriptions>

            <el-alert v-if="selected.vipNote" :title="'Ghi chú VIP: ' + selected.vipNote" type="warning" :closable="false" class="mt-20" show-icon />

            <div class="mt-20">
              <el-space>
                <el-button type="primary" size="large" @click="linkToPos" :loading="posLinking" plain>Dùng cho đơn POS</el-button>
                <el-button v-if="selected.phone" @click="copyPhone(selected.phone)" plain>Copy SĐT</el-button>
              </el-space>
            </div>
          </el-card>
          <el-card v-else shadow="never" class="h-full flex-center text-center">
            <el-empty description="Chọn khách hàng từ kết quả tìm kiếm" />
          </el-card>
        </el-col>
      </el-row>
    </el-main>

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
                  <el-select v-model="form.gender" placeholder="Chọn giới tính" style="width: 100%">
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

        <el-alert v-if="formError" :title="formError" type="error" show-icon :closable="false" class="mt-20" />
        <el-text type="info" size="small" class="mt-10" block>
          Mật khẩu mặc định sẽ là số điện thoại của khách hàng.
        </el-text>
      </el-form>

      <template #footer>
        <el-button @click="showModal = false" plain>Hủy</el-button>
        <el-button type="primary" :loading="createLoading" @click="doCreate" plain>Đăng ký</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import { Search, Plus, Close } from "@element-plus/icons-vue";
import { customersApi } from "../../api/customers.api";
import { toast } from "../../ui/toast";

const router = useRouter();
const query = ref("");
const lastQuery = ref("");
const searchLoading = ref(false);
const searchError = ref("");
const searchResults = ref([]);
const searchDone = ref(false);
const selected = ref(null);
const showModal = ref(false);
const createLoading = ref(false);
const formError = ref("");
const posLinking = ref(false);
const activeCollapse = ref([]);

const form = reactive({ fullName: "", phone: "", email: "", dateOfBirth: "", gender: "", address: "" });
const v = reactive({ fullName: "", phone: "", email: "" });

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
      (c.phone || "").includes(q) || 
      (c.email || "").toLowerCase().includes(lower)
    );
    searchDone.value = true;
    if (!searchResults.value.length) toast(`Không tìm thấy "${q}"`, "warning");
  } catch (e) {
    searchError.value = "Lỗi tìm kiếm.";
  } finally {
    searchLoading.value = false;
  }
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
  if (!form.phone.trim()) { v.phone = "Thiếu SĐT"; ok = false; }
  if (!form.email.trim()) { v.email = "Thiếu email"; ok = false; }
  if (!ok) return;

  createLoading.value = true;
  try {
    const payload = { ...form, password: form.phone };
    const res = await customersApi.create(payload);
    selected.value = res.data?.data || res.data;
    showModal.value = false;
    toast("Đăng ký thành công", "success");
    if (searchDone.value) searchResults.value.unshift(selected.value);
  } catch (e) {
    formError.value = e.response?.data?.message || "Lỗi khi đăng ký.";
  } finally {
    createLoading.value = false;
  }
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
.scm-container { padding: 0; background: #f5f7fa; min-height: 100vh; }
.header { height: auto; padding: 10px 0; margin-bottom: 20px; }
.h-full { height: calc(100vh - 120px); }
.flex-col { display: flex; flex-direction: column; }
.flex-1 { flex: 1; }
.scroll-y { overflow-y: auto; }
.mb-20 { margin-bottom: 20px; }
.mb-10 { margin-bottom: 10px; }
.mt-10 { margin-top: 10px; }
.mt-20 { margin-top: 20px; }
.py-20 { padding: 20px 0; }
.result-item { padding: 15px; border-bottom: 1px solid #f2f6fc; cursor: pointer; transition: background 0.2s; }
.result-item:hover { background: #fdf6ec; }
.result-item.active { background: #fdf6ec; border-left: 4px solid #e6a23c; }
.vip-avatar { background: #e6a23c; color: #fff; }
.stats-row { border-bottom: 1px solid #ebeef5; padding-bottom: 20px; }
.stat-item { text-align: center; }
.stat-item b { display: block; font-size: 18px; }
.flex-center { display: flex; align-items: center; justify-content: center; }
.text-right { text-align: right; }
.text-center { text-align: center; }
</style>