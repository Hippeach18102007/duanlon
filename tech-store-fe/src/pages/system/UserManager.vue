<template>
  <div style="max-width: 1200px; margin: 0 auto; padding: 32px 24px 80px;">

    <!-- Header -->
    <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:20px; margin-bottom:22px; flex-wrap:wrap;">
      <div>
        <div style="font-size:11px; font-weight:700; letter-spacing:0.08em; text-transform:uppercase; color:var(--el-text-color-secondary); margin-bottom:6px; display:flex; align-items:center; gap:5px;">
            <el-icon><User /></el-icon> Hệ thống & Phân quyền
          </div>
        <div style="font-size:28px; font-weight:800; letter-spacing:-0.03em; margin-bottom:4px;">Quản lý người dùng</div>
        <div style="font-size:13px; color:var(--el-text-color-secondary);">{{ filteredRows.length }} / {{ rows.length }} tài khoản</div>
      </div>
      <div style="display:flex; align-items:center; gap:8px; flex-shrink:0; padding-top:4px; flex-wrap:wrap;">
        <el-button plain :loading="loading" @click="load">
          <el-icon><Refresh /></el-icon> Làm mới
        </el-button>
        <el-button type="primary" plain @click="openCreate">
          <el-icon><Plus /></el-icon> Thêm người dùng
        </el-button>
      </div>
    </div>

    <!-- Filters -->
    <el-card shadow="never" style="margin-bottom: 16px;">
      <el-row :gutter="12" align="middle" style="flex-wrap: wrap; gap: 8px 0;">
        <el-col :xs="24" :sm="10" :md="8">
          <el-input
            v-model="search"
            placeholder="Tìm theo username hoặc email..."
            clearable
            @input="currentPage = 1"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </el-col>
        <el-col :xs="12" :sm="6" :md="4">
          <el-select
            v-model="roleFilter"
            placeholder="Vai trò"
            clearable
            style="width: 100%;"
            @change="currentPage = 1"
          >
            <el-option label="ADMIN"     value="ADMIN" />
            <el-option label="SALES"     value="SALES" />
            <el-option label="INVENTORY" value="INVENTORY" />
            <el-option label="CUSTOMER"  value="CUSTOMER" />
          </el-select>
        </el-col>
        <el-col :xs="12" :sm="6" :md="4">
          <el-select
            v-model="activeFilter"
            placeholder="Trạng thái"
            clearable
            style="width: 100%;"
            @change="currentPage = 1"
          >
            <el-option label="Đang hoạt động" :value="true" />
            <el-option label="Vô hiệu hóa"    :value="false" />
          </el-select>
        </el-col>
        <el-col flex="auto">
          <el-button
            v-if="search || roleFilter !== null || activeFilter !== null"
            text
            type="info"
            @click="clearFilters"
          >
            <el-icon><Close /></el-icon> Xóa bộ lọc
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- Table -->
    <el-card shadow="never" :body-style="{ padding: 0 }">
      <el-skeleton v-if="loading" :rows="6" animated style="padding: 20px;" />

      <el-empty
        v-else-if="filteredRows.length === 0"
        :description="rows.length === 0 ? 'Không có người dùng nào' : 'Không tìm thấy kết quả phù hợp'"
        :image-size="80"
        style="padding: 48px 0;"
      />

      <el-table
        v-else
        :data="pagedRows"
        stripe
        style="width: 100%;"
        :header-cell-style="{ background: 'var(--el-fill-color-light)', color: 'var(--el-text-color-secondary)', fontSize: '11px', fontWeight: '700', textTransform: 'uppercase', letterSpacing: '0.06em' }"
      >
        <!-- ID -->
        <el-table-column label="ID" width="70" align="center">
          <template #default="{ row }">
            <el-text size="small" type="info" style="font-weight: 700;">#{{ row.id }}</el-text>
          </template>
        </el-table-column>

        <!-- Username -->
        <el-table-column label="Username" min-width="160">
          <template #default="{ row }">
            <el-space :size="10" align="center">
              <el-avatar :size="30" style="background: #eff6ff; color: #2563eb; font-size: 12px; font-weight: 800; flex-shrink: 0;">
                {{ row.username?.[0]?.toUpperCase() }}
              </el-avatar>
              <el-text style="font-weight: 600; color: #111827;">{{ row.username }}</el-text>
            </el-space>
          </template>
        </el-table-column>

        <!-- Email -->
        <el-table-column label="Email" min-width="200">
          <template #default="{ row }">
            <el-text type="info">{{ row.email }}</el-text>
          </template>
        </el-table-column>

        <!-- Role -->
        <el-table-column label="Vai trò" width="130">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" effect="plain" size="small" round>
              {{ row.role || '—' }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- Active -->
        <el-table-column label="Trạng thái" width="140" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.isActive ? 'success' : 'danger'"
              effect="plain"
              size="small"
              round
            >
              <span :style="`display:inline-block;width:6px;height:6px;border-radius:50%;background:${row.isActive ? 'var(--el-color-success)' : 'var(--el-color-danger)'};margin-right:5px;vertical-align:middle;`"></span>
              {{ row.isActive ? 'Hoạt động' : 'Vô hiệu hóa' }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- Created at -->
        <el-table-column label="Ngày tạo" width="160">
          <template #default="{ row }">
            <el-space direction="vertical" :size="2">
              <el-text size="small">{{ formatDate(row.createdAt) }}</el-text>
              <el-text size="small" type="info">{{ formatTime(row.createdAt) }}</el-text>
            </el-space>
          </template>
        </el-table-column>

        <!-- Updated at -->
        <el-table-column label="Cập nhật" width="160">
          <template #default="{ row }">
            <el-space direction="vertical" :size="2">
              <el-text size="small">{{ formatDate(row.updatedAt) }}</el-text>
              <el-text size="small" type="info">{{ formatTime(row.updatedAt) }}</el-text>
            </el-space>
          </template>
        </el-table-column>

        <!-- Actions -->
        <el-table-column label="Thao tác" width="130" align="right" fixed="right">
          <template #default="{ row }">
            <el-space :size="6">
              <el-tooltip content="Chỉnh sửa" placement="top">
                <el-button
                  size="small"
                  plain
                  @click="openEdit(row)"
                >
                  <el-icon><Edit /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="Xóa" placement="top">
                <el-button size="small" type="danger" plain @click="remove(row)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </el-tooltip>
            </el-space>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <el-row
        v-if="filteredRows.length > pageSize"
        justify="center"
        style="padding: 20px 0;"
      >
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="filteredRows.length"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="(s) => { pageSize = s; currentPage = 1; }"
        />
      </el-row>
    </el-card>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dlg.open"
      :title="dlg.mode === 'create' ? 'Thêm người dùng' : 'Cập nhật người dùng'"
      width="520px"
      align-center
    >
      <el-alert
        v-if="dlg.alert"
        type="error"
        :title="dlg.alert"
        show-icon
        :closable="false"
        style="margin-bottom: 16px;"
      />

      <el-form label-position="top" @submit.prevent="save">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="Username *">
              <el-input v-model="dlg.form.username" placeholder="Nhập username..." />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Email *">
              <el-input v-model="dlg.form.email" type="email" placeholder="Nhập email..." />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="dlg.mode === 'create'">
            <el-form-item label="Mật khẩu *">
              <el-input v-model="dlg.form.password" type="password" show-password placeholder="Nhập mật khẩu..." />
            </el-form-item>
          </el-col>
          <el-col :span="dlg.mode === 'create' ? 12 : 24">
            <el-form-item label="Vai trò *">
              <el-select v-model="dlg.form.role" style="width: 100%;">
                <el-option label="ADMIN"     value="ADMIN" />
                <el-option label="SALES"     value="SALES" />
                <el-option label="INVENTORY" value="INVENTORY" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24" v-if="dlg.mode === 'edit'">
            <el-form-item label="Trạng thái">
              <el-switch
                v-model="dlg.form.isActive"
                :active-text="dlg.form.isActive ? 'Đang hoạt động' : 'Vô hiệu hóa'"
                active-color="var(--el-color-success)"
                inactive-color="var(--el-color-danger)"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-space :size="10">
          <el-button @click="dlg.open = false">Hủy</el-button>
          <el-button type="primary" :loading="dlg.loading" @click="save">
            {{ dlg.mode === 'create' ? 'Tạo tài khoản' : 'Lưu thay đổi' }}
          </el-button>
        </el-space>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import {
  CircleCheck, CircleClose, Close, Delete, Edit,
  Plus, Refresh, Search,
} from "@element-plus/icons-vue";
import { computed, onMounted, reactive, ref } from "vue";
import { usersApi } from "../../api/users.api";
import { toast } from "../../ui/toast";
import { confirmModal } from "../../ui/confirm";

const loading = ref(false);
const rows    = ref([]);

// ── Filters ──────────────────────────────────────────────────────────────────
const search      = ref("");
const roleFilter  = ref(null);
const activeFilter = ref(null);
const currentPage = ref(1);
const pageSize    = ref(10);

function clearFilters() {
  search.value = "";
  roleFilter.value = null;
  activeFilter.value = null;
  currentPage.value = 1;
}

const filteredRows = computed(() => {
  let result = rows.value;

  if (search.value.trim()) {
    const q = search.value.trim().toLowerCase();
    result = result.filter(
      (r) =>
        r.username?.toLowerCase().includes(q) ||
        r.email?.toLowerCase().includes(q),
    );
  }

  if (roleFilter.value) {
    result = result.filter((r) => r.role === roleFilter.value);
  }

  if (activeFilter.value !== null) {
    result = result.filter((r) => r.isActive === activeFilter.value);
  }

  return result;
});

const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredRows.value.slice(start, start + pageSize.value);
});

// ── Data helpers ──────────────────────────────────────────────────────────────
function extractList(payload) {
  if (!payload) return [];
  if (Array.isArray(payload)) return payload;
  const root = payload?.data ?? payload;
  if (Array.isArray(root)) return root;
  for (const k of ["content", "items", "results", "rows", "list"]) {
    if (Array.isArray(root?.[k])) return root[k];
    if (Array.isArray(root?.data?.[k])) return root.data[k];
  }
  return [];
}

function normalize(list) {
  return (list || []).map((u) => {
    const rawRole = u?.role?.name ?? u?.role?.roleName ?? u?.role ?? "";
    return {
      id:        u?.id ?? u?.userId,
      username:  u?.username ?? u?.name ?? "",
      email:     u?.email ?? "",
      role:      String(rawRole).toUpperCase(),
      isActive:  u?.isActive ?? true,
      createdAt: u?.createdAt ?? null,
      updatedAt: u?.updatedAt ?? null,
      raw:       u,
    };
  });
}

function roleTagType(role) {
  return (
    { ADMIN: "danger", SALES: "success", INVENTORY: "primary", CUSTOMER: "warning" }[
      String(role || "").toUpperCase()
    ] ?? "info"
  );
}

function formatDate(dt) {
  if (!dt) return "—";
  return new Date(dt).toLocaleDateString("vi-VN", {
    day: "2-digit", month: "2-digit", year: "numeric",
  });
}
function formatTime(dt) {
  if (!dt) return "";
  return new Date(dt).toLocaleTimeString("vi-VN", {
    hour: "2-digit", minute: "2-digit",
  });
}

// ── Load ──────────────────────────────────────────────────────────────────────
async function load() {
  loading.value = true;
  try {
    const res = await usersApi.list();
    rows.value = normalize(extractList(res?.data));
  } catch {
    rows.value = [];
    toast("Không thể tải danh sách người dùng.", "error");
  } finally {
    loading.value = false;
  }
}

// ── Dialog ────────────────────────────────────────────────────────────────────
const dlg = reactive({
  open: false, mode: "create", loading: false, alert: "", id: null,
  originalRole: "",
  form: { username: "", email: "", password: "", role: "ADMIN", isActive: true },
});

function openCreate() {
  Object.assign(dlg, { open: true, mode: "create", alert: "", id: null, originalRole: "" });
  dlg.form = { username: "", email: "", password: "", role: "ADMIN", isActive: true };
}

function openEdit(row) {
  const rawRole = row?.raw?.role?.name ?? row?.raw?.role?.roleName ?? row?.role ?? "ADMIN";
  const role = String(rawRole).toUpperCase();
  Object.assign(dlg, { open: true, mode: "edit", alert: "", id: row?.id, originalRole: role });
  dlg.form = { username: row?.username || "", email: row?.email || "", password: "", role, isActive: row?.isActive ?? true };
}

async function save() {
  dlg.alert = "";
  if (!dlg.form.username || !dlg.form.email || !dlg.form.role) {
    dlg.alert = "Username, email và vai trò là bắt buộc.";
    return;
  }
  if (dlg.mode === "create" && !dlg.form.password) {
    dlg.alert = "Mật khẩu là bắt buộc khi tạo mới.";
    return;
  }

  dlg.loading = true;
  try {
    if (dlg.mode === "create") {
      await usersApi.add({
        username: dlg.form.username,
        email:    dlg.form.email,
        password: dlg.form.password,
        role:     dlg.form.role,
      });
      toast("Tạo tài khoản thành công!", "success");
    } else {
      await usersApi.update(dlg.id, {
        username: dlg.form.username,
        email:    dlg.form.email,
      });
      if (dlg.form.role !== dlg.originalRole) {
        await usersApi.updateRole(dlg.id, dlg.form.role);
      }
      toast("Cập nhật thành công!", "success");
    }
    dlg.open = false;
    await load();
  } catch (e) {
    const msg = e?.response?.data?.message || e?.message || "Lưu thất bại";
    dlg.alert = typeof msg === "string" ? msg : JSON.stringify(msg);
  } finally {
    dlg.loading = false;
  }
}

async function remove(row) {
  const ok = await confirmModal(`Xóa người dùng "${row?.username}" (#${row?.id})?`, "Xác nhận xóa", "Xóa", true);
  if (!ok) return;
  try {
    await usersApi.remove(row.id);
    toast("Đã xóa người dùng.", "success");
    await load();
  } catch {
    toast("Xóa thất bại.", "error");
  }
}

onMounted(load);
</script>

<style></style>