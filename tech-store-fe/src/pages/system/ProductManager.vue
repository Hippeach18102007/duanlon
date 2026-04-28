<template>
  <div style="background:#f6f7f9; min-height:100vh; padding:32px 40px 60px;">

    <!-- Header -->
    <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:20px; margin-bottom:22px; flex-wrap:wrap;">
      <div>
        <div style="font-size:11px; font-weight:700; letter-spacing:0.08em; text-transform:uppercase; color:var(--el-text-color-secondary); margin-bottom:6px; display:flex; align-items:center; gap:5px;">
            <el-icon><Goods /></el-icon> Kho & Sản phẩm
          </div>
        <div style="font-size:28px; font-weight:800; letter-spacing:-0.03em; margin-bottom:4px;">Quản lý sản phẩm</div>
        <div style="font-size:13px; color:var(--el-text-color-secondary);">Hỗ trợ: Biến thể · Tồn kho · Đa danh mục · Sắp xếp · Thùng rác · Thẻ</div>
      </div>
      <!-- End Header -->
      <div style="display:flex; align-items:center; gap:8px; flex-shrink:0; padding-top:4px; flex-wrap:wrap;">
        <el-radio-group v-model="viewMode" size="small" @change="load">
          <el-radio-button value="active"><el-icon><Check /></el-icon> Hoạt động</el-radio-button>
          <el-radio-button value="trash"><el-icon><Delete /></el-icon> Thùng rác</el-radio-button>
        </el-radio-group>
        <el-button plain :loading="loading" @click="load"><el-icon><Refresh /></el-icon> Tải lại</el-button>
        <el-button v-if="viewMode==='active'" plain @click="showImport = true"><el-icon><Upload /></el-icon> Import Excel</el-button>
        <el-button v-if="viewMode==='active'" type="primary" @click="openCreateDialog"><el-icon><Plus /></el-icon> Thêm sản phẩm</el-button>
      </div>
    </div>

    <el-card v-if="viewMode==='active'" shadow="never" style="margin-bottom:16px;" :body-style="{ padding:0 }">
      <div style="display:flex; align-items:center; justify-content:space-between; padding:10px 16px; border-bottom:1px solid var(--el-border-color-lighter);">
        <el-space :size="8">
          <el-icon><Filter /></el-icon>
          <el-text style="font-size:13px; font-weight:500;">Bộ lọc</el-text>
          <el-tag v-if="activeFilterCount>0" type="primary" size="small" effect="plain">{{ activeFilterCount }} đang áp dụng</el-tag>
        </el-space>
        <el-button v-if="activeFilterCount>0" link type="danger" @click="clearFilters">
          <el-icon><Close /></el-icon> Xoá tất cả
        </el-button>
      </div>

      <div style="display:grid; grid-template-columns:2fr 1fr 1fr 1fr; border-bottom:1px solid var(--el-border-color-lighter);">
        <div style="padding:12px 16px; border-right:1px solid var(--el-border-color-lighter);">
          <div style="font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:0.05em; color:var(--el-text-color-secondary); margin-bottom:6px;">Tìm kiếm</div>
          <el-input v-model="keyword" placeholder="Tên sản phẩm / SKU / Thương hiệu…" clearable size="small" @keyup.enter="onFilter" @clear="onFilter">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>
        <div style="padding:12px 16px; border-right:1px solid var(--el-border-color-lighter);">
          <div style="font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:0.05em; color:var(--el-text-color-secondary); margin-bottom:6px;">Danh mục</div>
          <el-select v-model="categoryIds" multiple collapse-tags collapse-tags-tooltip placeholder="Tất cả danh mục" clearable size="small" style="width:100%;" @change="onFilter">
            <el-option v-for="c in categories.filter(c => c.id !== 6 && c.name !== 'Chuột' && c.name !== 'Bàn phím')" :key="c.id" :value="c.id" :label="c.name" />
          </el-select>
        </div>
        <div style="padding:12px 16px; border-right:1px solid var(--el-border-color-lighter);">
          <div style="font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:0.05em; color:var(--el-text-color-secondary); margin-bottom:6px;">Thẻ chiến dịch</div>
          <el-select v-model="filterTagId" clearable placeholder="Tất cả thẻ" size="small" style="width:100%;" @change="onFilter">
            <el-option :value="null" label="Tất cả thẻ" />
            <el-option v-for="t in tags" :key="t.id" :value="t.id" :label="t.name" />
          </el-select>
        </div>
        <div style="padding:12px 16px;">
          <div style="font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:0.05em; color:var(--el-text-color-secondary); margin-bottom:6px;">Sắp xếp</div>
          <el-select v-model="sortBy" size="small" style="width:100%;" @change="onFilter">
            <el-option value="recently_updated" label="Mới cập nhật" />
            <el-option value="newest_arrival" label="Ngày nhập mới nhất" />
            <el-option value="newest" label="Mới nhất" />
            <el-option value="oldest" label="Cũ nhất" />
            <el-option value="best_selling" label="Bán chạy nhất" />
            <el-option value="price_asc" label="Giá: Thấp → Cao" />
            <el-option value="price_desc" label="Giá: Cao → Thấp" />
            <el-option value="name_asc" label="Tên: A → Z" />
            <el-option value="name_desc" label="Tên: Z → A" />
          </el-select>
        </div>
      </div>

      <div style="display:grid; grid-template-columns:2fr 1fr 1fr 1fr;">
        <div style="padding:12px 16px; border-right:1px solid var(--el-border-color-lighter);">
          <div style="font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:0.05em; color:var(--el-text-color-secondary); margin-bottom:6px;">Ngày nhập kho</div>
          <el-date-picker v-model="dateRange" type="daterange" range-separator="→" start-placeholder="Từ ngày" end-placeholder="Đến ngày" size="small" style="width:100%;" @change="onFilter" />
        </div>
        <div style="padding:12px 16px; border-right:1px solid var(--el-border-color-lighter);">
          <div style="font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:0.05em; color:var(--el-text-color-secondary); margin-bottom:6px;">Sản phẩm mới</div>
          <el-select v-model="filterIsNew" clearable placeholder="Tất cả" size="small" style="width:100%;" @change="onFilter">
            <el-option :value="null" label="Tất cả" />
            <el-option :value="true" label="Chỉ hàng mới" />
            <el-option :value="false" label="Hàng thường" />
          </el-select>
        </div>
        <div style="padding:12px 16px; border-right:1px solid var(--el-border-color-lighter);">
          <div style="font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:0.05em; color:var(--el-text-color-secondary); margin-bottom:6px;">Chất lượng</div>
          <el-select v-model="filterIsFaulty" clearable placeholder="Tất cả" size="small" style="width:100%;" @change="onFilter">
            <el-option :value="null" label="Tất cả" />
            <el-option :value="true" label="Bị lỗi (ẩn)" />
            <el-option :value="false" label="Chất lượng tốt" />
          </el-select>
        </div>
        <div style="padding:12px 16px; display:flex; align-items:flex-end; gap:12px;">
          <el-checkbox v-model="inStockOnly" @change="onFilter">Chỉ còn hàng</el-checkbox>
          <el-button type="primary" plain size="small" @click="onFilter">
            <el-icon><Search /></el-icon> Lọc
          </el-button>
        </div>
      </div>
    </el-card>

    <transition name="el-fade-in">
      <el-card v-if="selectedIds.length>0 && viewMode==='active'" shadow="never" style="margin-bottom:16px; border-color:var(--el-color-primary-light-5); background:var(--el-color-primary-light-9);" :body-style="{ padding:'10px 16px' }">
        <div style="display:flex; align-items:center; justify-content:space-between;">
          <el-space :size="8">
            <span style="width:8px; height:8px; border-radius:50%; background:var(--el-color-primary); display:inline-block;"></span>
            <el-text>Đã chọn <strong>{{ selectedIds.length }}</strong> sản phẩm</el-text>
          </el-space>
          <el-space :size="8">
            <el-button plain size="small" @click="openBatchUpdateDialog">
              <el-icon><Edit /></el-icon> Sửa hàng loạt
            </el-button>
            <el-button type="danger" plain size="small" :loading="isBatchDeleting" @click="confirmBatchDelete">
              <el-icon><Delete /></el-icon> Xóa hàng loạt
            </el-button>
          </el-space>
        </div>
      </el-card>
    </transition>

    <el-card shadow="never" :body-style="{ padding:0 }">
      <div style="display:flex; align-items:center; justify-content:space-between; padding:12px 20px; border-bottom:1px solid var(--el-border-color-lighter); background:var(--el-fill-color-light);">
        <el-space :size="8">
          <el-icon><Grid /></el-icon>
          <el-text style="font-size:12.5px; font-weight:700; color:var(--el-text-color-secondary);">{{ viewMode==='active' ? 'Sản phẩm hoạt động' : 'Thùng rác' }}</el-text>
        </el-space>
        <div>
          <span style="font-size:18px; font-weight:800;">{{ totalElements }}</span>
          <span style="font-size:12px; color:var(--el-text-color-placeholder); font-weight:600; margin-left:4px;">sản phẩm</span>
        </div>
      </div>

      <el-table :data="rows" v-loading="loading" stripe size="small" @selection-change="(val) => selectedIds = val.map(r => r.id)">
        <el-table-column v-if="viewMode==='active'" type="selection" width="44" />
        <el-table-column label="ID" width="70" align="center">
          <template #default="{ row }">
            <el-tag type="primary" effect="plain" size="small" style="font-family:monospace;">#{{ row.id }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Ảnh" width="88" align="center">
          <template #default="{ row }">
            <el-image v-if="row.imageUrl" :src="row.imageUrl" :alt="row.name" style="width:64px; height:46px; border-radius:6px; object-fit:cover;" fit="cover" />
            <div v-else style="width:64px; height:46px; border-radius:6px; background:var(--el-fill-color-light); border:1px dashed var(--el-border-color); display:flex; align-items:center; justify-content:center; margin:0 auto;">
              <el-icon style="color:var(--el-text-color-placeholder);"><Picture /></el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Thông tin sản phẩm" min-width="260">
          <template #default="{ row }">
            <div style="display:flex; align-items:center; gap:6px; margin-bottom:4px; flex-wrap:wrap;">
              <span style="font-weight:700; font-size:13px;">{{ row.name }}</span>
              <el-tag v-if="row.isNew" type="success" size="small" effect="plain">MỚI</el-tag>
              <el-tag v-if="row.isFaulty" type="danger" size="small" effect="plain">LỖI</el-tag>
            </div>
            <div style="display:flex; align-items:center; gap:6px; margin-bottom:4px;">
              <el-text size="small" type="info">SKU:</el-text>
              <span style="font-family:monospace; font-size:11px; color:var(--el-text-color-secondary);">{{ row.sku }}</span>
              <el-text v-if="row.brand" size="small" type="info" style="margin-left:8px;">Hãng:</el-text>
              <span v-if="row.brand" style="font-size:11px; font-weight:600; color:var(--el-color-primary);">{{ row.brand }}</span>
            </div>
            <el-space v-if="row.tags?.length" wrap :size="4" style="margin-bottom:4px;">
              <el-tag v-for="(tagName, idx) in row.tags" :key="idx" type="warning" size="small" effect="plain">{{ tagName }}</el-tag>
            </el-space>
            <div style="display:flex; align-items:center; gap:8px;">
              <span v-if="row.minPrice" style="font-family:monospace; font-size:12px; font-weight:700; color:var(--el-color-primary);">{{ formatCurrency(row.minPrice) }}</span>
              <el-tag v-if="viewMode==='active'" :type="row.inStock?'success':'danger'" size="small" effect="plain" round>
                <span :style="`width:5px;height:5px;border-radius:50%;background:currentColor;display:inline-block;vertical-align:middle;margin-right:3px;`"></span>
                {{ row.inStock ? `Còn hàng (${row.totalStock||0})` : 'Hết hàng' }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Mô tả" min-width="200">
          <template #default="{ row }">
            <el-text size="small" type="info" style="display:block; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; max-width:240px;">{{ row.description || '—' }}</el-text>
          </template>
        </el-table-column>
        <el-table-column label="Ngày nhập" width="130" align="center">
          <template #default="{ row }">
            <el-text size="small" type="info" style="font-family:monospace;">{{ formatDate(row.createdAt) }}</el-text>
          </template>
        </el-table-column>
        <el-table-column label="Trạng thái" width="140" align="center">
          <template #default="{ row }">
            <el-switch v-if="viewMode==='active'" v-model="row.isVisible" @change="toggleProductStatus(row)" active-text="ON" inactive-text="OFF" size="small" />
            <el-tag v-else type="info" size="small" effect="plain" round>
              <span style="width:5px;height:5px;border-radius:50%;background:currentColor;display:inline-block;vertical-align:middle;margin-right:3px;"></span>
              Thùng rác
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Hành động" width="200" align="center">
          <template #default="{ row }">
            <el-space v-if="viewMode==='active'" :size="4">
              <el-button size="small" plain type="primary" @click="openVariantDrawer(row)">
                <el-icon><Guide /></el-icon> Biến thể
              </el-button>
              <el-button size="small" plain @click="onEdit(row)"><el-icon><Edit /></el-icon> Sửa</el-button>
              <el-button size="small" plain type="danger" @click="openDeleteConfirm(row)">
                <el-icon><Delete /></el-icon> {{ row.isFaulty ? 'Xóa hẳn' : 'Xóa' }}
              </el-button>
            </el-space>
            <el-button v-else plain size="small" @click="onRestore(row.id)">
              <el-icon><RefreshLeft /></el-icon> Khôi phục
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="viewMode==='trash' ? 'Thùng rác trống' : 'Chưa có sản phẩm nào'" :image-size="80" />
        </template>
      </el-table>

      <div v-if="totalElements>0" style="display:flex; align-items:center; justify-content:space-between; padding:12px 20px; border-top:1px solid var(--el-border-color-lighter); background:var(--el-fill-color-light); flex-wrap:wrap; gap:10px;">
        <el-text size="small" type="info">{{ page*20+1 }}–{{ Math.min((page+1)*20, totalElements) }} / {{ totalElements }}</el-text>
        <el-pagination :current-page="page+1" :page-size="20" :total="totalElements" layout="prev, pager, next" background small @current-change="onPageChange" />
      </div>
    </el-card>

    <el-dialog v-model="deleteDlg.open" :title="deleteDlg.isFaulty ? 'Xóa vĩnh viễn?' : 'Xóa sản phẩm?'" width="420px" :close-on-click-modal="false">
      <el-alert :type="deleteDlg.isFaulty?'error':'warning'" :closable="false" style="margin-bottom:12px;">
        {{ deleteDlg.isFaulty ? 'Không thể hoàn tác sau khi xác nhận.' : 'Có thể khôi phục từ thùng rác.' }}
      </el-alert>
      <el-text>Sản phẩm <strong>{{ deleteDlg.name }}</strong> {{ deleteDlg.isFaulty ? 'sẽ bị xóa vĩnh viễn khỏi hệ thống.' : 'sẽ được chuyển vào thùng rác.' }}</el-text>
      <template #footer>
        <el-button @click="deleteDlg.open=false">Hủy bỏ</el-button>
        <el-button type="danger" @click="confirmDelete"><el-icon><Delete /></el-icon> Xác nhận</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchDeleteDlg" title="Xóa hàng loạt" width="420px" :close-on-click-modal="false">
      <el-alert type="warning" title="Có thể khôi phục từng sản phẩm từ thùng rác." :closable="false" style="margin-bottom:12px;" />
      <el-text>Tất cả <strong>{{ selectedIds.length }}</strong> sản phẩm đã chọn sẽ được chuyển vào thùng rác.</el-text>
      <template #footer>
        <el-button @click="batchDeleteDlg=false">Hủy bỏ</el-button>
        <el-button type="danger" :loading="isBatchDeleting" @click="handleBatchDelete"><el-icon><Delete /></el-icon> Xác nhận</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchDlg.open" title="Cập nhật hàng loạt" width="480px" :close-on-click-modal="false">
      <el-alert type="info" title="Chỉ những trường có giá trị mới được áp dụng." :closable="false" style="margin-bottom:16px;" />
      <el-form label-position="top" style="display:flex; flex-direction:column; gap:0;">
        <el-form-item label="Trạng thái hiển thị">
          <el-select v-model="batchDlg.form.isVisible" style="width:100%;" clearable placeholder="Không thay đổi">
            <el-option :value="null" label="Không thay đổi" /><el-option :value="true" label="Bật hiển thị (ON)" /><el-option :value="false" label="Tắt hiển thị (OFF)" />
          </el-select>
        </el-form-item>
        <el-form-item label="Nhãn hàng mới">
          <el-select v-model="batchDlg.form.isNew" style="width:100%;" clearable placeholder="Không thay đổi">
            <el-option :value="null" label="Không thay đổi" /><el-option :value="true" label="Đánh dấu Hàng Mới" /><el-option :value="false" label="Gỡ nhãn Mới" />
          </el-select>
        </el-form-item>
        <el-form-item label="Trạng thái lỗi">
          <el-select v-model="batchDlg.form.isFaulty" style="width:100%;" clearable placeholder="Không thay đổi">
            <el-option :value="null" label="Không thay đổi" /><el-option :value="true" label="Đánh dấu Lỗi" /><el-option :value="false" label="Hàng Tốt" />
          </el-select>
        </el-form-item>
        <el-form-item label="Gắn Tags">
          <el-select v-model="batchDlg.form.tagIds" multiple collapse-tags style="width:100%;" placeholder="Chọn thẻ" clearable>
            <el-option v-for="t in tags" :key="t.id" :value="t.id" :label="t.name" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDlg.open=false">Hủy bỏ</el-button>
        <el-button type="primary" :loading="batchDlg.loading" @click="submitBatchUpdate"><el-icon><Check /></el-icon> Áp dụng thay đổi</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="vr.open" :title="vr.productName" size="560px" direction="rtl">
  <template #header>
    <div>
      <div style="font-size:11px; color:var(--el-text-color-secondary); margin-bottom:3px;">Quản lý biến thể</div>
      <div style="font-size:16px; font-weight:800;">{{ vr.productName }}</div>
    </div>
  </template>

  <el-tabs v-model="vr.activeTab" type="border-card" style="height: 100%;">
    
    <!-- Tab 1: Danh sách biến thể -->
    <el-tab-pane name="list">
      <template #label>
        <el-space :size="5"><el-icon><Grid /></el-icon> Danh sách biến thể</el-space>
      </template>

      <el-table :data="vr.variants" v-loading="vr.loading" size="small" stripe style="margin-bottom:20px;">
        <el-table-column label="Tên biến thể" min-width="120">
          <template #default="{ row }"><span style="font-weight:600; font-size:13px;">{{ row.variantName }}</span></template>
        </el-table-column>
        <el-table-column label="SKU" width="110">
          <template #default="{ row }"><span style="font-family:monospace; font-size:12px;">{{ row.sku }}</span></template>
        </el-table-column>
        <el-table-column label="Giá / Tồn" width="130" align="right">
          <template #default="{ row }">
            <div style="font-size:13px; font-weight:700; font-family:monospace; color:var(--el-color-primary);">{{ formatCurrency(row.price) }}</div>
            <div style="font-size:11px; margin-top:2px;">Tồn: <span :style="`font-weight:700; color:${row.stockQuantity>0?'var(--el-color-success)':'var(--el-color-danger)'}`">{{ row.stockQuantity }}</span></div>
          </template>
        </el-table-column>
        <el-table-column label="Hành động" width="160" align="center">
          <template #default="{ row }">
            <el-space :size="4">
              <el-button size="small" plain type="primary" @click="openSerialDialog(row)" style="font-size:11px; padding:5px 9px;">Số Seri</el-button>
              <el-button size="small" plain @click="editVariant(row); vr.activeTab = 'form'"><el-icon><Edit /></el-icon></el-button>
              <el-button size="small" plain type="danger" @click="deleteVariant(row.id)"><el-icon><Delete /></el-icon></el-button>
            </el-space>
          </template>
        </el-table-column>
        <template #empty><el-empty description="Chưa có biến thể" :image-size="48" /></template>
      </el-table>

      <div style="text-align:right;">
        <el-button type="primary" size="small" @click="resetVariantForm(); vr.activeTab = 'form'">
          <el-icon><Plus /></el-icon> Thêm biến thể mới
        </el-button>
      </div>
    </el-tab-pane>

    <!-- Tab 2: Form thêm / sửa biến thể -->
    <el-tab-pane name="form">
      <template #label>
        <el-space :size="5">
          <el-icon><Edit /></el-icon>
          {{ vr.isEdit ? 'Cập nhật biến thể' : 'Thêm biến thể mới' }}
        </el-space>
      </template>

      <el-form label-position="top">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="Tên biến thể *">
              <el-input v-model="vr.form.variantName" placeholder="vd: Đỏ, 16GB…" size="small" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="SKU *">
              <el-input v-model="vr.form.sku" size="small" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Giá (VND) *">
              <el-input-number v-model="vr.form.price" :min="0" size="small" style="width:100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Tồn kho">
              <el-input-number v-model="vr.form.stockQuantity" :disabled="true" size="small" style="width:100%;" />
            </el-form-item>
          </el-col>
        </el-row>

        <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:8px;">
          <el-text size="small" style="font-weight:700; text-transform:uppercase; letter-spacing:0.05em; color:var(--el-text-color-secondary);">Dynamic Attributes</el-text>
          <el-button link type="primary" size="small" @click="addVariantAttr"><el-icon><Plus /></el-icon> Thêm</el-button>
        </div>
        <div v-for="(attr, i) in vr.attrsList" :key="i" style="display:flex; gap:8px; margin-bottom:8px;">
          <el-input v-model="attr.key" placeholder="Key (e.g. Color)" size="small" style="flex:1;" />
          <el-input v-model="attr.value" placeholder="Value" size="small" style="flex:1;" />
          <el-button circle plain size="small" type="danger" @click="removeVariantAttr(i)"><el-icon><Close /></el-icon></el-button>
        </div>

        <div style="display:flex; gap:8px; justify-content:flex-end; margin-top:16px;">
          <el-button plain size="small" @click="resetVariantForm(); vr.activeTab = 'list'">
            <el-icon><Close /></el-icon> Hủy
          </el-button>
          <el-button type="primary" plain size="small" :loading="vr.saving" @click="saveVariant">
            <el-icon><Check /></el-icon> {{ vr.isEdit ? 'Lưu thay đổi' : 'Thêm biến thể' }}
          </el-button>
        </div>
      </el-form>
    </el-tab-pane>

  </el-tabs>
</el-drawer>

    <el-dialog v-model="serialDlg.open" :title="`Quản lý Seri — ${serialDlg.variantName}`" width="620px" :close-on-click-modal="false">
      <div style="display:flex; align-items:center; gap:10px; margin-bottom:16px; flex-wrap: wrap;">
        <el-upload action="#" :show-file-list="false" accept=".xlsx, .xls" :auto-upload="false" @change="importSerialsExcel">
          <el-button type="primary" plain :loading="serialDlg.adding">
            <el-icon><Upload /></el-icon> Nhập Excel
          </el-button>
        </el-upload>
        <el-divider direction="vertical" />
        <el-input 
          v-model="serialDlg.inputText" 
          placeholder="Nhập Seri laptop (VD: 5CG123...)" 
          style="width: 200px;" 
          @input="formatSerialInput"
          @keyup.enter="addManualSerial" 
          clearable 
        />
        <el-button type="success" :loading="serialDlg.addingManual" @click="addManualSerial">Thêm</el-button>
      </div>
      <el-text tag="div" style="font-size:12px; font-weight:700; text-transform:uppercase; letter-spacing:0.05em; color:var(--el-text-color-secondary); margin-bottom:8px;">Danh sách máy trong kho</el-text>
      <el-table :data="serialDlg.list" v-loading="serialDlg.loading" :max-height="320" stripe size="small">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column label="Số Seri / IMEI">
          <template #default="{ row }"><span style="font-family:monospace; font-weight:600;">{{ row.serialNumber }}</span></template>
        </el-table-column>
        <el-table-column label="Trạng thái" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status==='IN_STOCK'?'success':row.status==='SOLD'?'info':'danger'" size="small" effect="plain" round>
              <span :style="`width:5px;height:5px;border-radius:50%;background:currentColor;display:inline-block;vertical-align:middle;margin-right:3px;`"></span>
              {{ row.status==='IN_STOCK'?'Trong kho':row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Thao tác" width="80" align="center">
          <template #default="{ row }">
            <el-button size="small" plain type="danger" :disabled="row.status!=='IN_STOCK'" @click="deleteSerial(row.id)">Xóa</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="Chưa có seri nào" :image-size="48" /></template>
      </el-table>
      <template #footer>
        <el-button @click="serialDlg.open=false">Đóng</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dlg.open" :title="dlg.isEdit ? 'Thông tin sản phẩm' : 'Thêm sản phẩm mới'" width="680px" :close-on-click-modal="false">
      <el-alert v-if="dlg.alert" type="error" :title="dlg.alert" show-icon :closable="false" style="margin-bottom:12px;" />

      <el-tabs v-model="dlg.activeTab">
        <el-tab-pane label="Thông tin cơ bản" name="info">
          <template #label><el-space :size="5"><el-icon><InfoFilled /></el-icon>Thông tin cơ bản</el-space></template>
        </el-tab-pane>
        <el-tab-pane v-if="dlg.isEdit" label="Lịch sử thay đổi" name="history">
          <template #label><el-space :size="5"><el-icon><Clock /></el-icon>Lịch sử thay đổi</el-space></template>
        </el-tab-pane>
      </el-tabs>

      <div v-if="dlg.activeTab==='info'" style="padding-top:8px;">
        <el-form label-position="top">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="Tên sản phẩm *"><el-input v-model="dlg.form.name" placeholder="Nhập tên sản phẩm" /></el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="SKU chính *"><el-input v-model="dlg.form.sku" /></el-form-item>
            </el-col>
            
            <el-col :span="12">
              <el-form-item label="Thương hiệu">
                <el-select v-model="dlg.form.brand" placeholder="Chọn hoặc nhập mới (Apple, Asus...)" filterable allow-create default-first-option clearable style="width:100%;">
                  <el-option v-for="brand in commonBrands" :key="brand" :value="brand" :label="brand" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="Danh mục">
                <el-select v-model="dlg.form.categoryIds" multiple collapse-tags style="width:100%;" placeholder="Chọn danh mục" clearable>
                  <el-option v-for="c in categories" :key="c.id" :value="c.id" :label="c.name" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="Thẻ chiến dịch">
                <el-select v-model="dlg.form.tagIds" multiple collapse-tags style="width:100%;" placeholder="Chọn thẻ" clearable>
                  <el-option v-for="t in tags" :key="t.id" :value="t.id" :label="t.name" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24" style="margin-bottom:16px;">
            <el-col :span="12">
              <div style="display:flex; align-items:center; gap:12px;">
                <el-text size="small">Đánh dấu Mới</el-text>
                <el-switch v-model="dlg.form.isNew" active-text="Mới" inactive-text="Thường" />
              </div>
            </el-col>
            
            <el-col :span="12" v-if="dlg.isEdit">
              <div style="display:flex; align-items:center; gap:12px;">
                <el-text size="small">Đánh dấu Lỗi (Ẩn)</el-text>
                <el-switch v-model="dlg.form.isFaulty" active-text="Lỗi" inactive-text="Tốt" />
              </div>
            </el-col>
            </el-row>

          <el-form-item label="Mô tả">
            <el-input v-model="dlg.form.description" type="textarea" :rows="3" />
          </el-form-item>

          <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:8px;">
            <el-text size="small" style="font-weight:700; text-transform:uppercase; letter-spacing:0.05em; color:var(--el-text-color-secondary);">Thông số kỹ thuật</el-text>
            <el-button link type="primary" size="small" @click="addAttribute"><el-icon><Plus /></el-icon> Thêm</el-button>
          </div>
          <div v-for="(attr, i) in dlg.attributesList" :key="i" style="display:flex; gap:8px; margin-bottom:8px;">
            <el-input v-model="attr.name" placeholder="Label (e.g. RAM)" size="small" style="flex:1;" />
            <el-input v-model="attr.value" placeholder="Value" size="small" style="flex:1;" />
            <el-button circle plain size="small" type="danger" @click="removeAttribute(i)"><el-icon><Close /></el-icon></el-button>
          </div>

          <div v-if="dlg.isEdit && dlg.existingImages.length>0" style="margin-top:14px;">
            <el-text size="small" tag="div" style="font-weight:700; margin-bottom:10px;">Thư viện ảnh</el-text>
            <div style="display:flex; gap:10px; flex-wrap:wrap;">
              <div v-for="img in dlg.existingImages" :key="img.id" style="position:relative; width:80px; height:60px;">
                <el-image :src="fixImageUrl(img.url)" style="width:100%; height:100%; border-radius:6px; object-fit:cover;" :class="img.isPrimary ? 'is-primary' : ''" />
                <div v-if="img.isPrimary" style="position:absolute; top:2px; left:2px; background:var(--el-color-primary); color:#fff; font-size:9px; font-weight:700; padding:1px 5px; border-radius:3px;">MAIN</div>
                <div style="position:absolute; top:2px; right:2px; display:flex; gap:2px;">
                  <el-tooltip v-if="!img.isPrimary" content="Đặt ảnh chính">
                    <el-button circle size="small" type="warning" @click="setPrimaryImage(img.id)" style="width:20px; height:20px; padding:0;"><el-icon :size="10"><Star /></el-icon></el-button>
                  </el-tooltip>
                  <el-button circle size="small" type="danger" @click="markImageForDelete(img.id)" style="width:20px; height:20px; padding:0;"><el-icon :size="10"><Close /></el-icon></el-button>
                </div>
              </div>
            </div>
          </div>

          <el-form-item label="Upload ảnh mới" style="margin-top:14px;">
            <el-upload action="#" :auto-upload="false" :show-file-list="true" accept="image/*" :multiple="true" :limit="10" @change="(file, files) => dlg.form.galleryImages = files.map(f => f.raw)">
              <el-button plain><el-icon><Upload /></el-icon> {{ dlg.form.galleryImages?.length ? `${dlg.form.galleryImages.length} file đã chọn` : 'Chọn ảnh (nhiều file)…' }}</el-button>
            </el-upload>
          </el-form-item>
        </el-form>
      </div>

      <div v-else-if="dlg.activeTab==='history'" style="padding-top:8px;">
        <el-skeleton v-if="dlg.historyLoading" :rows="4" animated />
        <el-empty v-else-if="!dlg.history.length" description="Chưa có bản ghi thay đổi nào" :image-size="60" />
        <el-timeline v-else>
          <el-timeline-item v-for="(log, i) in dlg.history" :key="i"
            :type="log.severity==='HIGH'||log.severity==='CRITICAL'?'danger':log.severity==='MEDIUM'?'warning':'primary'"
            :timestamp="formatDate(log.createdAt)" placement="top">
            <el-card shadow="never" :body-style="{ padding:'10px 14px' }">
              <div style="display:flex; align-items:center; gap:8px; margin-bottom:4px;">
                <el-avatar :size="22" style="font-size:10px; font-weight:700; background:var(--el-fill-color);">{{ (log.username||'S').charAt(0).toUpperCase() }}</el-avatar>
                <el-text style="font-weight:600; font-size:12px;">{{ log.username||'System' }}</el-text>
                <el-tag :type="getLogType(log.severity)" size="small" effect="plain">{{ log.actionType||'UPDATE' }}</el-tag>
              </div>
              <el-text size="small" type="info" style="display:block;">{{ log.description }}</el-text>
              <el-text size="small" type="info" style="font-size:10px; margin-top:3px; display:flex; align-items:center; gap:4px;">
                <el-icon :size="10"><Monitor /></el-icon> IP: {{ log.ipAddress||'N/A' }}
              </el-text>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </div>

      <template #footer>
        <el-button @click="dlg.open=false">Hủy bỏ</el-button>
        <el-button v-if="dlg.activeTab==='info'" type="primary" :loading="dlg.loading" @click="submitForm">
          <el-icon><Check /></el-icon> {{ dlg.isEdit ? 'Cập nhật sản phẩm' : 'Tạo sản phẩm' }}
        </el-button>
      </template>
    </el-dialog>

    <ProductImportDialog :open="showImport" @close="showImport=false" @imported="load()" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from "vue";
import { Check, Close, Clock, Delete, Edit, Filter, Grid, Guide, InfoFilled, Monitor, Picture, Plus, Refresh, RefreshLeft, Search, Star, Upload, VideoPlay } from "@element-plus/icons-vue";
import { categoriesApi } from "../../api/categories.api";
import { productsApi } from "../../api/products.api";
import { toast } from "../../ui/toast";
import axios from 'axios';
import http from "../../api/http";
import ProductImportDialog from '../../components/Productimportdialog.vue'
import { ElNotification } from "element-plus";
function formatSerialInput(val) {
  if (!val) return;
  serialDlg.inputText = val.toUpperCase().replace(/[^A-Z0-9-]/g, '');
}

const BASE_URL_API = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const commonBrands = ref(["Apple", "Samsung", "Asus", "Acer", "Dell", "HP", "Lenovo", "MSI", "Xiaomi", "Khác"]);
const loading = ref(false);
const categories = ref([]);
const rows = ref([]);
const tags = ref([]);
const filterTagId = ref(null);
const viewMode = ref('active');
const page = ref(0);
const totalElements = ref(0);
const keyword = ref("");
const categoryIds = ref([]);
const sortBy = ref("newest_arrival");
const inStockOnly = ref(false);
const selectedIds = ref([]);
const isBatchDeleting = ref(false);
const dateRange = ref([]);
const filterIsNew = ref(null);
const filterIsFaulty = ref(null);
const showImport = ref(false)
const deleteDlg = reactive({ open: false, row: null, name: '', isFaulty: false });
const batchDeleteDlg = ref(false);

const activeFilterCount = computed(() =>
  [keyword.value, categoryIds.value?.length, filterTagId.value, inStockOnly.value || null, dateRange.value?.length, filterIsNew.value, filterIsFaulty.value]
    .filter(v => v !== null && v !== undefined && v !== '' && v !== false && v !== 0).length
);

const pageNumbers = computed(() => {
  const total = Math.ceil(totalElements.value / 20);
  const p = page.value + 1;
  const range = [];
  for (let i = Math.max(1, p - 2); i <= Math.min(total, p + 2); i++) range.push(i);
  return range;
});

function clearFilters() {
  keyword.value = ''; categoryIds.value = []; filterTagId.value = null;
  sortBy.value = 'newest_arrival'; inStockOnly.value = false;
  dateRange.value = []; filterIsNew.value = null; filterIsFaulty.value = null;
  onFilter();
}
function toggleSelectAll(e) {
  if (e.target.checked) selectedIds.value = rows.value.map(r => r.id);
  else selectedIds.value = [];
}
function toggleRowSelect(id) {
  const i = selectedIds.value.indexOf(id);
  if (i >= 0) selectedIds.value.splice(i, 1);
  else selectedIds.value.push(id);
}
function openDeleteConfirm(row) {
  deleteDlg.row = row; deleteDlg.name = row.name; deleteDlg.isFaulty = row.isFaulty; deleteDlg.open = true;
}
async function confirmDelete() {
  await onDelete(deleteDlg.row);
  deleteDlg.open = false;
}
function confirmBatchDelete() { batchDeleteDlg.value = true; }

function formatDate(dateStr) {
  if (!dateStr) return "—";
  return new Date(dateStr).toLocaleString("vi-VN", { dateStyle: 'short', timeStyle: 'short' });
}
function fixImageUrl(url) {
  if (!url) return "https://via.placeholder.com/150?text=No+Image";
  if (url.startsWith("http")) return url;
  return `${BASE_URL_API}${url}`;
}
function normalizeProducts(list) {
  return (list || []).map((p) => ({ ...p, imageUrl: fixImageUrl(p.imageUrl) }));
}
function formatCurrency(val) {
  if (!val) return '0 ₫';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
}

async function loadCategories() {
  try {
    const res = await categoriesApi.list(false);
    categories.value = res.data?.data || res.data || [];
  } catch (e) { console.error(e); }
}
async function loadTags() {
  try {
    const res = await http.get('/api/tags');
    tags.value = res.data?.data || res.data || [];
  } catch (e) { console.error("Lỗi lấy tag:", e); }
}

const serialDlg = reactive({
  open: false, variantId: null, variantName: "", list: [], inputText: "", genQuantity: 1, loading: false, adding: false, addingManual: false
});

async function load() {
  loading.value = true;
  try {
    let res;
    if (viewMode.value === 'trash') {
      res = await axios.get(`${BASE_URL_API}/api/products/trash`, { params: { page: page.value } });
    } else {
      const params = {
        page: page.value, keyword: keyword.value || undefined, sortBy: sortBy.value || undefined,
        inStockOnly: inStockOnly.value, tagId: filterTagId.value || undefined,
        startDate: dateRange.value?.[0] ? `${dateRange.value[0]}T00:00:00` : undefined,
        endDate: dateRange.value?.[1] ? `${dateRange.value[1]}T23:59:59` : undefined,
        isNew: filterIsNew.value === null ? undefined : filterIsNew.value,
        isFaulty: filterIsFaulty.value === null ? undefined : filterIsFaulty.value
      };
      if (categoryIds.value?.length > 0) params.categoryIds = categoryIds.value.join(',');
      res = await productsApi.list(params);
    }
    const pageData = res.data?.data || res.data;
    rows.value = normalizeProducts(pageData.content || []);
    totalElements.value = pageData.totalElements || 0;
  } catch (e) {
    toast("Không tải được sản phẩm.", "error");
  } finally {
    loading.value = false;
  }
}

function onFilter() { page.value = 0; load(); }
function onPageChange(val) { page.value = val - 1; load(); }

async function handleBatchDelete() {
  if (selectedIds.value.length === 0) return;
  isBatchDeleting.value = true;
  try {
    await productsApi.batchDelete(selectedIds.value);
    toast(`Đã chuyển ${selectedIds.value.length} sản phẩm vào thùng rác`, "success");
    selectedIds.value = [];
    batchDeleteDlg.value = false;
    await load();
  } catch (e) {
    toast("Lỗi khi xóa hàng loạt", "error");
  } finally {
    isBatchDeleting.value = false;
  }
}

async function toggleProductStatus(row) {
  try {
    if (row.isVisible) {
      await axios.put(`${BASE_URL_API}/api/products/${row.id}/restore`);
      toast(`Đã bật hiển thị: ${row.name}`, "success");
    } else {
      await axios.delete(`${BASE_URL_API}/api/products/${row.id}`);
      toast(`Đã tắt (ẩn) sản phẩm: ${row.name}`, "warning");
    }
  } catch (e) {
    toast("Lỗi cập nhật trạng thái", "error");
    row.isVisible = !row.isVisible;
  }
}

const batchDlg = reactive({
  open: false, loading: false,
  form: { isVisible: null, isNew: null, isFaulty: null, tagIds: [] }
});
function openBatchUpdateDialog() {
  batchDlg.form = { isVisible: null, isNew: null, isFaulty: null, tagIds: [] };
  batchDlg.open = true;
}
async function submitBatchUpdate() {
  batchDlg.loading = true;
  try {
    const payload = {
      ids: selectedIds.value, isVisible: batchDlg.form.isVisible,
      isNew: batchDlg.form.isNew, isFaulty: batchDlg.form.isFaulty,
      tagIds: batchDlg.form.tagIds.length > 0 ? batchDlg.form.tagIds : null
    };
    await productsApi.batchUpdate(payload);
    toast("Cập nhật hàng loạt thành công!", "success");
    batchDlg.open = false; selectedIds.value = []; await load();
  } catch (e) {
    toast("Lỗi cập nhật hàng loạt", "error");
  } finally {
    batchDlg.loading = false;
  }
}

const vr = reactive({
  open: false, productId: null, productName: "", variants: [], loading: false, saving: false,
  isEdit: false, editId: null, activeTab: 'list',  // ← thêm dòng này
  form: { variantName: "", sku: "", price: 0, stockQuantity: 0, isActive: true },
  attrsList: [{ key: "", value: "" }]
});

async function openVariantDrawer(row) {
  vr.productId = row.id; vr.productName = row.name;
  vr.activeTab = 'list';  // ← thêm dòng này
  vr.open = true;
  resetVariantForm(); await loadVariants();
}
async function loadVariants() {
  vr.loading = true;
  try {
    const res = await axios.get(`${BASE_URL_API}/api/products/${vr.productId}/variants`);
    vr.variants = res.data || [];
  } catch (e) { toast("Tải biến thể thất bại", "error"); }
  vr.loading = false;
}
function resetVariantForm() {
  vr.isEdit = false; vr.editId = null;
  vr.form = { variantName: "", sku: "", price: 0, stockQuantity: 0, isActive: true };
  vr.attrsList = [{ key: "", value: "" }];
}
function addVariantAttr() { vr.attrsList.push({ key: "", value: "" }); }
function removeVariantAttr(index) { vr.attrsList.splice(index, 1); }
function editVariant(row) {
  vr.isEdit = true; vr.editId = row.id;
  vr.form = { variantName: row.variantName, sku: row.sku, price: row.price, stockQuantity: row.stockQuantity, isActive: row.isActive };
  try {
    const obj = JSON.parse(row.attributesJson || '{}');
    const keys = Object.keys(obj);
    vr.attrsList = keys.length > 0 ? keys.map(k => ({ key: k, value: obj[k] })) : [{ key: "", value: "" }];
  } catch { vr.attrsList = [{ key: "", value: "" }]; }
}
async function saveVariant() {
  if (!vr.form.variantName || !vr.form.sku) return toast("Vui lòng điền các trường bắt buộc", "warning");
  vr.saving = true;
  try {
    const attrObj = {};
    vr.attrsList.forEach(item => { if (item.key && item.value) attrObj[item.key.trim()] = item.value.trim(); });
    const payload = { ...vr.form, attributesJson: Object.keys(attrObj).length > 0 ? JSON.stringify(attrObj) : null };
    if (vr.isEdit) await axios.put(`${BASE_URL_API}/api/products/variants/${vr.editId}`, payload);
    else await axios.post(`${BASE_URL_API}/api/products/${vr.productId}/variants`, payload);
    toast("Thành công", "success");
    resetVariantForm(); await loadVariants(); load();
  } catch { toast("Thất bại", "error"); }
  vr.saving = false;
}
async function deleteVariant(id) {
  try { await axios.delete(`${BASE_URL_API}/api/products/variants/${id}`); toast("Đã xóa", "success"); await loadVariants(); load(); }
  catch { toast("Thất bại", "error"); }
}
async function openSerialDialog(row) {
  serialDlg.variantId = row.id; serialDlg.variantName = row.variantName;
  serialDlg.inputText = ""; serialDlg.open = true; await loadSerials();
}
async function loadSerials() {
  serialDlg.loading = true;
  try {
    const res = await http.get(`/api/products/variants/${serialDlg.variantId}/serials`);
    serialDlg.list = res.data || [];
  } catch (e) { toast("Lỗi tải danh sách seri", "error"); }
  finally { serialDlg.loading = false; }
}
async function deleteSerial(serialId) {
  try {
    await http.delete(`/api/products/variants/serials/${serialId}`);
    toast("Xóa Seri thành công", "success");
    await loadSerials(); await loadVariants(); await load();
  } catch (e) { toast("Xóa thất bại", "error"); }
}
async function importSerialsExcel(fileObj) {
  serialDlg.adding = true;
  try {
    const res = await productsApi.importVariantSerials(serialDlg.variantId, fileObj.raw);
    const data = res.data; // Lấy dữ liệu Map từ Backend
    
    if (data.errorCount > 0) {
        // CÓ TRÙNG LẶP MỘT PHẦN: Hiển thị tối đa 5 mã lỗi, còn lại gom vào "..."
        const errorListHtml = data.errors.slice(0, 5).map(err => `<li>${err}</li>`).join('');
        const more = data.errors.length > 5 ? `<li><i>... và ${data.errors.length - 5} mã khác bị trùng</i></li>` : '';
        
        ElNotification({
          title: 'Cảnh báo trùng lặp',
          dangerouslyUseHTMLString: true,
          message: `<div style="font-size: 13px;"><b>${data.message}</b><ul style="padding-left: 15px; margin-top: 8px; color: #f56c6c;">${errorListHtml}${more}</ul></div>`,
          type: 'warning',
          duration: 6000
        });
    } else {
        toast(data.message, "success");
    }

    await loadSerials(); 
    await loadVariants(); 
    await load();
  } catch (e) {
    const errData = e.response?.data;
    if (errData && errData.errors) {
         // LỖI TRÙNG LẶP 100%
         const errorListHtml = errData.errors.slice(0, 5).map(err => `<li>${err}</li>`).join('');
         const more = errData.errors.length > 5 ? `<li><i>... và ${errData.errors.length - 5} mã khác bị trùng</i></li>` : '';
         
         ElNotification({
          title: 'Lỗi nhập Excel',
          dangerouslyUseHTMLString: true,
          message: `<div style="font-size: 13px;"><b>${errData.message}</b><ul style="padding-left: 15px; margin-top: 8px; color: #f56c6c;">${errorListHtml}${more}</ul></div>`,
          type: 'error',
          duration: 6000
        });
    } else {
        toast(typeof errData === 'string' ? errData : (errData?.error || "Lỗi file Excel"), "error");
    }
  } finally {
    serialDlg.adding = false;
  }
}
async function addManualSerial() {
  const val = serialDlg.inputText?.trim();
  if (!val) return;
  
  // Rào độ dài cho chuẩn laptop
  if (val.length < 5) {
    return toast("Seri laptop phải có độ dài tối thiểu 5 ký tự!", "warning");
  }

  serialDlg.addingManual = true;
  try {
    const res = await productsApi.addSerials(serialDlg.variantId, [val]);
    toast(res.data || "Thêm thủ công thành công", "success");
    serialDlg.inputText = "";
    await loadSerials();
    await loadVariants();
    await load();
  } catch (e) {
    const errMessage = typeof e.response?.data === 'string' ? e.response.data : "Lỗi kết nối hoặc Seri không hợp lệ";
    toast(errMessage, "error");
  } finally {
    serialDlg.addingManual = false;
  }
}
async function onRestore(id) {
  try { await axios.put(`${BASE_URL_API}/api/products/${id}/restore`); toast("Khôi phục thành công!", "success"); load(); }
  catch { toast("Khôi phục thất bại", "error"); }
}
async function onDelete(row) {
  try {
    if (row.isFaulty) {
      await axios.delete(`${BASE_URL_API}/api/products/${row.id}/hard`);
      toast("Đã xóa vĩnh viễn sản phẩm lỗi.", "success");
    } else {
      await axios.delete(`${BASE_URL_API}/api/products/${row.id}`);
      toast("Đã chuyển vào thùng rác.", "success");
    }
    load();
  } catch { toast("Xóa thất bại.", "error"); }
}
async function setPrimaryImage(imgId) {
  try {
    await axios.put(`${BASE_URL_API}/api/products/${dlg.editId}/images/${imgId}/primary`);
    toast("Cập nhật ảnh chính thành công", "success");
    const res = await productsApi.get(dlg.editId);
    dlg.existingImages = (res.data?.data || res.data).images || [];
  } catch { toast("Thất bại", "error"); }
}

const dlg = reactive({
  open: false, isEdit: false, loading: false, alert: "", editId: null, attributesList: [], existingImages: [], idsToDelete: [],
  activeTab: 'info', history: [], historyLoading: false,
  
  form: { name: "", sku: "", brand: "", description: "", isVisible: true, categoryIds: [], galleryImages: [], tagIds: [], isNew: true, isFaulty: false },
});

function getLogType(severity) {
  if (severity === 'HIGH' || severity === 'CRITICAL') return 'danger';
  if (severity === 'MEDIUM') return 'warning';
  return 'primary';
}
function openCreateDialog() {
  dlg.isEdit = false; dlg.editId = null; dlg.attributesList = [{ name: "", value: "" }];
  dlg.existingImages = []; dlg.idsToDelete = []; dlg.activeTab = 'info';
  
  dlg.form = { name: "", sku: "", brand: "", description: "", isVisible: true, categoryIds: [], galleryImages: [], tagIds: [], isNew: true, isFaulty: false };
  dlg.alert = ""; dlg.open = true;
}
async function onEdit(row) {
  dlg.isEdit = true; dlg.editId = row.id; dlg.idsToDelete = []; dlg.existingImages = [];
  dlg.activeTab = 'info'; dlg.open = true; dlg.loading = true; dlg.historyLoading = true;
  try {
    const [detailRes, historyRes] = await Promise.all([
      productsApi.get(row.id),
      productsApi.getHistory(row.id).catch(() => ({ data: [] }))
    ]);
    const data = detailRes.data?.data || detailRes.data;
    dlg.form = {
      name: data.name, sku: data.sku, 
      brand: data.brand || "", 
      description: data.description, isVisible: data.isVisible,
      categoryIds: data.categoryId ? [data.categoryId] : [], galleryImages: [], tagIds: [],
      isNew: data.isNew, isFaulty: data.isFaulty
    };
    if (data.tags?.length > 0) {
      dlg.form.tagIds = data.tags.map(name => tags.value.find(t => t.name === name)?.id).filter(id => id);
    }
    dlg.existingImages = data.images || [];
    try {
      const attrs = JSON.parse(data.attributes);
      dlg.attributesList = Array.isArray(attrs) ? attrs : [];
    } catch { dlg.attributesList = []; }
    dlg.history = historyRes.data || [];
  } catch { toast("Tải dữ liệu thất bại", "error"); }
  finally { dlg.loading = false; dlg.historyLoading = false; }
}
function markImageForDelete(imageId) {
  dlg.idsToDelete.push(imageId);
  dlg.existingImages = dlg.existingImages.filter(img => img.id !== imageId);
}
function addAttribute() { dlg.attributesList.push({ name: "", value: "" }); }
function removeAttribute(index) { dlg.attributesList.splice(index, 1); }
function onPickFiles(e) { dlg.form.galleryImages = Array.from(e.target.files); }

async function submitForm() {
  if (!dlg.form.name || !dlg.form.sku) return toast("Vui lòng nhập Tên và SKU", "warning");
  dlg.loading = true;
  try {
    const formData = new FormData();
    formData.append("name", String(dlg.form.name || ""));
    formData.append("sku", String(dlg.form.sku || ""));
    
    formData.append("brand", String(dlg.form.brand || ""));
    
    formData.append("description", String(dlg.form.description || ""));
    formData.append("isVisible", String(dlg.form.isVisible));
    formData.append("isNew", String(dlg.form.isNew));
    
    formData.append("isFaulty", String(dlg.form.isFaulty));
    
    if (Array.isArray(dlg.form.categoryIds) && dlg.form.categoryIds.length > 0)
      dlg.form.categoryIds.forEach(id => formData.append("categoryIds", id?.id ?? id));
    if (Array.isArray(dlg.form.tagIds) && dlg.form.tagIds.length > 0)
      dlg.form.tagIds.forEach(id => formData.append("tagIds", id?.id ?? id));
    if (Array.isArray(dlg.idsToDelete) && dlg.idsToDelete.length > 0)
      dlg.idsToDelete.forEach(id => formData.append("idsToDelete", id?.id ?? id));
    if (dlg.form.galleryImages && dlg.form.galleryImages.length > 0)
      for (let i = 0; i < dlg.form.galleryImages.length; i++) {
        let file = dlg.form.galleryImages[i];
        if (file instanceof File || file instanceof Blob) formData.append("galleryImages", file);
      }
    const validAttrs = dlg.attributesList.filter(a => a.name?.trim() && a.value?.trim());
    if (validAttrs.length > 0) formData.append("attributes", JSON.stringify(validAttrs));
    
    if (dlg.isEdit) await productsApi.update(dlg.editId, formData);
    else await productsApi.create(formData);
    
    dlg.open = false; await load(); toast("Thành công", "success");
  } catch (e) {
    console.error("LỖI GỬI LÊN:", e);
    console.error("RESPONSE DATA:", JSON.stringify(e.response?.data)); 
    toast("Lỗi: " + (e.response?.data?.message || e.message), "error");
  } finally { dlg.loading = false; }
}

onMounted(async () => {
  await loadCategories();
  await loadTags();
  await load();
});
</script>

<style></style>