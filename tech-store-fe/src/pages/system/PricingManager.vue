<template>
  <div class="pp-page">

    <!-- Header -->
    <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:20px; margin-bottom:22px; flex-wrap:wrap;">
      <div>
        <div style="font-size:11px; font-weight:700; letter-spacing:0.08em; text-transform:uppercase; color:var(--el-text-color-secondary); margin-bottom:6px; display:flex; align-items:center; gap:5px;">
            <el-icon><TrendCharts /></el-icon> Vận hành & Sản phẩm
          </div>
        <div style="font-size:28px; font-weight:800; letter-spacing:-0.03em; margin-bottom:4px;">Quản lý giá &amp; Xung đột</div>
        <div style="font-size:13px; color:var(--el-text-color-secondary);">Quản lý bảng giá, lịch sử và cảnh báo xung đột toàn hệ thống</div>
      </div>
      <div style="display:flex; align-items:center; gap:8px; flex-shrink:0; padding-top:4px; flex-wrap:wrap;">
        <el-space v-if="dashData" wrap>
          <el-tag type="success" effect="dark">{{ dashData.activePromotions ?? 0 }} KM active</el-tag>
          <el-tag :type="(dashData.variantsBelowCost ?? 0) > 0 ? 'danger' : 'info'" effect="dark">
            {{ dashData.variantsBelowCost ?? 0 }} dưới giá nhập
          </el-tag>
          <el-tag :type="(dashData.expiringIn3Days ?? 0) > 0 ? 'warning' : 'info'" effect="dark">
            {{ dashData.expiringIn3Days ?? 0 }} sắp hết hạn
          </el-tag>
        </el-space>
      </div>
    </div>

    <!-- ── Tabs ── -->
    <el-tabs v-model="activeTab" type="border-card" class="pp-tabs">

      <!-- ── TAB: ĐẶT GIÁ ── -->
      <el-tab-pane label="Đặt giá" name="set">
        <el-row :gutter="20">

          <!-- Card: Đặt giá variant -->
          <el-col :span="12">
            <el-card shadow="never" class="pp-card">
              <template #header><span class="pp-card-title">Đặt giá variant</span></template>

              <el-form label-position="top">
                <el-form-item label="Chọn variant theo sản phẩm">
                  <el-space wrap>
                    <el-input-number v-model="variantPickerProductId" placeholder="Product ID" :controls="false" style="width:130px" @keyup.enter="loadVariantsForPicker" />
                    <el-button plain :loading="variantPickerLoading" @click="loadVariantsForPicker">
                      <el-icon v-if="!variantPickerLoading"><Search /></el-icon>
                      Tải
                    </el-button>
                    <el-tag v-if="setForm.variantId" type="success" effect="plain">Variant #{{ setForm.variantId }}</el-tag>
                    <el-button v-if="setForm.variantId" text @click="setForm.variantId = null; variantList = []; variantPickerOpen = false; currentPricePreview = null">
                      <el-icon><Close /></el-icon>
                    </el-button>
                  </el-space>

                  <!-- Variant dropdown -->
                  <div v-if="variantPickerOpen && variantList.length" class="pp-variant-list">
                    <div
                      v-for="v in variantList"
                      :key="v.id"
                      class="pp-variant-option"
                      :class="{ 'pp-variant-option--selected': setForm.variantId === v.id, 'pp-variant-option--inactive': !v.isActive }"
                      @click="selectVariant(v)"
                    >
                      <el-row justify="space-between" align="middle">
                        <div>
                          <span class="pp-variant-name">{{ v.variantName }}</span>
                          <el-text type="info" size="small" class="pp-ml">{{ v.sku }}</el-text>
                        </div>
                        <el-space>
                          <el-text :type="v.costPrice && v.price < v.costPrice ? 'danger' : ''">{{ fmtMoney(v.price) }}</el-text>
                          <el-text :type="v.stockQuantity === 0 ? 'danger' : 'info'" size="small">Tồn: {{ v.stockQuantity }}</el-text>
                          <el-tag v-if="!v.isActive" type="danger" size="small" effect="plain">Inactive</el-tag>
                        </el-space>
                      </el-row>
                    </div>
                  </div>
                  <el-empty v-else-if="variantPickerOpen && !variantList.length" description="Không có variant nào." :image-size="40" />
                </el-form-item>

                <el-row :gutter="12">
                  <el-col :span="12">
                    <el-form-item label="Variant ID *">
                      <el-input-number v-model="setForm.variantId" placeholder="Hoặc nhập trực tiếp" :controls="false" style="width:100%" @blur="loadCurrentPricePreview" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="Currency">
                      <el-input :value="setForm.currencyCode || 'VND'" readonly />
                    </el-form-item>
                  </el-col>
                </el-row>

                <!-- Giá hiện tại preview -->
                <div v-if="currentPricePreview" class="pp-price-preview">
                  <el-descriptions :column="1" border size="small">
                    <el-descriptions-item label="Giá đang active">
                      <el-text :type="currentPricePreview.belowCost ? 'danger' : 'success'" class="pp-bold">
                        {{ fmtMoney(currentPricePreview.price) }}
                      </el-text>
                    </el-descriptions-item>
                    <el-descriptions-item label="Giá nhập">
                      <el-text type="info">{{ currentPricePreview.costPrice ? fmtMoney(currentPricePreview.costPrice) : '—' }}</el-text>
                    </el-descriptions-item>
                    <el-descriptions-item v-if="currentPricePreview.belowCost" label="Cảnh báo">
                      <el-text type="danger">Giá bán thấp hơn giá nhập</el-text>
                    </el-descriptions-item>
                  </el-descriptions>
                </div>

                <el-row :gutter="12">
                  <el-col :span="12">
                    <el-form-item label="Giá bán">
                      <el-input-number v-model="setForm.price" placeholder="42900000" :controls="false" style="width:100%" />
                      <div v-if="setForm.price" class="pp-field-hint">{{ fmtMoney(setForm.price) }}</div>
                      <div v-if="setForm.price && setForm.costPrice && setForm.price < setForm.costPrice" class="pp-field-hint pp-field-hint--danger">Giá bán thấp hơn giá nhập!</div>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="Giá nhập">
                      <el-input-number v-model="setForm.costPrice" placeholder="36000000" :controls="false" style="width:100%" />
                      <div v-if="setForm.costPrice" class="pp-field-hint">{{ fmtMoney(setForm.costPrice) }}</div>
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-form-item label="Lý do">
                  <el-input v-model="setForm.reason" placeholder="MANUAL / PROMO_ENDED / PRICE_UPDATE" />
                </el-form-item>
              </el-form>

              <el-button type="primary" :loading="setLoading" @click="setPrice" style="width:100%">
                <el-icon v-if="!setLoading"><FolderChecked /></el-icon>
                Lưu giá
              </el-button>
            </el-card>
          </el-col>

          <!-- Card: Giá hiệu lực -->
          <el-col :span="12">
            <el-card shadow="never" class="pp-card">
              <template #header><span class="pp-card-title">Giá hiệu lực</span></template>

              <el-text type="info" size="small">Dùng Variant ID đã nhập bên trái.</el-text>

              <div class="pp-mt">
                <el-button plain :loading="effLoading" @click="loadEffective">
                  <el-icon v-if="!effLoading"><Lightning /></el-icon>
                  Tải giá hiệu lực
                </el-button>
              </div>

              <div v-if="effective" class="pp-result pp-mt">
                <el-descriptions :column="1" border size="small">
                  <el-descriptions-item label="SKU"><el-text type="info" class="pp-mono">{{ effective.sku ?? '—' }}</el-text></el-descriptions-item>
                  <el-descriptions-item label="Base price">{{ fmtMoney(effective.price) }}</el-descriptions-item>
                  <el-descriptions-item label="Final price">
                    <strong class="pp-text--green">{{ fmtMoney(effective.finalPrice) }}</strong>
                  </el-descriptions-item>
                  <el-descriptions-item label="Discount">
                    <el-text type="danger">-{{ fmtMoney((effective.price ?? 0) - (effective.finalPrice ?? 0)) }}</el-text>
                  </el-descriptions-item>
                  <el-descriptions-item label="Promotion">
                    <el-tag v-if="effective.promotionCode" type="warning" size="small" effect="plain">{{ effective.promotionCode }}</el-tag>
                    <el-text v-else type="info">none</el-text>
                  </el-descriptions-item>
                  <el-descriptions-item v-if="effective.comboInfo" label="Combo">
                    <el-tag type="success" size="small" effect="plain">{{ effective.comboInfo }}</el-tag>
                  </el-descriptions-item>
                </el-descriptions>
              </div>

              <el-divider />

              <strong class="pp-sub-title">Giá theo khách hàng</strong>
              <el-space class="pp-mt" wrap>
                <el-input-number v-model="customerIdForPrice" placeholder="Customer ID" :controls="false" style="width:130px" />
                <el-button plain :loading="effCustLoading" @click="loadEffectiveForCustomer">Tải</el-button>
              </el-space>

              <div v-if="effectiveForCustomer" class="pp-result pp-mt">
                <el-descriptions :column="1" border size="small">
                  <el-descriptions-item label="Variant">{{ effectiveForCustomer.variantName ?? '—' }}</el-descriptions-item>
                  <el-descriptions-item label="Base price">{{ fmtMoney(effectiveForCustomer.price) }}</el-descriptions-item>
                  <el-descriptions-item label="Final price">
                    <strong class="pp-text--green">{{ fmtMoney(effectiveForCustomer.finalPrice) }}</strong>
                  </el-descriptions-item>
                  <el-descriptions-item label="Promotion">
                    <el-tag v-if="effectiveForCustomer.promotionCode" type="warning" size="small" effect="plain">{{ effectiveForCustomer.promotionCode }}</el-tag>
                    <el-text v-else type="info">none</el-text>
                  </el-descriptions-item>
                </el-descriptions>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- ── TAB: LỊCH SỬ GIÁ ── -->
      <el-tab-pane label="Lịch sử giá" name="history">

        <el-card shadow="never" class="pp-card pp-mb">
          <template #header><span class="pp-card-title">Giá theo sản phẩm</span></template>
          <el-space wrap>
            <el-input-number v-model="productId" placeholder="Product ID" :controls="false" style="width:140px" />
            <el-button type="primary" :loading="prodLoading" @click="loadByProduct">Tải</el-button>
          </el-space>

          <el-table v-if="productPrices.length" :data="productPrices" class="pp-mt" border size="small"
            :row-class-name="({ row }) => row.costPrice && row.price < row.costPrice ? 'pp-row-danger' : ''">
            <el-table-column prop="variantId" label="Variant ID" width="100">
              <template #default="{ row }"><span class="pp-mono-id">{{ row.variantId }}</span></template>
            </el-table-column>
            <el-table-column prop="variantName" label="Tên variant" min-width="160" />
            <el-table-column prop="sku" label="SKU" width="120">
              <template #default="{ row }"><span class="pp-mono pp-text--muted">{{ row.sku }}</span></template>
            </el-table-column>
            <el-table-column label="Giá nhập" width="130" align="right">
              <template #default="{ row }">
                <el-space v-if="row.costPrice">
                  <span :class="row.price < row.costPrice ? 'pp-text--red pp-bold' : 'pp-text--muted'">{{ fmtMoney(row.costPrice) }}</span>
                  <el-icon v-if="row.price < row.costPrice" color="#dc2626"><Warning /></el-icon>
                </el-space>
                <span v-else class="pp-text--muted">—</span>
              </template>
            </el-table-column>
            <el-table-column label="Base price" width="130" align="right">
              <template #default="{ row }">{{ fmtMoney(row.price) }}</template>
            </el-table-column>
            <el-table-column label="Final price" width="130" align="right">
              <template #default="{ row }">
                <span :class="row.finalPrice < row.price ? 'pp-text--green pp-bold' : ''">{{ fmtMoney(row.finalPrice) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="Giảm giá" width="120" align="right">
              <template #default="{ row }">
                <span v-if="row.discountAmount > 0" class="pp-text--red">-{{ fmtMoney(row.discountAmount) }}</span>
                <span v-else class="pp-text--muted">—</span>
              </template>
            </el-table-column>
            <el-table-column label="Promotion" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.promotionCode" type="warning" size="small" effect="plain">{{ row.promotionCode }}</el-tag>
                <span v-else class="pp-text--muted">—</span>
              </template>
            </el-table-column>
            <el-table-column label="Combo" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.comboInfo" type="success" size="small" effect="plain">{{ row.comboInfo }}</el-tag>
                <span v-else class="pp-text--muted">—</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card shadow="never" class="pp-card">
          <template #header><span class="pp-card-title">Lịch sử giá variant</span></template>
          <el-space wrap>
            <el-input-number v-model="histVariantId" placeholder="Variant ID" :controls="false" style="width:140px" />
            <el-button type="primary" :loading="histLoading" @click="loadVariantHistory(0)">Tải lịch sử</el-button>
          </el-space>

          <el-table v-if="variantHistory.length" :data="variantHistory" class="pp-mt" border size="small"
            :row-class-name="({ row }) => row.belowCost ? 'pp-row-danger' : ''">
            <el-table-column prop="id" label="ID" width="72">
              <template #default="{ row }"><span class="pp-mono-id">{{ row.id }}</span></template>
            </el-table-column>
            <el-table-column label="Giá bán" width="130" align="right">
              <template #default="{ row }">
                <el-space>
                  <span :class="['pp-bold', row.belowCost ? 'pp-text--red' : '']">{{ fmtMoney(row.price) }}</span>
                  <el-icon v-if="row.belowCost" color="#dc2626"><Warning /></el-icon>
                </el-space>
              </template>
            </el-table-column>
            <el-table-column label="Giá nhập" width="130" align="right">
              <template #default="{ row }"><span class="pp-text--muted">{{ row.costPrice ? fmtMoney(row.costPrice) : '—' }}</span></template>
            </el-table-column>
            <el-table-column label="Lý do" width="130">
              <template #default="{ row }">
                <el-tag type="primary" size="small" effect="plain">{{ row.reason || '—' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Hiệu lực từ" width="160">
              <template #default="{ row }"><span class="pp-mono pp-text--muted pp-small">{{ fmtDate(row.effectiveFrom) }}</span></template>
            </el-table-column>
            <el-table-column label="Hiệu lực đến" width="160">
              <template #default="{ row }"><span class="pp-mono pp-text--muted pp-small">{{ row.effectiveTo ? fmtDate(row.effectiveTo) : '—' }}</span></template>
            </el-table-column>
            <el-table-column prop="createdByUsername" label="Người tạo" width="130" />
            <el-table-column label="Thao tác" width="140" align="center" fixed="right">
              <template #default="{ row }">
                <el-space>
                  <el-button v-if="!row.effectiveTo" text size="small" type="primary" @click="openEdit(row)">
                    <el-icon><Edit /></el-icon>Sửa
                  </el-button>
                  <el-button text size="small" type="danger" @click="removeHistory(row)">
                    <el-icon><Delete /></el-icon>Xóa
                  </el-button>
                </el-space>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else-if="histLoaded" description="Không có bản ghi nào." :image-size="60" />

          <el-pagination
            v-if="histTotalPages > 1"
            class="pp-mt"
            layout="prev, pager, next"
            :total="histTotalPages * 20"
            :page-size="20"
            :current-page="histPage + 1"
            @current-change="(p) => changeHistPage(p - 1)"
          />
        </el-card>
      </el-tab-pane>

      <!-- ── TAB: CẢNH BÁO GIÁ ── -->
      <el-tab-pane label="Cảnh báo giá" name="warning">

        <el-card shadow="never" class="pp-card pp-mb">
          <template #header>
            <el-row justify="space-between" align="middle">
              <span class="pp-card-title">Quét toàn hệ thống — Giá bán &lt; Giá nhập</span>
              <el-button type="warning" :loading="conflictsLoading" @click="loadConflicts">
                <el-icon v-if="!conflictsLoading"><Search /></el-icon>
                Quét toàn hệ thống
              </el-button>
            </el-row>
          </template>

          <template v-if="conflictsLoaded">
            <el-alert v-if="!priceConflicts.length" type="success" show-icon :closable="false">
              Không phát hiện variant nào có giá bán thấp hơn giá nhập.
            </el-alert>
            <template v-else>
              <el-alert type="error" show-icon :closable="false" class="pp-mb">
                {{ priceConflicts.length }} variant có giá bán thấp hơn giá nhập
              </el-alert>
              <el-table :data="priceConflicts" border size="small" :row-class-name="() => 'pp-row-danger'">
                <el-table-column label="Variant ID" width="100">
                  <template #default="{ row }"><span class="pp-mono-id">{{ row.variantId }}</span></template>
                </el-table-column>
                <el-table-column prop="variantName" label="Tên variant" min-width="160" />
                <el-table-column prop="sku" label="SKU" width="120">
                  <template #default="{ row }"><span class="pp-mono pp-text--muted">{{ row.sku }}</span></template>
                </el-table-column>
                <el-table-column label="Giá bán" width="130" align="right">
                  <template #default="{ row }"><span class="pp-text--red pp-bold">{{ fmtMoney(row.price) }}</span></template>
                </el-table-column>
                <el-table-column label="Giá nhập" width="130" align="right">
                  <template #default="{ row }">{{ fmtMoney(row.costPrice) }}</template>
                </el-table-column>
                <el-table-column label="Chênh lệch" width="120" align="right">
                  <template #default="{ row }"><span class="pp-text--red">-{{ fmtMoney(row.diff) }}</span></template>
                </el-table-column>
                <el-table-column prop="currencyCode" label="Currency" width="90">
                  <template #default="{ row }"><span class="pp-mono pp-text--muted">{{ row.currencyCode }}</span></template>
                </el-table-column>
                <el-table-column label="Thao tác" width="110" align="center" fixed="right">
                  <template #default="{ row }">
                    <el-button text size="small" type="warning" @click="openQuickFix(row)">
                      <el-icon><Edit /></el-icon>Sửa giá
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </template>
          </template>
          <el-empty v-else description='Nhấn "Quét toàn hệ thống" để bắt đầu kiểm tra.' :image-size="60" />
        </el-card>

        <el-card shadow="never" class="pp-card">
          <template #header><span class="pp-card-title">Kiểm tra nhanh 1 variant</span></template>
          <el-space wrap>
            <el-input-number v-model="costWarnVariantId" placeholder="Variant ID" :controls="false" style="width:140px" />
            <el-button type="warning" :loading="costWarnLoading" @click="loadCostWarning">
              <el-icon v-if="!costWarnLoading"><Warning /></el-icon>
              Kiểm tra
            </el-button>
          </el-space>

          <div v-if="costWarning" class="pp-mt">
            <el-alert :type="costWarning.belowCost ? 'error' : 'success'" show-icon :closable="false" class="pp-mb">
              {{ costWarning.belowCost ? (costWarning.message || 'Giá bán THẤP hơn giá nhập!') : 'Giá bán hợp lệ' }}
            </el-alert>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="Variant ID"><span class="pp-mono-id">{{ costWarning.variantId }}</span></el-descriptions-item>
              <el-descriptions-item label="Tên">{{ costWarning.variantName ?? '—' }}</el-descriptions-item>
              <el-descriptions-item label="Giá bán">
                <span :class="costWarning.belowCost ? 'pp-text--red pp-bold' : ''">{{ fmtMoney(costWarning.price) }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="Giá nhập">{{ costWarning.costPrice ? fmtMoney(costWarning.costPrice) : '—' }}</el-descriptions-item>
            </el-descriptions>
            <el-button v-if="costWarning.belowCost" type="warning" class="pp-mt" @click="openQuickFix({ variantId: costWarning.variantId, variantName: costWarning.variantName, price: costWarning.price, costPrice: costWarning.costPrice })">
              <el-icon><Edit /></el-icon>Sửa giá ngay
            </el-button>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- ── TAB: TÍNH GIÁ ĐƠN ── -->
      <el-tab-pane label="Tính giá đơn" name="calculate">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card shadow="never" class="pp-card">
              <template #header><span class="pp-card-title">Giỏ hàng</span></template>
              <el-form label-position="top">
                <el-form-item label="Customer ID (tùy chọn)">
                  <el-input-number v-model="calcForm.customerId" placeholder="VD: 1" :controls="false" style="width:100%" />
                </el-form-item>
                <el-form-item label="Sản phẩm">
                  <el-space direction="vertical" :size="8" style="width:100%">
                    <el-space v-for="(item, idx) in calcForm.items" :key="idx" style="width:100%">
                      <span class="pp-item-num">{{ idx + 1 }}</span>
                      <el-input-number v-model="item.variantId" placeholder="Variant ID" :controls="false" style="width:120px" />
                      <span class="pp-item-sep">×</span>
                      <el-input-number v-model="item.quantity" placeholder="SL" :controls="false" style="width:60px" :min="1" />
                      <el-button text @click="calcForm.items.splice(idx, 1)">
                        <el-icon><Close /></el-icon>
                      </el-button>
                    </el-space>
                  </el-space>
                  <el-button text class="pp-mt" @click="calcForm.items.push({ variantId: null, quantity: 1 })">
                    <el-icon><Plus /></el-icon>Thêm sản phẩm
                  </el-button>
                </el-form-item>
              </el-form>
              <el-button type="primary" :loading="calcLoading" @click="calculateOrder" style="width:100%">
                <el-icon v-if="!calcLoading"><Memo /></el-icon>
                Tính giá
              </el-button>
            </el-card>
          </el-col>

          <el-col :span="12">
            <el-card shadow="never" class="pp-card">
              <template #header><span class="pp-card-title">Kết quả tính giá</span></template>
              <div v-if="calcResult">
                <div class="pp-calc-box">
                  <div class="pp-calc-row">
                    <span>Tổng giá gốc</span>
                    <span>{{ fmtMoney(calcResult.subtotal) }}</span>
                  </div>
                  <div class="pp-calc-row pp-calc-row--disc">
                    <span>Giảm giá</span>
                    <span class="pp-text--red">-{{ fmtMoney(calcResult.discountTotal) }}</span>
                  </div>
                  <div v-if="calcResult.freeQty" class="pp-calc-row">
                    <span>Tặng kèm</span>
                    <el-tag type="success" size="small" effect="plain">{{ calcResult.freeQty }} sp miễn phí</el-tag>
                  </div>
                  <div class="pp-calc-sep"></div>
                  <div class="pp-calc-row pp-calc-row--total">
                    <span>Tổng thanh toán</span>
                    <span class="pp-text--green pp-bold">{{ fmtMoney(calcResult.finalTotal) }}</span>
                  </div>
                </div>

                <el-table v-if="calcResult.items?.length" :data="calcResult.items" border size="small" class="pp-mt">
                  <el-table-column label="Variant" width="80">
                    <template #default="{ row }"><span class="pp-mono-id">{{ row.variantId }}</span></template>
                  </el-table-column>
                  <el-table-column prop="variantName" label="Tên" min-width="120" />
                  <el-table-column prop="quantity" label="SL" width="60" align="center" />
                  <el-table-column label="Đơn giá" width="110" align="right">
                    <template #default="{ row }">{{ fmtMoney(row.unitPrice) }}</template>
                  </el-table-column>
                  <el-table-column label="Sau KM" width="110" align="right">
                    <template #default="{ row }">
                      <span :class="row.finalUnitPrice < row.unitPrice ? 'pp-text--green pp-bold' : ''">
                        {{ fmtMoney(row.finalUnitPrice) }}
                      </span>
                    </template>
                  </el-table-column>
                  <el-table-column label="Thành tiền" width="110" align="right">
                    <template #default="{ row }"><strong>{{ fmtMoney(row.lineTotal) }}</strong></template>
                  </el-table-column>
                </el-table>
              </div>
              <el-empty v-else description='Nhập sản phẩm và nhấn "Tính giá".' :image-size="60" />
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- ── TAB: DASHBOARD ── -->
      <el-tab-pane label="Dashboard" name="dashboard">
        <el-row justify="end" class="pp-mb">
          <el-button plain :loading="dashLoading" @click="loadDashboard">
            <el-icon v-if="!dashLoading"><Refresh /></el-icon>
            Làm mới
          </el-button>
        </el-row>

        <template v-if="dashData">
          <el-row :gutter="16" class="pp-mb">
            <el-col :span="6">
              <div class="pp-dash-stat pp-dash-stat--green">
                <div class="pp-dash-stat__num">{{ dashData.activePromotions ?? 0 }}</div>
                <div class="pp-dash-stat__label">KM đang chạy</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div :class="['pp-dash-stat', (dashData.variantsBelowCost ?? 0) > 0 ? 'pp-dash-stat--red' : 'pp-dash-stat--dim']">
                <div class="pp-dash-stat__num">{{ dashData.variantsBelowCost ?? 0 }}</div>
                <div class="pp-dash-stat__label">Giá dưới giá nhập</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div :class="['pp-dash-stat', (dashData.expiringIn3Days ?? 0) > 0 ? 'pp-dash-stat--orange' : 'pp-dash-stat--dim']">
                <div class="pp-dash-stat__num">{{ dashData.expiringIn3Days ?? 0 }}</div>
                <div class="pp-dash-stat__label">Sắp hết hạn (3 ngày)</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div :class="['pp-dash-stat', (dashData.conflictCount ?? 0) > 0 ? 'pp-dash-stat--purple' : 'pp-dash-stat--dim']">
                <div class="pp-dash-stat__num">{{ dashData.conflictCount ?? 0 }}</div>
                <div class="pp-dash-stat__label">Xung đột KM</div>
              </div>
            </el-col>
          </el-row>

          <el-card shadow="never" class="pp-card">
            <template #header>
              <el-row justify="space-between" align="middle">
                <span class="pp-card-title">Chi tiết xung đột khuyến mãi</span>
                <el-tag v-if="dashData.promotionConflicts?.length" type="danger" effect="dark">{{ dashData.promotionConflicts.length }}</el-tag>
              </el-row>
            </template>
            <el-table v-if="dashData.promotionConflicts?.length" :data="dashData.promotionConflicts" border size="small">
              <el-table-column prop="id" label="ID" width="72">
                <template #default="{ row }"><span class="pp-mono-id">{{ row.id }}</span></template>
              </el-table-column>
              <el-table-column label="Code" width="120">
                <template #default="{ row }"><el-tag type="warning" size="small" effect="plain">{{ row.code }}</el-tag></template>
              </el-table-column>
              <el-table-column prop="name" label="Tên chiến dịch" min-width="180" />
              <el-table-column label="Lý do xung đột" min-width="200">
                <template #default="{ row }"><span class="pp-text--red">{{ row.conflictReason }}</span></template>
              </el-table-column>
            </el-table>
            <el-alert v-else type="success" show-icon :closable="false">Không có xung đột khuyến mãi.</el-alert>
          </el-card>
        </template>
        <el-empty v-else-if="!dashLoading" description='Nhấn "Làm mới" để tải dữ liệu.' :image-size="60" class="pp-mt" />
      </el-tab-pane>
    </el-tabs>

    <!-- ── Dialog: Sửa giá ── -->
    <el-dialog v-model="dlg.open" width="460px">
      <template #header>
        <el-space>
          <el-icon :class="dlg.isQuickFix ? 'color-warning' : 'color-primary'">
            <component :is="dlg.isQuickFix ? 'Edit' : 'InfoFilled'" />
          </el-icon>
          <span class="pp-dlg-title">{{ dlg.isQuickFix ? 'Sửa giá nhanh' : 'Sửa bản ghi giá' }}</span>
          <el-tag type="info" size="small" class="pp-mono">#{{ dlg.isQuickFix ? dlg.variantId : dlg.id }}</el-tag>
        </el-space>
      </template>

      <el-alert v-if="dlg.isQuickFix" type="warning" show-icon :closable="false" class="pp-mb">
        Giá bán hiện tại <strong>{{ fmtMoney(dlg.currentPrice) }}</strong> thấp hơn giá nhập <strong>{{ fmtMoney(dlg.currentCostPrice) }}</strong>
      </el-alert>
      <el-alert v-if="dlg.alert" type="error" show-icon :closable="false" class="pp-mb">{{ dlg.alert }}</el-alert>

      <el-form label-position="top">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="Currency">
              <el-input :value="dlg.form.currencyCode || 'VND'" readonly />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Giá bán mới">
              <el-input-number v-model="dlg.form.price" :controls="false" style="width:100%" />
              <div v-if="dlg.form.price" class="pp-field-hint">{{ fmtMoney(dlg.form.price) }}</div>
              <div v-if="dlg.form.price && dlg.form.costPrice && dlg.form.price < dlg.form.costPrice" class="pp-field-hint pp-field-hint--danger">Vẫn thấp hơn giá nhập!</div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="Giá nhập">
              <el-input-number v-model="dlg.form.costPrice" :controls="false" style="width:100%" />
              <div v-if="dlg.form.costPrice" class="pp-field-hint">{{ fmtMoney(dlg.form.costPrice) }}</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Lý do">
              <el-input v-model="dlg.form.reason" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="dlg.open = false">Hủy bỏ</el-button>
        <el-button
          :type="dlg.isQuickFix ? 'warning' : 'primary'"
          :loading="dlg.loading"
          @click="dlg.isQuickFix ? saveQuickFix() : saveEdit()"
        >
          <el-icon v-if="!dlg.loading"><FolderChecked /></el-icon>
          Lưu thay đổi
        </el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { pricesApi } from "../../api/prices.api";
import { settingsApi } from "../../api/settings.api";
import { toast } from "../../ui/toast";
import { confirmModal } from "../../ui/confirm";
import "../../assets/styles/pricing-manager.css";
import {
  Search, Close, FolderChecked, Lightning, Warning,
  Edit, Delete, Plus, Memo, Refresh, InfoFilled,
} from "@element-plus/icons-vue";

const activeTab = ref("set");

const defaultCurrency = ref("VND");

async function loadDefaultCurrency() {
  try {
    const r = await settingsApi.getDefaultCurrency();
    const raw = r?.data ?? null;
    const c = raw?.data?.currencyCode ?? raw?.currencyCode ?? raw?.data?.data?.currencyCode ?? null;
    if (c) { defaultCurrency.value = String(c); setForm.currencyCode = String(c); }
  } catch {}
}

const setLoading = ref(false);
const currentPricePreview = ref(null);
const setForm = reactive({ variantId: null, currencyCode: "VND", price: 0, costPrice: 0, reason: "" });

async function loadCurrentPricePreview() {
  if (!setForm.variantId) return;
  try {
    const r = await pricesApi.getEffective(setForm.variantId);
    const d = r?.data?.data ?? r?.data ?? null;
    const h = await pricesApi.getVariantHistory(setForm.variantId);
    const hist = h?.data?.data?.content ?? h?.data?.content ?? h?.data?.data ?? [];
    const active = hist.find((x) => !x.effectiveTo);
    currentPricePreview.value = d ? { price: d.price, costPrice: active?.costPrice ?? d.costPrice ?? null, belowCost: active?.belowCost ?? false } : null;
  } catch { currentPricePreview.value = null; }
}

async function setPrice() {
  if (!setForm.variantId) return toast("Vui lòng nhập Variant ID.", "warning");
  setLoading.value = true;
  try {
    await pricesApi.setVariantPrice(setForm.variantId, { currencyCode: setForm.currencyCode, price: setForm.price, costPrice: setForm.costPrice, reason: setForm.reason });
    toast("Đã lưu giá.", "success");
    currentPricePreview.value = null;
    await loadDashboard();
  } catch { toast("Lưu giá thất bại.", "error"); }
  finally { setLoading.value = false; }
}

const effLoading = ref(false);
const effective = ref(null);
async function loadEffective() {
  if (!setForm.variantId) return toast("Vui lòng nhập Variant ID.", "warning");
  effLoading.value = true;
  try { const r = await pricesApi.getEffective(setForm.variantId); effective.value = r?.data?.data ?? r?.data ?? null; }
  catch { effective.value = null; toast("Tải thất bại.", "error"); }
  finally { effLoading.value = false; }
}

const effCustLoading = ref(false);
const effectiveForCustomer = ref(null);
const customerIdForPrice = ref(null);
async function loadEffectiveForCustomer() {
  if (!setForm.variantId) return toast("Nhập Variant ID.", "warning");
  if (!customerIdForPrice.value) return toast("Nhập Customer ID.", "warning");
  effCustLoading.value = true;
  try { const r = await pricesApi.getEffectiveForCustomer(setForm.variantId, customerIdForPrice.value); effectiveForCustomer.value = r?.data?.data ?? r?.data ?? null; }
  catch { effectiveForCustomer.value = null; toast("Tải thất bại.", "error"); }
  finally { effCustLoading.value = false; }
}

const prodLoading = ref(false);
const productId = ref(null);
const productPrices = ref([]);
async function loadByProduct() {
  if (!productId.value) return toast("Nhập Product ID.", "warning");
  prodLoading.value = true;
  try { const r = await pricesApi.listByProduct(productId.value); productPrices.value = r?.data?.data ?? r?.data ?? []; }
  catch { productPrices.value = []; toast("Tải thất bại.", "error"); }
  finally { prodLoading.value = false; }
}

const histLoading = ref(false);
const histLoaded = ref(false);
const histVariantId = ref(null);
const variantHistory = ref([]);
const histPage = ref(0);
const histTotalPages = ref(1);
const HIST_PAGE_SIZE = 20;

async function loadVariantHistory(page = 0) {
  if (!histVariantId.value) return toast("Nhập Variant ID.", "warning");
  histLoading.value = true; histLoaded.value = false;
  try {
    const r = await pricesApi.getVariantHistory(histVariantId.value, page, HIST_PAGE_SIZE);
    const paged = r?.data?.data ?? r?.data ?? {};
    variantHistory.value = paged.content ?? paged ?? [];
    histTotalPages.value = paged.totalPages ?? 1;
    histPage.value = page; histLoaded.value = true;
  } catch { variantHistory.value = []; toast("Tải thất bại.", "error"); }
  finally { histLoading.value = false; }
}

function changeHistPage(p) { loadVariantHistory(p); }

const dlg = reactive({ open: false, loading: false, alert: "", id: null, variantId: null, isQuickFix: false, currentPrice: null, currentCostPrice: null, form: { currencyCode: "VND", price: 0, costPrice: 0, reason: "" } });

function openEdit(row) {
  dlg.open = true; dlg.alert = ""; dlg.isQuickFix = false; dlg.id = row.id;
  dlg.form = { currencyCode: defaultCurrency.value || row.currencyCode || "VND", price: Number(row.price || 0), costPrice: Number(row.costPrice || 0), reason: row.reason || "" };
}

function openQuickFix(row) {
  dlg.open = true; dlg.alert = ""; dlg.isQuickFix = true; dlg.variantId = row.variantId;
  dlg.currentPrice = row.price; dlg.currentCostPrice = row.costPrice;
  dlg.form = { currencyCode: defaultCurrency.value || "VND", price: Number(row.price || 0), costPrice: Number(row.costPrice || 0), reason: "PRICE_UPDATE" };
}

async function saveEdit() {
  dlg.loading = true; dlg.alert = "";
  try { await pricesApi.updateHistory(dlg.id, dlg.form); toast("Đã cập nhật.", "success"); dlg.open = false; await loadVariantHistory(histPage.value); await loadDashboard(); }
  catch (e) { dlg.alert = e?.response?.data?.message || e?.message || "Cập nhật thất bại"; }
  finally { dlg.loading = false; }
}

async function saveQuickFix() {
  dlg.loading = true; dlg.alert = "";
  try { await pricesApi.setVariantPrice(dlg.variantId, dlg.form); toast("Đã cập nhật giá.", "success"); dlg.open = false; await loadConflicts(); await loadDashboard(); }
  catch (e) { dlg.alert = e?.response?.data?.message || e?.message || "Cập nhật thất bại"; }
  finally { dlg.loading = false; }
}

async function removeHistory(row) {
  const ok = await confirmModal(`Xóa bản ghi #${row.id}?`, "Xác nhận", "Xóa", true);
  if (!ok) return;
  try { await pricesApi.removeHistory(row.id); toast("Đã xóa.", "success"); await loadVariantHistory(histPage.value); await loadDashboard(); }
  catch { toast("Xóa thất bại.", "error"); }
}

const costWarnLoading = ref(false);
const costWarnVariantId = ref(null);
const costWarning = ref(null);
async function loadCostWarning() {
  if (!costWarnVariantId.value) return toast("Nhập Variant ID.", "warning");
  costWarnLoading.value = true;
  try { const r = await pricesApi.getCostWarning(costWarnVariantId.value); costWarning.value = r?.data?.data ?? r?.data ?? null; }
  catch { costWarning.value = null; toast("Tải thất bại.", "error"); }
  finally { costWarnLoading.value = false; }
}

const conflictsLoading = ref(false);
const conflictsLoaded = ref(false);
const priceConflicts = ref([]);
async function loadConflicts() {
  conflictsLoading.value = true;
  try { const r = await pricesApi.getConflicts(); priceConflicts.value = r?.data?.data ?? r?.data ?? []; conflictsLoaded.value = true; }
  catch { priceConflicts.value = []; toast("Tải xung đột thất bại.", "error"); }
  finally { conflictsLoading.value = false; }
}

const calcLoading = ref(false);
const calcResult = ref(null);
const calcForm = reactive({ customerId: null, items: [{ variantId: null, quantity: 1 }] });
async function calculateOrder() {
  const items = calcForm.items.filter((i) => i.variantId);
  if (!items.length) return toast("Thêm ít nhất 1 sản phẩm.", "warning");
  calcLoading.value = true;
  try { const r = await pricesApi.calculateOrder(calcForm.customerId || null, items); calcResult.value = r?.data?.data ?? r?.data ?? null; }
  catch (e) { toast(e?.response?.data?.message || "Tính giá thất bại.", "error"); }
  finally { calcLoading.value = false; }
}

const dashLoading = ref(false);
const dashData = ref(null);
async function loadDashboard() {
  dashLoading.value = true;
  try { const r = await pricesApi.getDashboard(); dashData.value = r?.data?.data ?? r?.data ?? null; }
  catch { toast("Tải dashboard thất bại.", "error"); }
  finally { dashLoading.value = false; }
}

onMounted(async () => { await Promise.all([loadDashboard(), loadDefaultCurrency()]); });

function fmtMoney(val) {
  if (val == null) return "—";
  return Number(val).toLocaleString("vi-VN") + " ₫";
}
function fmtDate(iso) {
  if (!iso) return "—";
  try { return new Date(iso).toLocaleString("vi-VN"); } catch { return iso; }
}

const variantPickerProductId = ref(null);
const variantPickerLoading = ref(false);
const variantList = ref([]);
const variantPickerOpen = ref(false);

async function loadVariantsForPicker() {
  if (!variantPickerProductId.value) return toast("Nhập Product ID.", "warning");
  variantPickerLoading.value = true; variantList.value = [];
  try { const r = await pricesApi.getVariantsByProduct(variantPickerProductId.value); variantList.value = r?.data ?? []; variantPickerOpen.value = true; }
  catch { toast("Không tải được danh sách variant.", "error"); }
  finally { variantPickerLoading.value = false; }
}

function selectVariant(v) { setForm.variantId = v.id; variantPickerOpen.value = false; loadCurrentPricePreview(); }
</script>

<style scoped>
/* ── Page ── */
.pp-page {
  min-height: 100vh;
  background: #f6f7f9;
  padding: 28px 32px 48px;
}

/* ── Header ── */
.pp-header-card { margin-bottom: 20px; }

.pp-eyebrow {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: var(--el-color-primary);
  margin-bottom: 4px;
}

.pp-title {
  font-size: 22px;
  font-weight: 800;
  color: #0f1117;
  margin: 0 0 4px;
}

.pp-subtitle {
  font-size: 13px;
  color: #6b7280;
  margin: 0;
}

/* ── Tabs ── */
.pp-tabs { border-radius: 0; }

/* ── Cards ── */
.pp-card { border: 1px solid #e4e7ec !important; }
.pp-card-title { font-size: 14px; font-weight: 700; color: #1e3a5f; }
.pp-mb { margin-bottom: 20px; }
.pp-mt { margin-top: 12px; }

/* ── Variant picker ── */
.pp-variant-list {
  margin-top: 8px;
  width: 100%;
  border: 1px solid #e4e7ec;
  border-radius: 6px;
  overflow: hidden;
}

.pp-variant-option {
  padding: 8px 12px;
  cursor: pointer;
  border-bottom: 1px solid #f0f2f5;
}
.pp-variant-option:last-child { border-bottom: none; }
.pp-variant-option:hover { background: #eff6ff; }
.pp-variant-option--selected { background: #dbeafe; }
.pp-variant-option--inactive { opacity: 0.6; }
.pp-variant-name { font-weight: 600; color: #1e3a5f; }

/* ── Price preview ── */
.pp-price-preview { margin: 12px 0; }

/* ── Field hints ── */
.pp-field-hint { font-size: 12px; color: #6b7280; margin-top: 3px; }
.pp-field-hint--danger { color: #dc2626; font-weight: 600; }

/* ── Result / sub-title ── */
.pp-result { margin-top: 12px; }
.pp-sub-title { font-size: 13px; font-weight: 700; color: #1e3a5f; }

/* ── Calc box ── */
.pp-calc-box {
  background: #f8faff;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  padding: 14px 16px;
  margin-bottom: 12px;
}
.pp-calc-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 5px 0;
  font-size: 14px;
  color: #374151;
}
.pp-calc-row--disc { color: #6b7280; }
.pp-calc-row--total { font-weight: 700; font-size: 15px; padding-top: 8px; }
.pp-calc-sep { height: 1px; background: #dbeafe; margin: 6px 0; }

/* ── Dashboard stat cards ── */
.pp-dash-stat {
  border-radius: 12px;
  padding: 16px 18px;
  border: 1px solid transparent;
  text-align: center;
}
.pp-dash-stat--green  { background: #f0fdf4; border-color: #bbf7d0; }
.pp-dash-stat--red    { background: #fff1f2; border-color: #fecdd3; }
.pp-dash-stat--orange { background: #fffbeb; border-color: #fde68a; }
.pp-dash-stat--purple { background: #faf5ff; border-color: #e9d5ff; }
.pp-dash-stat--dim    { background: #f9fafb; border-color: #e4e7ec; }

.pp-dash-stat__num {
  font-size: 28px;
  font-weight: 800;
  color: #0f1117;
  line-height: 1;
}
.pp-dash-stat__label {
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
}

/* ── Dialog ── */
.pp-dlg-title { font-size: 16px; font-weight: 700; }

/* ── Cart items ── */
.pp-item-num {
  width: 22px; height: 22px;
  background: #e4e7ec;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 700;
  color: #6b7280;
  flex-shrink: 0;
}
.pp-item-sep { font-size: 14px; font-weight: 700; color: #9ca3af; }

/* ── Table danger rows ── */
:deep(.pp-row-danger td) { background: #fff1f2 !important; }

/* ── Typography utilities ── */
.pp-mono    { font-family: "JetBrains Mono", "Fira Code", monospace; }
.pp-mono-id { font-family: "JetBrains Mono", "Fira Code", monospace; font-size: 12px; font-weight: 700; color: #6b7280; }
.pp-bold    { font-weight: 700; }
.pp-small   { font-size: 12px; }
.pp-ml      { margin-left: 6px; }

.pp-text--red   { color: #dc2626; }
.pp-text--green { color: #16a34a; }
.pp-text--muted { color: #9ca3af; }

/* ── Color utilities ── */
.color-primary { color: var(--el-color-primary); }
.color-warning { color: var(--el-color-warning); }
</style>