<template>
  <div class="pm-page">

    <!-- Header -->
    <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:20px; margin-bottom:22px; flex-wrap:wrap;">
      <div>
        <div style="font-size:11px; font-weight:700; letter-spacing:0.08em; text-transform:uppercase; color:var(--el-text-color-secondary); margin-bottom:6px; display:flex; align-items:center; gap:5px;">
            <el-icon><Present /></el-icon> Marketing
          </div>
        <div style="font-size:28px; font-weight:800; letter-spacing:-0.03em; margin-bottom:4px;">Quản lý khuyến mãi</div>
        <div style="font-size:13px; color:var(--el-text-color-secondary);">Tạo và quản lý các chiến dịch khuyến mãi, xung đột và báo cáo</div>
      </div>
      <div style="display:flex; align-items:center; gap:8px; flex-shrink:0; padding-top:4px; flex-wrap:wrap;">
        <el-button plain :loading="conflictLoading"     @click="loadConflicts"      :disabled="conflictLoading">
          <template #icon><el-icon><Warning /></el-icon></template>
          Xung đột
        </el-button>
        <el-button plain :loading="expiringLoading"     @click="loadExpiring"       :disabled="expiringLoading">
          <template #icon><el-icon><Clock /></el-icon></template>
          Sắp hết hạn
        </el-button>
        <el-button plain :loading="activeReportLoading" @click="loadActiveReport"   :disabled="activeReportLoading">
          <template #icon><el-icon><Document /></el-icon></template>
          Đang hoạt động
        </el-button>
        <el-button plain :loading="reportLoading || summaryReportLoading" @click="loadCombinedReport" :disabled="reportLoading || summaryReportLoading">
          <template #icon><el-icon><DataAnalysis /></el-icon></template>
          Báo cáo
        </el-button>
        <el-button type="primary" @click="openCreate">
          <template #icon><el-icon><Plus /></el-icon></template>
          Tạo mới
        </el-button>
      </div>
    </div>

    <!-- ── Validate bar ── -->
    <el-card shadow="never">
      <div class="validate-row">
        <el-icon :size="16"><Search /></el-icon>
        <span class="validate-label">Validate mã KM</span>
        <div class="validate-field">
          <label class="validate-field-tag">CODE</label>
          <input
            class="validate-input"
            v-model="validateCode"
            placeholder="VD: GAMING10"
          />
        </div>
        <div class="validate-field">
          <label class="validate-field-tag">₫</label>
          <input
            class="validate-input validate-input--num"
            v-model.number="validateTotal"
            placeholder="Tổng đơn hàng"
            type="number"
          />
        </div>
        <el-button type="primary" :loading="validateLoading" @click="doValidate" :disabled="validateLoading">
          <template #icon><el-icon><Check /></el-icon></template>
          Kiểm tra
        </el-button>
        <el-button v-if="validateResult" text @click="validateResult = null">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
      <div v-if="validateResult" class="validate-result-wrap">
        <el-alert
          v-if="validateResult.valid"
          type="success"
          :closable="false"
          show-icon
        >
          Hợp lệ — giảm <strong>{{ fmtMoney(validateResult.discountAmount) }}</strong>
          <el-tag v-if="validateResult.promotionName" type="warning" effect="plain" size="small" class="ml-sm">
            {{ validateResult.promotionName }}
          </el-tag>
        </el-alert>
        <el-alert
          v-else
          type="error"
          :title="validateResult.message || 'Mã không hợp lệ'"
          :closable="false"
          show-icon
        />
      </div>
    </el-card>

    <!-- ── Conflicts panel ── -->
    <el-card v-if="conflicts !== null" shadow="never">
      <template #header>
        <el-row justify="space-between" align="middle">
          <el-space>
            <el-icon><Warning /></el-icon>
            <span>Xung đột khuyến mãi</span>
            <el-tag v-if="conflicts.length" type="danger" effect="plain">{{ conflicts.length }}</el-tag>
          </el-space>
          <el-button text @click="conflicts = null"><el-icon><Close /></el-icon></el-button>
        </el-row>
      </template>
      <el-alert v-if="!conflicts.length" type="success" title="Không có xung đột" :closable="false" show-icon />
      <el-table v-else :data="conflicts" border size="small">
        <el-table-column label="Khuyến mãi 1" prop="promotion1" />
        <el-table-column label="Khuyến mãi 2" prop="promotion2" />
        <el-table-column label="Stackable" align="center" width="120">
          <template #default="{ row }">
            <el-tag :type="row.stackable ? 'success' : 'danger'" effect="plain" size="small">
              {{ row.stackable ? 'Có' : 'Không' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- ── Expiring panel ── -->
    <el-card v-if="expiringRows !== null" shadow="never">
      <template #header>
        <el-row justify="space-between" align="middle">
          <el-space>
            <el-icon><Clock /></el-icon>
            <span>Sắp hết hạn (3 ngày)</span>
            <el-tag :type="expiringRows.length ? 'warning' : 'success'" effect="plain">{{ expiringRows.length }}</el-tag>
          </el-space>
          <el-button text @click="expiringRows = null"><el-icon><Close /></el-icon></el-button>
        </el-row>
      </template>
      <el-alert v-if="!expiringRows.length" type="success" title="Không có KM nào sắp hết hạn" :closable="false" show-icon />
      <el-table v-else :data="expiringRows" border size="small">
        <el-table-column prop="id"   label="ID"     width="70" />
        <el-table-column prop="code" label="Code"   width="130" />
        <el-table-column prop="name" label="Tên" />
        <el-table-column label="Hết hạn" width="120">
          <template #default="{ row }">
            <el-tag type="warning" effect="plain" size="small">{{ fmtDate(row.endDate) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- ── Active report panel ── -->
    <el-card v-if="activeReportData !== null" shadow="never">
      <template #header>
        <el-row justify="space-between" align="middle">
          <el-space>
            <el-icon><Document /></el-icon>
            <span>Khuyến mãi đang áp dụng</span>
            <el-tag type="success" effect="plain">{{ Array.isArray(activeReportData) ? activeReportData.length : 0 }}</el-tag>
          </el-space>
          <el-button text @click="activeReportData = null"><el-icon><Close /></el-icon></el-button>
        </el-row>
      </template>
      <el-alert v-if="!activeReportData?.length" type="info" title="Không có KM nào đang chạy" :closable="false" show-icon />
      <el-table v-else :data="activeReportData" border size="small">
        <el-table-column prop="id"   label="ID"   width="70" />
        <el-table-column prop="code" label="Code" width="130" />
        <el-table-column prop="name" label="Tên" />
        <el-table-column label="Loại giảm" width="120">
          <template #default="{ row }">
            <el-tag type="primary" effect="plain" size="small">
              {{ row.discountType === 'PERCENT' ? '% Giảm' : 'Số tiền' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Hết hạn" width="120">
          <template #default="{ row }">
            <el-tag type="warning" effect="plain" size="small">{{ fmtDate(row.endDate) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- ── Combined report panel ── -->
    <el-card v-if="reportData !== null || summaryReportData !== null" shadow="never">
      <template #header>
        <el-row justify="space-between" align="middle">
          <el-space>
            <el-icon><DataAnalysis /></el-icon>
            <span>Báo cáo khuyến mãi</span>
            <el-radio-group v-model="reportPeriod" size="small" @change="loadCombinedReport">
              <el-radio-button value="week">Tuần</el-radio-button>
              <el-radio-button value="month">Tháng</el-radio-button>
            </el-radio-group>
          </el-space>
          <el-button text @click="reportData = null; summaryReportData = null"><el-icon><Close /></el-icon></el-button>
        </el-row>
      </template>

      <!-- Stats -->
      <el-divider content-position="left">Tổng quan</el-divider>
      <el-row :gutter="16">
        <el-col :span="5" :xs="12">
          <el-statistic title="Tổng KM" :value="reportData?.total ?? 0" />
        </el-col>
        <el-col :span="5" :xs="12">
          <el-statistic title="Đang chạy" :value="reportData?.activeCount ?? 0" />
        </el-col>
        <el-col :span="5" :xs="12">
          <el-statistic title="Combo" :value="reportData?.comboCount ?? summaryReportData?.comboCount ?? 0" />
        </el-col>
        <el-col :span="5" :xs="12">
          <el-statistic title="Giới hạn lượt" :value="reportData?.usageLimitedCount ?? summaryReportData?.usageLimitedCount ?? 0" />
        </el-col>
        <el-col :span="4" :xs="12">
          <el-statistic
            title="Tổng lượt dùng 🔍"
            :value="summaryReportData?.totalRedemptions ?? reportData?.totalRedemptions ?? 0"
            @click="openRedemptionAllDetail()"
            class="clickable-stat"
          />
        </el-col>
      </el-row>

      <!-- Ratio bars -->
      <el-divider content-position="left">Tỉ lệ hoạt động</el-divider>
      <el-space direction="vertical" fill style="width:100%">
        <div>
          <el-row justify="space-between">
            <el-text>Đang chạy</el-text>
            <el-text type="success">{{ ratioPct('activeCount') }}%</el-text>
          </el-row>
          <el-progress :percentage="ratioPct('activeCount')" :show-text="false" status="success" />
        </div>
        <div>
          <el-row justify="space-between">
            <el-text>Combo</el-text>
            <el-text type="warning">{{ ratioPct('comboCount') }}%</el-text>
          </el-row>
          <el-progress :percentage="ratioPct('comboCount')" :show-text="false" color="#f59e0b" />
        </div>
        <div>
          <el-row justify="space-between">
            <el-text>Giới hạn lượt</el-text>
            <el-text type="primary">{{ ratioPct('usageLimitedCount') }}%</el-text>
          </el-row>
          <el-progress :percentage="ratioPct('usageLimitedCount')" :show-text="false" />
        </div>
      </el-space>
    </el-card>

    <!-- ── Main table ── -->
    <el-card shadow="never" :body-style="{ padding: 0 }">
      <template #header>
        <el-row justify="space-between" align="middle">
          <el-space>
            <el-text><strong>{{ displayRows.length }}</strong> / {{ rows.length }} khuyến mãi</el-text>
            <el-tag v-if="!showInactive" type="success" effect="plain" size="small">Chỉ ACTIVE</el-tag>
          </el-space>
          <el-space>
            <span><el-switch v-model="showInactive" /></span>
            <el-text size="small" :type="showInactive ? '' : 'info'">
              {{ showInactive ? 'Hiện tất cả' : 'Ẩn km tắt' }}
            </el-text>
          </el-space>
        </el-row>
      </template>

      <el-table :data="displayRows" v-loading="loading" border stripe>
        <el-table-column prop="id"   label="ID"   width="70" />
        <el-table-column prop="code" label="Code" width="130">
          <template #default="{ row }">
            <el-tag effect="plain" size="small">{{ row.code }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="Tên chiến dịch" min-width="160" />

        <el-table-column label="Loại giảm" width="140">
          <template #default="{ row }">
            <template v-if="row.buyQty">
              <el-text type="success"><strong>Mua {{ row.buyQty }} tặng {{ row.getQty }}</strong></el-text>
              <div><el-text type="info" size="small">COMBO</el-text></div>
            </template>
            <template v-else>
              <strong>{{ row.discountValue }}{{ row.discountType === 'PERCENT' ? '%' : ' ₫' }}</strong>
              <div><el-text type="info" size="small"><el-icon><component :is="row.discountType === 'PERCENT' ? 'Opportunity' : 'Money'" /></el-icon> {{ row.discountType === 'PERCENT' ? '% Giảm' : 'Số tiền' }}</el-text></div>
            </template>
          </template>
        </el-table-column>

        <el-table-column label="Đối tượng" width="140">
          <template #default="{ row }">
            <el-tag v-if="row.vipTiers?.length"     type="warning" effect="plain" size="small">VIP {{ row.vipTiers.join(', ') }}</el-tag>
            <el-tag v-else-if="row.customerTypes?.length" type="primary" effect="plain" size="small">{{ row.customerTypes.join(', ') }}</el-tag>
            <el-text v-else type="info" size="small">All</el-text>
          </template>
        </el-table-column>

        <el-table-column label="Giới hạn" align="center" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.usageLimit" type="primary" effect="plain" size="small">{{ row.usageLimit }}</el-tag>
            <el-text v-else type="info">∞</el-text>
          </template>
        </el-table-column>

        <el-table-column prop="priority" label="Ưu tiên" align="center" width="80" />

        <el-table-column label="Thời gian" min-width="160">
          <template #default="{ row }">
            <el-space direction="vertical" :size="2" alignment="flex-start">
              <el-text size="small">{{ fmtDate(row.startDate) }}</el-text>
              <el-text size="small" type="info">→ {{ fmtDate(row.endDate) }}</el-text>
            </el-space>
          </template>
        </el-table-column>

        <el-table-column label="Trạng thái" align="center" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'danger'" effect="plain" size="small">
              {{ row.isActive ? 'ACTIVE' : 'OFF' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Thao tác" align="center" width="200">
          <template #default="{ row }">
            <el-space>
              <el-button link type="primary"  size="small" @click="openEdit(row)">Sửa</el-button>
              <el-button link type="info"     size="small" @click="openPreview(row)">Preview</el-button>
              <el-button link type="warning"  size="small" @click="openRedemptionDetail(row.id, row.code)">Lượt dùng</el-button>
              <el-button link type="danger"   size="small" @click="remove(row)">Xóa</el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- ── Preview panel ── -->
    <el-card v-if="previewPromo" shadow="never">
      <template #header>
        <el-row justify="space-between" align="middle" wrap>
          <el-space>
            <el-icon><View /></el-icon>
            <span>Xem giá —</span>
            <el-tag effect="plain">{{ previewPromo.code }}</el-tag>
          </el-space>
          <el-space wrap>
            <el-input v-model.number="previewProductId"  placeholder="Product ID"       type="number" style="width:130px" />
            <el-input v-model.number="previewCustomerId" placeholder="Customer ID (opt)" type="number" style="width:160px" />
            <el-button type="primary" :loading="previewLoading" @click="loadPreviewPrices" :disabled="previewLoading">Tải giá</el-button>
            <el-button text @click="previewPromo = null; previewPrices = []"><el-icon><Close /></el-icon></el-button>
          </el-space>
        </el-row>
      </template>
      <el-empty v-if="previewLoaded && !previewPrices.length" description="Không có dữ liệu" />
      <el-table v-else-if="previewPrices.length" :data="previewPrices" border size="small">
        <el-table-column prop="variantId"   label="Variant ID" width="100" />
        <el-table-column prop="variantName" label="Tên variant" min-width="140" />
        <el-table-column prop="sku"          label="SKU"        width="120">
          <template #default="{ row }"><el-text type="info">{{ row.sku }}</el-text></template>
        </el-table-column>
        <el-table-column prop="currencyCode" label="Currency"   width="90" />
        <el-table-column label="Giá gốc" width="130">
          <template #default="{ row }">{{ fmtMoney(row.price) }}</template>
        </el-table-column>
        <el-table-column label="Giá sau KM" width="130">
          <template #default="{ row }">
            <el-text :type="row.finalPrice < row.price ? 'success' : ''">
              <strong>{{ fmtMoney(row.finalPrice) }}</strong>
            </el-text>
          </template>
        </el-table-column>
        <el-table-column label="Mã KM áp dụng" width="140">
          <template #default="{ row }">
            <el-tag v-if="row.promotionCode" type="warning" effect="plain" size="small">{{ row.promotionCode }}</el-tag>
            <el-text v-else type="info">—</el-text>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- ════ DIALOG: Tạo / Sửa ════ -->
    <el-dialog
      v-model="dlg.open"
      :title="dlg.mode === 'create' ? 'Tạo khuyến mãi mới' : 'Sửa khuyến mãi'"
      width="780px"
      :close-on-click-modal="false"
    >
      <el-alert v-if="dlg.alert" type="error" :title="dlg.alert" :closable="false" show-icon class="mb-md" />

      <el-form label-position="top" size="small">

        <el-divider content-position="left">Thông tin cơ bản</el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="Code *">
              <el-input v-model="dlg.form.code" :disabled="dlg.mode === 'edit'" placeholder="GAMING_Q2_2026" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Tên chiến dịch *">
              <el-input v-model="dlg.form.name" placeholder="Sale Laptop Gaming Q2/2026" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="Mô tả">
          <el-input v-model="dlg.form.description" type="textarea" :rows="2" placeholder="Mô tả ngắn..." />
        </el-form-item>

        <el-divider content-position="left">Giảm giá</el-divider>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="Loại giảm">
              <el-select v-model="dlg.form.discountType" style="width:100%">
                <el-option value="PERCENT" label="PERCENT (%)" />
                <el-option value="AMOUNT"  label="AMOUNT (₫)"  />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Giá trị giảm">
              <el-input v-model.number="dlg.form.discountValue" type="number" :min="0" placeholder="10" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Ưu tiên (priority)">
              <el-input v-model.number="dlg.form.priority" type="number" :min="0" placeholder="1" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">🎁 Combo <el-text type="info" size="small">(để trống nếu không phải combo)</el-text></el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="Số lượng mua (buyQty)">
              <el-input v-model.number="dlg.form.buyQty" type="number" :min="0" placeholder="VD: 2" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Số lượng tặng (getQty)">
              <el-input v-model.number="dlg.form.getQty" type="number" :min="0" placeholder="VD: 1" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">Thời gian & Giới hạn</el-divider>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="Ngày bắt đầu">
              <el-input v-model="dlg.form.startDate" type="datetime-local" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Ngày kết thúc">
              <el-input v-model="dlg.form.endDate" type="datetime-local" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Giới hạn lượt dùng">
              <el-input v-model.number="dlg.form.usageLimit" type="number" :min="0" placeholder="Không giới hạn" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">Phạm vi áp dụng</el-divider>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="Scope">
              <el-select v-model="dlg.form.scope" style="width:100%">
                <el-option value="ALL"     label="ALL" />
                <el-option value="PRODUCT" label="PRODUCT" />
                <el-option value="VARIANT" label="VARIANT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Stackable">
              <el-switch v-model="dlg.form.stackable" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Kích hoạt">
              <el-switch v-model="dlg.form.isActive" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="Product IDs (cách nhau bởi dấu phẩy)">
              <el-input v-model="dlg.form.productIdsText" placeholder="1,2,3" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Variant IDs (cách nhau bởi dấu phẩy)">
              <el-input v-model="dlg.form.variantIdsText" placeholder="10,11,12" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">👥 Đối tượng khách hàng <el-text type="info" size="small">(để trống = tất cả)</el-text></el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="Loại khách hàng (customerTypes)">
              <el-checkbox-group v-model="dlg.form.customerTypes">
                <el-checkbox v-for="t in ['VIP', 'REGULAR', 'NEW']" :key="t" :value="t">{{ t }}</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Hạng VIP (vipTiers)">
              <el-checkbox-group v-model="dlg.form.vipTiers">
                <el-checkbox v-for="t in ['BRONZE', 'SILVER', 'GOLD', 'PLATINUM', 'DIAMOND']" :key="t" :value="t">{{ t }}</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="dlg.open = false">Hủy bỏ</el-button>
        <el-button
          :type="dlg.mode === 'create' ? 'primary' : 'warning'"
          :loading="dlg.loading"
          @click="save"
          :disabled="dlg.loading"
        >
          <template #icon>
            <el-icon v-if="dlg.mode === 'create'"><Plus /></el-icon>
            <el-icon v-else><Edit /></el-icon>
          </template>
          {{ dlg.mode === 'create' ? 'Tạo mới' : 'Lưu thay đổi' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ════ DIALOG: Lượt dùng ════ -->
    <el-dialog
      v-model="redemptionModal.open"
      width="860px"
      :close-on-click-modal="false"
    >
      <template #header>
        <el-space>
          <el-icon><CreditCard /></el-icon>
          <span>Chi tiết lượt dùng</span>
          <el-tag effect="plain" size="small">{{ redemptionModal.promotionCode }}</el-tag>
          <el-tag type="danger" effect="plain" size="small">{{ redemptionModal.rows.length }} lượt</el-tag>
        </el-space>
      </template>

      <div v-if="redemptionModal.loading" class="loading-center">
        <el-icon class="is-loading"><Loading /></el-icon> Đang tải...
      </div>
      <el-empty v-else-if="!redemptionModal.rows.length" description="Chưa có lượt dùng nào" />
      <el-table v-else :data="redemptionModal.rows" border size="small">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column label="Mã đơn" width="130">
          <template #default="{ row }">
            <el-tag effect="plain" size="small">{{ row.orderNumber }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Khách hàng" min-width="140">
          <template #default="{ row }">
            <div>{{ row.customerName && row.customerName !== '—' ? row.customerName : '—' }}</div>
            <el-text v-if="row.customerId" type="info" size="small">#{{ row.customerId }}</el-text>
          </template>
        </el-table-column>
        <el-table-column label="Giảm được" align="right" width="120">
          <template #default="{ row }">
            <el-text type="success"><strong>{{ fmtMoney(row.discountTotal) }}</strong></el-text>
          </template>
        </el-table-column>
        <el-table-column label="Tổng đơn" align="right" width="120">
          <template #default="{ row }"><strong>{{ fmtMoney(row.totalAmount) }}</strong></template>
        </el-table-column>
        <el-table-column label="Thời gian" align="center" width="130">
          <template #default="{ row }">
            <div>{{ row.usedAt ? new Date(row.usedAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : '—' }}</div>
            <el-text type="info" size="small">{{ row.usedAt ? new Date(row.usedAt).toLocaleDateString('vi-VN') : '' }}</el-text>
          </template>
        </el-table-column>
        <el-table-column label="Trạng thái" align="center" width="110">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 'DELIVERED' ? 'success' : row.status === 'CANCELLED' ? 'danger' : row.status === 'PENDING' ? 'warning' : ''"
              effect="plain"
              size="small"
            >{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="redemptionModal.open = false">Đóng</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from "vue";
import {
  Present, Warning, Clock, Document, DataAnalysis, Plus,
  Search, Check, Close, View, Edit, CreditCard, Loading, Refresh,
  CircleCheck, CircleClose, Ticket, Opportunity, Money
} from "@element-plus/icons-vue";
import { promotionsApi } from "../../api/promotions.api";
import { pricesApi }     from "../../api/prices.api";
import { toast }         from "../../ui/toast";
import { confirmModal }  from "../../ui/confirm";
import "../../assets/styles/promotion-manager.css";

const loading    = ref(false);
const activeOnly = ref(false);
const rows       = ref([]);
const showInactive = ref(false);

const displayRows = computed(() =>
  showInactive.value ? rows.value : rows.value.filter((r) => r.isActive),
);

function extractList(p) {
  if (!p) return [];
  if (Array.isArray(p)) return p;
  const r = p?.data ?? p;
  if (Array.isArray(r)) return r;
  for (const k of ["content", "items", "results", "rows", "list"]) {
    if (Array.isArray(r?.[k])) return r[k];
    if (Array.isArray(r?.data?.[k])) return r.data[k];
  }
  return [];
}

function normalize(list) {
  return (list || []).map((p) => {
    const rulesObj = (() => { try { return p?.rulesJson ? JSON.parse(p.rulesJson) : {}; } catch { return {}; } })();
    const combo    = rulesObj?.combo ?? null;
    const appObj   = (() => { try { return p?.applicabilityJson ? JSON.parse(p.applicabilityJson) : {}; } catch { return {}; } })();
    return {
      id:            p?.id ?? p?.promotionId,
      code:          p?.code ?? "",
      name:          p?.name ?? "",
      description:   p?.description ?? "",
      discountType:  p?.discountType ?? "",
      discountValue: p?.discountValue ?? 0,
      startDate:     p?.startDate ?? "",
      endDate:       p?.endDate ?? "",
      scope:         appObj?.scope ?? p?.scope ?? "ALL",
      productIds:    appObj?.product_ids ?? p?.productIds ?? [],
      variantIds:    appObj?.variant_ids ?? p?.variantIds ?? [],
      priority:      p?.priority ?? 0,
      stackable:     !!p?.stackable,
      isActive:      !!p?.isActive,
      buyQty:        combo?.buy_qty ?? p?.buyQty ?? null,
      getQty:        combo?.get_qty ?? p?.getQty ?? null,
      usageLimit:    rulesObj?.usage_limit ?? p?.usageLimit ?? null,
      customerTypes: appObj?.customer_types ?? p?.customerTypes ?? [],
      vipTiers:      appObj?.vip_tiers ?? p?.vipTiers ?? [],
    };
  });
}

async function load() {
  loading.value = true;
  try {
    const r = await promotionsApi.list(activeOnly.value);
    rows.value = normalize(extractList(r?.data)).sort((a, b) => Number(b.isActive) - Number(a.isActive));
  } catch {
    rows.value = [];
    toast("Tải thất bại.", "error");
  } finally {
    loading.value = false;
  }
}

// ── Validate ──────────────────────────────────────────────
const validateCode    = ref("");
const validateTotal   = ref(0);
const validateLoading = ref(false);
const validateResult  = ref(null);

async function doValidate() {
  if (!validateCode.value) return toast("Nhập mã KM.", "warning");
  validateLoading.value = true;
  validateResult.value  = null;
  try {
    const r = await promotionsApi.validate(validateCode.value, validateTotal.value || 0);
    validateResult.value = r?.data?.data ?? r?.data ?? null;
  } catch (e) {
    validateResult.value = { valid: false, message: e?.response?.data?.message || "Mã không hợp lệ" };
  } finally {
    validateLoading.value = false;
  }
}

// ── Conflicts ─────────────────────────────────────────────
const conflictLoading = ref(false);
const conflicts       = ref(null);

async function loadConflicts() {
  conflictLoading.value = true;
  try {
    const r = await promotionsApi.getConflicts();
    const raw = r?.data?.data ?? r?.data ?? [];
    conflicts.value = Array.isArray(raw) ? raw : [];
  } catch { toast("Tải conflicts thất bại.", "error"); }
  finally { conflictLoading.value = false; }
}

// ── Expiring ──────────────────────────────────────────────
const expiringLoading = ref(false);
const expiringRows    = ref(null);

async function loadExpiring() {
  expiringLoading.value = true;
  try {
    const r = await promotionsApi.getExpiring(3);
    expiringRows.value = r?.data?.data ?? r?.data ?? [];
  } catch { toast("Tải sắp hết hạn thất bại.", "error"); }
  finally { expiringLoading.value = false; }
}

// ── Active report ─────────────────────────────────────────
const activeReportLoading = ref(false);
const activeReportData    = ref(null);

async function loadActiveReport() {
  activeReportLoading.value = true;
  try {
    const r = await promotionsApi.getActiveReport();
    activeReportData.value = r?.data?.data ?? r?.data ?? [];
  } catch { toast("Tải báo cáo thất bại.", "error"); }
  finally { activeReportLoading.value = false; }
}

// ── Combined report ───────────────────────────────────────
const reportLoading        = ref(false);
const summaryReportLoading = ref(false);
const reportData           = ref(null);
const summaryReportData    = ref(null);
const reportPeriod         = ref("month");

async function loadCombinedReport() {
  reportLoading.value = summaryReportLoading.value = true;
  try {
    const [r1, r2] = await Promise.all([
      promotionsApi.getReport(reportPeriod.value),
      promotionsApi.getSummaryReport(reportPeriod.value),
    ]);
    reportData.value        = r1?.data?.data ?? r1?.data ?? {};
    summaryReportData.value = r2?.data?.data ?? r2?.data ?? null;
  } catch { toast("Tải báo cáo thất bại.", "error"); }
  finally { reportLoading.value = summaryReportLoading.value = false; }
}

function ratioPct(key) {
  const total = reportData.value?.total;
  if (!total) return 0;
  const val = reportData.value?.[key] ?? summaryReportData.value?.[key] ?? 0;
  return Math.round((val / total) * 100);
}

// ── Preview ───────────────────────────────────────────────
const previewPromo      = ref(null);
const previewProductId  = ref(null);
const previewCustomerId = ref(null);
const previewLoading    = ref(false);
const previewPrices     = ref([]);
const previewLoaded     = ref(false);

function openPreview(row) {
  previewPromo.value      = row;
  previewPrices.value     = [];
  previewLoaded.value     = false;
  previewProductId.value  = null;
  previewCustomerId.value = null;
}

async function loadPreviewPrices() {
  if (!previewProductId.value) return toast("Nhập Product ID.", "warning");
  previewLoading.value = true;
  previewLoaded.value  = false;
  try {
    const r = previewCustomerId.value
      ? await pricesApi.listByProductForCustomer(previewProductId.value, previewCustomerId.value)
      : await pricesApi.listByProduct(previewProductId.value);
    previewPrices.value = r?.data?.data ?? r?.data ?? [];
    previewLoaded.value = true;
  } catch { previewPrices.value = []; toast("Tải giá thất bại.", "error"); }
  finally { previewLoading.value = false; }
}

// ── Dialog create/edit ────────────────────────────────────
const dlg = reactive({
  open: false, mode: "create", loading: false, alert: "", id: null,
  form: {
    code: "", name: "", description: "", discountType: "PERCENT", discountValue: 0,
    startDate: "", endDate: "", scope: "ALL", productIdsText: "", variantIdsText: "",
    priority: 0, stackable: false, isActive: true, buyQty: null, getQty: null,
    usageLimit: null, customerTypes: [], vipTiers: [],
  },
});

function toLocalDT(iso) {
  if (!iso) return "";
  try {
    const s = String(iso);
    if (s.length >= 16 && !s.endsWith("Z") && !s.includes("+")) return s.slice(0, 16);
    const d = new Date(iso), pad = (n) => String(n).padStart(2, "0");
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  } catch { return ""; }
}
function fromLocalDT(s) { return !s ? "" : s.length === 16 ? s + ":00" : s; }
function parseIds(t) {
  return String(t || "").split(",").map(x=>x.trim()).filter(Boolean).map(Number).filter(n=>Number.isFinite(n));
}

function openCreate() {
  Object.assign(dlg, { open: true, mode: "create", alert: "", id: null });
  dlg.form = { code:"", name:"", description:"", discountType:"PERCENT", discountValue:0, startDate:"", endDate:"", scope:"ALL", productIdsText:"", variantIdsText:"", priority:0, stackable:false, isActive:true, buyQty:null, getQty:null, usageLimit:null, customerTypes:[], vipTiers:[] };
}
function openEdit(row) {
  Object.assign(dlg, { open:true, mode:"edit", alert:"", id: row.id });
  dlg.form = {
    code: row.code, name: row.name, description: row.description,
    discountType: row.discountType || "PERCENT", discountValue: Number(row.discountValue || 0),
    startDate: toLocalDT(row.startDate), endDate: toLocalDT(row.endDate),
    scope: row.scope || "ALL", productIdsText: (row.productIds || []).join(","),
    variantIdsText: (row.variantIds || []).join(","), priority: Number(row.priority || 0),
    stackable: !!row.stackable, isActive: !!row.isActive,
    buyQty: row.buyQty || null, getQty: row.getQty || null, usageLimit: row.usageLimit || null,
    customerTypes: [...(row.customerTypes || [])], vipTiers: [...(row.vipTiers || [])],
  };
}

async function save() {
  dlg.alert = "";
  if (!dlg.form.code || !dlg.form.name) { dlg.alert = "Code và tên là bắt buộc."; return; }
  const payload = {
    code: dlg.form.code, name: dlg.form.name, description: dlg.form.description,
    discountType: dlg.form.discountType, discountValue: dlg.form.discountValue,
    startDate: fromLocalDT(dlg.form.startDate), endDate: fromLocalDT(dlg.form.endDate),
    scope: dlg.form.scope, productIds: parseIds(dlg.form.productIdsText), variantIds: parseIds(dlg.form.variantIdsText),
    priority: dlg.form.priority, stackable: dlg.form.stackable, isActive: dlg.form.isActive,
    ...(dlg.form.buyQty ? { buyQty: dlg.form.buyQty, getQty: dlg.form.getQty || 1 } : {}),
    ...(dlg.form.usageLimit ? { usageLimit: dlg.form.usageLimit } : {}),
    ...(dlg.form.customerTypes?.length ? { customerTypes: dlg.form.customerTypes } : {}),
    ...(dlg.form.vipTiers?.length ? { vipTiers: dlg.form.vipTiers } : {}),
  };
  dlg.loading = true;
  try {
    if (dlg.mode === "create") { await promotionsApi.create(payload); toast("Đã tạo khuyến mãi.", "success"); }
    else { await promotionsApi.update(dlg.id, payload); toast("Đã cập nhật.", "success"); }
    dlg.open = false;
    await load();
  } catch (e) { dlg.alert = e?.response?.data?.message || e?.message || "Lưu thất bại"; }
  finally { dlg.loading = false; }
}

async function remove(row) {
  const ok = await confirmModal(`Xóa KM #${row.id} (${row.code})?`, "Xác nhận", "Xóa", true);
  if (!ok) return;
  try { await promotionsApi.remove(row.id); toast("Đã xóa.", "success"); await load(); }
  catch { toast("Xóa thất bại.", "error"); }
}

// ── Redemption modal ──────────────────────────────────────
const redemptionModal = reactive({ open:false, loading:false, promotionCode:"", promotionId:null, rows:[] });

async function openRedemptionDetail(promotionId, code) {
  Object.assign(redemptionModal, { open:true, loading:true, promotionCode:code, promotionId, rows:[] });
  try {
    const r = await promotionsApi.getRedemptionDetails(promotionId);
    redemptionModal.rows = r?.data?.data ?? r?.data ?? [];
  } catch { toast("Tải chi tiết lượt dùng thất bại.", "error"); }
  finally { redemptionModal.loading = false; }
}

function openRedemptionAllDetail() {
  Object.assign(redemptionModal, { open:true, loading:false, promotionCode:"ALL", promotionId:null, rows:[] });
}

// ── Helpers ───────────────────────────────────────────────
function fmtDate(iso) {
  if (!iso) return "—";
  try { return new Date(iso).toLocaleDateString("vi-VN"); } catch { return iso; }
}
function fmtMoney(val) {
  if (val == null) return "—";
  return Number(val).toLocaleString("vi-VN") + " ₫";
}

onMounted(load);
</script>

<style scoped>
.pm-page :deep(.el-tag) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: normal;
  vertical-align: middle;
}
.pm-page :deep(.el-tag .el-icon) {
  display: none;
}
.pm-page {
  padding: 32px 40px 60px;
  display: flex;
  flex-direction: column;
  gap: 16px;
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
.ml-sm      { margin-left: 6px; }
.mb-md      { margin-bottom: 12px; }

/* ── Validate bar ── */
.validate-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.validate-label {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
}
.validate-field {
  display: flex;
  align-items: stretch;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  height: 32px;
  transition: border-color .2s;
}
.validate-field:focus-within {
  border-color: #409eff;
}
.validate-field-tag {
  display: flex;
  align-items: center;
  padding: 0 10px;
  background: #f5f7fa;
  border-right: 1px solid #dcdfe6;
  font-size: 12px;
  font-weight: 600;
  color: #909399;
  white-space: nowrap;
  user-select: none;
}
.validate-input {
  border: none;
  outline: none;
  padding: 0 11px;
  font-size: 13px;
  color: #303133;
  background: #fff;
  width: 150px;
  height: 100%;
  font-family: inherit;
}
.validate-input::placeholder {
  color: #c0c4cc;
}
.validate-input--num {
  width: 130px;
}
.validate-input[type="number"]::-webkit-inner-spin-button,
.validate-input[type="number"]::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
.validate-input[type="number"] {
  -moz-appearance: textfield;
  appearance: textfield;
}

.validate-result-wrap { margin-top: 12px; }

.clickable-stat { cursor: pointer; }

.loading-center {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px;
}
</style>