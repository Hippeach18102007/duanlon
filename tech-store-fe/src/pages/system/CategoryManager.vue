<template>
  <div style="background:#f6f7f9; min-height:100vh; padding:32px 40px 60px;">

    <!-- Header -->
    <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:20px; margin-bottom:22px; flex-wrap:wrap;">
      <div>
        <div style="font-size:11px; font-weight:700; letter-spacing:0.08em; text-transform:uppercase; color:var(--el-text-color-secondary); margin-bottom:6px; display:flex; align-items:center; gap:5px;">
            <el-icon><Grid /></el-icon> Kho & Sản phẩm
          </div>
        <div style="font-size:28px; font-weight:800; letter-spacing:-0.03em; margin-bottom:4px;">Quản lý danh mục</div>
        <div style="font-size:13px; color:var(--el-text-color-secondary);">Quản lý cấu trúc danh mục sản phẩm</div>
      </div>
      <div style="display:flex; align-items:center; gap:8px; flex-shrink:0; padding-top:4px; flex-wrap:wrap;">
        <el-button plain :loading="loading" @click="load">
          <el-icon><Refresh /></el-icon> Tải lại
        </el-button>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon> Thêm danh mục
        </el-button>
      </div>
    </div>

    <!-- Table Card -->
    <el-card shadow="never" :body-style="{ padding:0 }">
      <!-- Toolbar -->
      <div style="display:flex; align-items:center; justify-content:space-between; padding:12px 20px; border-bottom:1px solid var(--el-border-color-lighter); background:var(--el-fill-color-light);">
        <div style="display:flex; align-items:center; gap:8px; font-size:12.5px; font-weight:700; color:var(--el-text-color-secondary);">
          <el-icon><Grid /></el-icon> Cấu trúc cây danh mục
        </div>
        <div>
          <span style="font-size:18px; font-weight:800;">{{ treeData.length }}</span>
          <span style="font-size:12px; color:var(--el-text-color-placeholder); font-weight:600; margin-left:4px;">danh mục gốc</span>
        </div>
      </div>

      <!-- Tree table -->
      <el-table
        v-loading="loading"
        :data="treeData"
        row-key="id"
        :tree-props="{ children: 'children' }"
        stripe
        size="small"
      >
        <el-table-column label="ID" width="80" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.parentId ? 'info' : 'primary'"
              effect="plain"
              size="small"
              style="font-family:monospace;"
            >#{{ row.id }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Tên danh mục" min-width="220">
          <template #default="{ row }">
            <div style="display:flex; align-items:center; gap:8px;">
              <span :style="`font-weight:${!row.parentId ? 800 : row.children?.length ? 700 : 500}; font-size:13px;`">{{ row.name }}</span>
              <el-tag v-if="row.children?.length" size="small" type="info" effect="plain">{{ row.children.length }}</el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Ảnh" width="100" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.imageUrl"
              :src="fixImageUrl(row.imageUrl)"
              :alt="row.name"
              style="width:64px; height:46px; border-radius:6px; object-fit:cover;"
              fit="cover"
            />
            <div v-else style="width:64px; height:46px; border-radius:6px; background:var(--el-fill-color-light); border:1px dashed var(--el-border-color); display:flex; align-items:center; justify-content:center; margin:0 auto;">
              <el-icon style="color:var(--el-text-color-placeholder);"><Picture /></el-icon>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Mô tả" min-width="200">
          <template #default="{ row }">
            <el-text type="info" size="small" style="display:block; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; max-width:240px;">
              {{ row.description || '—' }}
            </el-text>
          </template>
        </el-table-column>

        <el-table-column label="Danh mục cha" width="160" align="center">
          <template #default="{ row }">
            <el-tag v-if="!row.parentId" type="info" effect="plain" size="small" round>— Gốc —</el-tag>
            <el-tag v-else type="primary" effect="plain" size="small" round style="max-width:120px; overflow:hidden; text-overflow:ellipsis;">
              {{ parentOptions.find(p => p.id === row.parentId)?.name || '—' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Trạng thái" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'danger'" effect="plain" size="small" round>
              <span :style="`width:6px;height:6px;border-radius:50%;background:currentColor;display:inline-block;vertical-align:middle;margin-right:4px;`"></span>
              {{ row.isActive ? 'Hoạt động' : 'Ẩn' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Thao tác" width="80" align="center">
          <template #default="{ row }">
            <el-dropdown @command="cmd => handleCommand(cmd, row)">
              <el-button circle plain size="small">
                <el-icon><MoreFilled /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit">
                    <el-icon><Edit /></el-icon> Chỉnh sửa
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" style="color:var(--el-color-danger);">
                    <el-icon><Delete /></el-icon> Xóa
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="Chưa có danh mục nào" :image-size="80">
            <el-button type="primary" @click="openCreate">Thêm danh mục đầu tiên</el-button>
          </el-empty>
        </template>
      </el-table>

      <!-- Footer -->
      <div v-if="!loading && treeData.length > 0" style="padding:11px 20px; border-top:1px solid var(--el-border-color-lighter); background:var(--el-fill-color-light); font-size:12.5px; color:var(--el-text-color-secondary);">
        Tổng cộng <strong>{{ treeData.length }}</strong> danh mục gốc
      </div>
    </el-card>

    <!-- Create / Edit Dialog -->
    <el-dialog
      v-model="dlg.open"
      :title="dlg.mode === 'create' ? 'Tạo danh mục mới' : 'Cập nhật danh mục'"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-alert v-if="dlg.alert" type="error" :title="dlg.alert" show-icon :closable="false" style="margin-bottom:16px;" />

      <el-form label-position="top" style="display:flex; flex-direction:column; gap:16px;">
        <el-form-item label="Tên danh mục *">
          <el-input v-model="dlg.form.name" placeholder="Nhập tên danh mục" :class="{'is-error': dlg.alert && !dlg.form.name}" />
        </el-form-item>

        <el-form-item label="Danh mục cha">
          <el-select v-model="dlg.form.parentId" placeholder="— Không có (danh mục gốc) —" clearable style="width:100%;" filterable>
            <el-option :value="null" label="— Không có (danh mục gốc) —" />
            <el-option
              v-for="c in parentOptions"
              :key="c.id"
              :value="c.id"
              :label="c.name"
              :disabled="c.id === dlg.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="Mô tả">
          <el-input
            v-model="dlg.form.description"
            type="textarea"
            :rows="3"
            placeholder="Nhập mô tả danh mục (tùy chọn)"
          />
        </el-form-item>

        <el-form-item label="Ảnh danh mục">
          <el-upload
            action="#"
            :auto-upload="false"
            :show-file-list="true"
            accept="image/*"
            :limit="1"
            @change="onPickFile"
          >
            <el-button plain>
              <el-icon><Upload /></el-icon> Chọn ảnh…
            </el-button>
          </el-upload>
          <div v-if="dlg.mode === 'edit' && !dlg.form.imageFile" style="font-size:12px; color:var(--el-text-color-secondary); margin-top:6px;">
            Để trống để giữ ảnh hiện tại.
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dlg.open = false">Hủy bỏ</el-button>
        <el-button type="primary" :loading="dlg.loading" @click="save">
          {{ dlg.mode === 'create' ? 'Tạo danh mục' : 'Lưu thay đổi' }}
        </el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { Delete, Edit, Grid, MoreFilled, Picture, Plus, Refresh, Upload } from "@element-plus/icons-vue";
import { onMounted, reactive, ref } from "vue";
import axios from "axios";
import { toast } from "../../ui/toast";
import { confirmModal } from "../../ui/confirm";

const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

const loading = ref(false);
const treeData = ref([]);
const parentOptions = ref([]);

// ── Command handler for el-dropdown ────────────────────────────────
async function handleCommand(cmd, row) {
  if (cmd === 'edit') {
    openEdit(row);
  } else if (cmd === 'delete') {
    const ok = await confirmModal(
      `Danh mục "${row.name}" sẽ bị ẩn khỏi hệ thống. Danh mục con (nếu có) vẫn được giữ nguyên.`,
      "Xóa danh mục này?",
      "Xác nhận xóa",
      true,
    );
    if (!ok) return;
    try {
      await axios.delete(`${BASE_URL}/api/categories/${row.id}`);
      toast("Danh mục đã được ẩn thành công.", "success");
      load();
    } catch {
      toast("Xóa thất bại.", "error");
    }
  }
}

// ── Helper ─────────────────────────────────────────────────────────
function fixImageUrl(url) {
  if (!url) return "https://via.placeholder.com/150?text=No+Image";
  if (url.startsWith("http")) return url;
  return `${BASE_URL}${url}`;
}

// ── Tree builder ───────────────────────────────────────────────────
function buildCategoryTree(flatList) {
  const map = {};
  const roots = [];
  flatList.forEach(item => { map[item.id] = { ...item, children: [] }; });
  flatList.forEach(item => {
    const node = map[item.id];
    if (item.parentId && map[item.parentId]) map[item.parentId].children.push(node);
    else roots.push(node);
  });
  Object.values(map).forEach(node => { if (node.children.length === 0) delete node.children; });
  return roots;
}

// ── API ────────────────────────────────────────────────────────────
async function load() {
  loading.value = true;
  try {
    const res = await axios.get(`${BASE_URL}/api/categories`, { params: { activeOnly: false } });
    const flatList = res.data?.data || [];
    parentOptions.value = flatList;
    treeData.value = buildCategoryTree(flatList);
  } catch {
    toast("Failed to load categories.", "error");
  } finally {
    loading.value = false;
  }
}

// ── Create / Edit dialog ───────────────────────────────────────────
const dlg = reactive({
  open: false, mode: "create", loading: false, alert: "", id: null,
  form: { name: "", description: "", parentId: null, imageFile: null },
});

function openCreate() {
  dlg.mode = "create"; dlg.id = null;
  dlg.form = { name: "", description: "", parentId: null, imageFile: null };
  dlg.alert = ""; dlg.open = true;
}

function openEdit(row) {
  dlg.mode = "edit"; dlg.id = row.id;
  dlg.form = { name: row.name, description: row.description, parentId: row.parentId, imageFile: null };
  dlg.alert = ""; dlg.open = true;
}

function onPickFile({ raw }) {
  if (raw) dlg.form.imageFile = raw;
}

async function save() {
  dlg.alert = "";
  if (!dlg.form.name) { dlg.alert = "Tên danh mục là bắt buộc."; return; }
  dlg.loading = true;
  try {
    const formData = new FormData();
    formData.append("name", dlg.form.name);
    formData.append("description", dlg.form.description || "");
    if (dlg.form.parentId) formData.append("parentId", dlg.form.parentId);
    if (dlg.form.imageFile) formData.append("imageFile", dlg.form.imageFile);
    if (dlg.mode === "create") {
      await axios.post(`${BASE_URL}/api/categories`, formData);
      toast("Category created successfully.", "success");
    } else {
      await axios.put(`${BASE_URL}/api/categories/${dlg.id}`, formData);
      toast("Category updated successfully.", "success");
    }
    dlg.open = false; load();
  } catch (e) {
    const msg = e?.response?.data?.message || e?.message || "Save failed";
    dlg.alert = typeof msg === "string" ? msg : JSON.stringify(msg);
  } finally {
    dlg.loading = false;
  }
}

onMounted(load);
</script>

<style></style>