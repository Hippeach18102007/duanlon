<template>
  <div class="sales-return">
    <!-- ══════════ HEADER ══════════ -->
    <el-row align="middle" justify="space-between" class="header">
      <el-space :size="12">
        <el-tooltip content="Quay lại tìm đơn khác" placement="bottom">
          <el-button v-if="order" :icon="ArrowLeft" @click="reset" link>Tìm đơn khác</el-button>
        </el-tooltip>
        <el-space direction="vertical" :size="0">
          <el-text tag="b" size="large">TRẢ HÀNG TẠI QUẦY</el-text>
          <el-text type="info" size="small">
            <span v-if="order">
              Đang xem: <strong>{{ order.orderNumber || `#${order.id}` }}</strong>
              <el-tag size="small" :type="orderStatusType(order.status)" plain class="ml-8">{{ order.status }}</el-tag>
            </span>
            <span v-else>Xử lý đổi trả và hoàn tiền khách hàng</span>
          </el-text>
        </el-space>
      </el-space>
      <el-tooltip v-if="order" content="Tải lại thông tin đơn" placement="left">
        <el-button :icon="Refresh" circle @click="handleSearch" plain />
      </el-tooltip>
    </el-row>

    <!-- ══════════ STEP PROGRESS ══════════ -->
    <el-steps v-if="order" :active="currentStep" finish-status="success" class="steps-bar">
      <el-step title="Tìm đơn" description="Nhập mã đơn" />
      <el-step title="Chọn hàng trả" description="Chọn sản phẩm + lý do" />
      <el-step title="Thẩm định" description="Admin xét duyệt" />
      <el-step title="Hoàn tất" description="Hoàn tiền khách" />
    </el-steps>

    <!-- ══════════ SEARCH SECTION ══════════ -->
    <el-card v-if="!order" shadow="never">
      <el-space direction="vertical" fill :size="20" class="search-wrap">
        <el-space direction="vertical" :size="4" align="center">
          <el-text tag="h2" style="margin: 0">Bắt đầu xử lý trả hàng</el-text>
          <el-text type="info">Nhập mã đơn hàng (ID số hoặc mã ORD-xxx) để tra cứu</el-text>
        </el-space>
        <el-input
          v-model="searchId"
          placeholder="Nhập Order ID hoặc Order Number..."
          @keyup.enter="handleSearch"
          size="large"
          style="max-width: 520px"
          clearable
        >
          <template #prefix><el-icon><Search /></el-icon></template>
          <template #append>
            <el-button @click="handleSearch" :loading="loading" plain>KIỂM TRA</el-button>
          </template>
        </el-input>
      </el-space>
    </el-card>

    <!-- ══════════ WORKFLOW ══════════ -->
    <div v-if="order">
      <el-row :gutter="20">
        <!-- ─── LEFT COL: Order Info & Items ─── -->
        <el-col :span="16">

          <!-- Order Overview -->
          <el-card shadow="never" class="mb-20">
            <template #header>
              <el-text tag="b">Thông tin đơn hàng</el-text>
            </template>
            <el-descriptions :column="3" size="small" border>
              <el-descriptions-item label="Mã đơn">
                <el-text tag="b">{{ order.orderNumber || `#${order.id}` }}</el-text>
              </el-descriptions-item>
              <el-descriptions-item label="Tổng tiền">
                <el-text type="primary" tag="b">{{ formatMoney(order.totalAmount) }}</el-text>
              </el-descriptions-item>
              <el-descriptions-item label="Thanh toán">
                <el-tag :type="order.paymentStatus === 'PAID' ? 'success' : 'warning'" plain size="small">
                  {{ order.paymentStatus || '—' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="Ngày tạo">{{ formatDate(order.createdAt) }}</el-descriptions-item>
              <el-descriptions-item label="Kênh">{{ order.channel || '—' }}</el-descriptions-item>
              <el-descriptions-item label="Phương thức TT">{{ order.paymentMethod || '—' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- Status Alert -->
          <el-alert
            v-if="existingReturn?.status === 'COMPLETED'"
            title="ĐÃ HOÀN TẤT TRẢ HÀNG"
            type="success"
            description="Hệ thống đã ghi nhận hoàn tiền. Không cần xử lý thêm."
            show-icon :closable="false" class="mb-20"
          />
          <el-alert
            v-else-if="existingReturn?.status === 'PENDING'"
            title="CÓ YÊU CẦU ĐANG CHỜ ADMIN DUYỆT"
            type="warning"
            description="Yêu cầu đã được tạo. Admin sẽ xem xét ảnh minh chứng và quyết định."
            show-icon :closable="false" class="mb-20"
          />
          <el-alert
            v-else-if="existingReturn?.status === 'REJECTED'"
            title="YÊU CẦU TRƯỚC ĐÃ BỊ TỪ CHỐI"
            type="error"
            :description="'Lý do: ' + (existingReturn.note || '—')"
            show-icon :closable="false" class="mb-20"
          />
          <el-alert
            v-else-if="!existingReturn && order?.status !== 'DELIVERED'"
            title="CHƯA ĐỦ ĐIỀU KIỆN TRẢ HÀNG"
            type="warning"
            :description="`Trạng thái đơn hiện tại: ${order?.status}. Chỉ có thể trả hàng sau khi đơn đã DELIVERED.`"
            show-icon :closable="false" class="mb-20"
          />
          <el-alert
            v-else-if="!existingReturn && order?.status === 'DELIVERED'"
            title="SẴN SÀNG TẠO YÊU CẦU"
            type="info"
            description="Đơn hàng đủ điều kiện. Chọn sản phẩm cần trả bên dưới."
            show-icon :closable="false" class="mb-20"
          />

          <!-- Items Table -->
          <el-card shadow="never">
            <template #header>
              <el-row justify="space-between" align="middle">
                <el-text tag="b">
                  Danh sách sản phẩm
                  <el-tag size="small" plain>{{ (order.items || []).length }} món</el-tag>
                </el-text>
                <el-text v-if="selectedItems.length > 0" type="warning" size="small">
                  Đang chọn {{ selectedItems.length }} / {{ (order.items || []).length }} sản phẩm
                </el-text>
              </el-row>
            </template>

            <el-empty v-if="!(order.items || []).length" description="Không có sản phẩm trong đơn hàng" />

            <el-table
              v-else
              :data="displayItems"
              @selection-change="handleSelectionChange"
              row-key="_uiKey"
            >
              <el-table-column type="selection" width="50" :selectable="canSelectItem" />

              <el-table-column label="Serial" min-width="140" width="140">
                <template #default="{ row }">
                  <el-text v-if="row._serialNumber" size="small" type="success" style="font-family: monospace;">
                    {{ row._serialNumber }}
                  </el-text>
                  <el-text v-else size="small" type="info">—</el-text>
                </template>
              </el-table-column>

              <el-table-column label="Sản phẩm" min-width="200">
                <template #default="{ row }">
                  <el-space>
                    <el-image
                      v-if="row.imageUrl"
                      :src="row.imageUrl"
                      class="product-thumb"
                      fit="cover"
                      @error="handleImageError(row)"
                    />
                    <el-space direction="vertical" :size="0" align="start">
                      <el-text tag="b">{{ row.productName }}</el-text>
                      <el-text size="small" type="info">{{ row.variantName }}</el-text>
                    </el-space>
                  </el-space>
                </template>
              </el-table-column>

              <el-table-column label="SL mua" width="80" align="center" prop="quantity" />

              <el-table-column label="Đơn giá" width="120" align="right">
                <template #default="{ row }">{{ formatMoney(row.price) }}</template>
              </el-table-column>

              <!-- Số lượng trả — chỉ hiện khi đang tạo mới -->
              <el-table-column v-if="canCreateReturn" label="SL trả" width="120" align="center">
                <template #default="{ row }">
                  <el-input-number
                    v-if="isSelected(row)"
                    v-model="returnQuantities[row._uiKey]"
                    :min="1"
                    :max="row.quantity"
                    size="small"
                    controls-position="right"
                    style="width: 90px"
                  />
                  <el-text v-else type="info" size="small">—</el-text>
                </template>
              </el-table-column>

              <el-table-column label="Thành tiền" width="130" align="right">
                <template #default="{ row }">{{ formatMoney(order.totalAmount) }}</template>
              </el-table-column>
            </el-table>

            <!-- ── Form tạo yêu cầu ── -->
            <div v-if="canCreateReturn" class="footer-actions">
              <el-divider />

              <!-- Ước tính hoàn tiền (tính theo tỉ lệ thực tế đã discount) -->
              <el-row justify="space-between" align="middle" class="mb-10" v-if="selectedItems.length > 0">
                <el-text size="small" type="info">
                  {{ selectedItems.length }} sản phẩm được chọn
                </el-text>
                <el-space>
                  <el-text size="small" type="info">Ước tính hoàn:</el-text>
                  <el-text type="danger" tag="b" size="large">{{ formatMoney(estimatedRefund) }}</el-text>
                </el-space>
              </el-row>

              <el-form label-position="top">
                <!-- Lý do trả hàng -->
                <el-form-item label="Lý do trả hàng" :error="reasonError" required>
                  <el-input
                    v-model="returnReason"
                    type="textarea"
                    :rows="2"
                    placeholder="Nhập lý do trả hàng... (bắt buộc)"
                    @input="reasonError = ''"
                  />
                </el-form-item>

                <!-- ✅ Ảnh minh chứng -->
                <el-form-item label="Ảnh minh chứng (tuỳ chọn)">
                  <el-upload
                    action="#"
                    :auto-upload="false"
                    :limit="1"
                    accept="image/*"
                    :on-change="(file) => { returnImage = file.raw }"
                    :on-remove="() => { returnImage = null }"
                    list-type="picture"
                  >
                    <el-button size="small" plain>Chọn ảnh</el-button>
                    <template #tip>
                      <el-text size="small" type="info">
                        Ảnh sản phẩm bị lỗi — giúp Admin xét duyệt nhanh hơn
                      </el-text>
                    </template>
                  </el-upload>
                </el-form-item>
              </el-form>

              <el-button
                type="warning"
                size="large"
                style="width: 100%; margin-top: 10px"
                :disabled="selectedItems.length === 0"
                :loading="submitting"
                @click="initReturn"
                plain
              >
                KHỞI TẠO YÊU CẦU TRẢ HÀNG
                {{ selectedItems.length > 0 ? `(${selectedItems.length} SP — ${formatMoney(estimatedRefund)})` : '' }}
              </el-button>
            </div>
          </el-card>
        </el-col>

        <!-- ─── RIGHT COL: Customer + Status Panels ─── -->
        <el-col :span="8">

          <!-- Khách hàng -->
          <el-card shadow="never" class="mb-20">
            <template #header><el-text tag="b">Khách hàng</el-text></template>
            <el-space>
              <el-avatar :size="40">{{ initials(order.customerName) }}</el-avatar>
              <el-space direction="vertical" :size="0" align="start">
                <el-text tag="b">{{ order.customerName || '—' }}</el-text>
                <el-text type="info" size="small">{{ order.customerPhone || '—' }}</el-text>
              </el-space>
            </el-space>
          </el-card>

          <!-- REJECTED -->
          <el-card v-if="existingReturn?.status === 'REJECTED'" shadow="never" class="mb-20">
            <template #header>
              <el-row justify="space-between" align="middle">
                <el-text tag="b">KẾT QUẢ XỬ LÝ</el-text>
                <el-tag type="danger" plain>TỪ CHỐI</el-tag>
              </el-row>
            </template>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="Lý do từ chối">{{ existingReturn.note || '—' }}</el-descriptions-item>
              <el-descriptions-item label="Người xử lý">{{ existingReturn.processedByName || '—' }}</el-descriptions-item>
              <el-descriptions-item label="Thời gian">{{ formatDate(existingReturn.createdAt) }}</el-descriptions-item>
            </el-descriptions>
            <el-button
              type="warning"
              style="width: 100%; margin-top: 16px"
              @click="existingReturn = null"
              plain
            >
              Tạo yêu cầu mới
            </el-button>
          </el-card>

          <!-- ✅ PENDING — hiển thị thông tin, không có nút approve/reject (Admin xử lý ở trang riêng) -->
          <el-card v-if="existingReturn?.status === 'PENDING'" shadow="never" class="mb-20">
            <template #header>
              <el-row justify="space-between" align="middle">
                <el-text tag="b">THÔNG TIN YÊU CẦU</el-text>
                <el-tag type="warning" plain>CHỜ ADMIN DUYỆT</el-tag>
              </el-row>
            </template>

            <el-skeleton v-if="!returnDetail" :rows="5" animated />
            <div v-else>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="Sản phẩm">{{ returnDetail.productName }}</el-descriptions-item>
                <el-descriptions-item label="Số lượng">{{ returnDetail.quantity }}</el-descriptions-item>
                <el-descriptions-item label="Lý do">"{{ returnDetail.reason }}"</el-descriptions-item>
                <el-descriptions-item label="Ngày yêu cầu">
                  {{ formatDate(returnDetail.createdAt) }}
                </el-descriptions-item>
              </el-descriptions>

              <!-- Ảnh minh chứng đã đính kèm -->
              <div v-if="returnDetail.imageUrl" style="margin-top: 12px">
                <el-text size="small" type="info">Ảnh minh chứng đã gửi:</el-text>
                <el-image
                  :src="returnDetail.imageUrl"
                  :preview-src-list="[returnDetail.imageUrl]"
                  fit="cover"
                  style="width: 100%; max-height: 180px; border-radius: 4px; margin-top: 6px"
                  preview-teleported
                />
              </div>

              <el-alert
                style="margin-top: 16px"
                title="Đang chờ Admin xét duyệt"
                description="Admin sẽ xem xét ảnh minh chứng và quyết định mức hoàn tiền tại trang Quản lý hoàn hàng."
                type="info"
                show-icon
                :closable="false"
              />
            </div>
          </el-card>

          <!-- COMPLETED -->
          <el-card v-if="existingReturn?.status === 'COMPLETED'" shadow="never">
            <el-result icon="success" title="Hoàn tất trả hàng">
              <template #sub-title>
                <el-descriptions :column="1" border size="small" class="mt-10">
                  <el-descriptions-item v-if="returnDetail?.refundAmount" label="Đã hoàn tiền">
                    <el-text type="success" tag="b">{{ formatMoney(returnDetail.refundAmount) }}</el-text>
                  </el-descriptions-item>
                  <el-descriptions-item v-if="returnDetail?.refundMethod" label="Loại hoàn">
                    {{ returnDetail.refundMethod }}
                  </el-descriptions-item>
                  <el-descriptions-item v-if="returnDetail?.processedByName" label="Người duyệt">
                    {{ returnDetail.processedByName }}
                  </el-descriptions-item>
                  <el-descriptions-item v-if="returnDetail?.refundedAt" label="Ngày hoàn tiền">
                    {{ formatDate(returnDetail.refundedAt) }}
                  </el-descriptions-item>
                </el-descriptions>
              </template>
              <template #extra>
                <el-button @click="reset" plain>Xử lý đơn khác</el-button>
              </template>
            </el-result>
          </el-card>

        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue';
import { Search, Refresh, ArrowLeft } from '@element-plus/icons-vue';
import { ordersApi } from '../../api/orders.api';
import { returnsApi } from '../../api/returns.api';
import { toast } from '../../ui/toast';
import { confirmModal } from '../../ui/confirm';

// ── State ─────────────────────────────────────────────────────────────
const searchId        = ref('');
const loading         = ref(false);
const submitting      = ref(false);
const order           = ref(null);
const existingReturn  = ref(null);
const returnDetail    = ref(null);
const selectedItems   = ref([]);
const returnReason    = ref('');
const reasonError     = ref('');
const returnImage     = ref(null); // ✅ ảnh minh chứng
const returnQuantities = reactive({});

// ── Computed ──────────────────────────────────────────────────────────

const currentStep = computed(() => {
  if (!order.value) return 0;
  const retStatus = existingReturn.value?.status;
  const ordStatus = order.value?.status;
  if (retStatus === 'COMPLETED') return 3;
  if (retStatus === 'PENDING')   return 2;
  if (ordStatus === 'RETURNED' || ordStatus === 'PARTIALLY_RETURNED') return 4;
  if (ordStatus === 'RETURN_REQUESTED') return 3;
  return 1;
});

const canCreateReturn = computed(() => {
  const noActiveReturn = !existingReturn.value || existingReturn.value.status === 'REJECTED';
  const isDelivered    = order.value?.status === 'DELIVERED';
  return order.value && noActiveReturn && isDelivered;
});

// ✅ Tính ước tính hoàn tiền theo tỉ lệ thực tế (đã áp discount)
const estimatedRefund = computed(() => {
  if (!selectedItems.value.length) return 0;
  const subtotal    = order.value?.subtotal    || 0;
  const totalAmount = order.value?.totalAmount || 0;
  const ratio = subtotal > 0 ? totalAmount / subtotal : 1;
  return selectedItems.value.reduce((sum, item) => {
    const k   = getRowKey(item);
    const qty = (k != null ? returnQuantities[k] : null) ?? item.quantity;
    return sum + (item.price ?? 0) * qty * ratio;
  }, 0);
});

// ── Helpers ───────────────────────────────────────────────────────────
function isSelected(row) {
  return selectedItems.value.some(i => i.id === row.id);
}
const formatMoney  = (v) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v || 0);
const formatDate   = (d) => d ? new Date(d).toLocaleString('vi-VN') : '—';
const orderStatusType = (s) => ({ PENDING: 'warning', PAID: 'primary', PROCESSING: '', SHIPPING: 'primary', DELIVERED: 'success', CANCELLED: 'danger' }[s] || 'info');
const initials     = (name = '') => (name || '').split(' ').map(n => n[0]).filter(Boolean).join('').toUpperCase().slice(0, 2) || '?';
function handleImageError(row) { row.imageUrl = null; }

function getItemKey(item) {
  return item.id ?? item.orderItemId ?? item.itemId ?? null;
}
function getRowKey(item) {
  return item._uiKey || getItemKey(item) || item.serialCode || item.sku || null;
}

function expandOrderItems(items) {
  return (items || []).flatMap((item) => {
    const baseKey = getItemKey(item);
    const serials = (item.serialNumbers || []).filter(Boolean);
    if (serials.length > 1) {
      return serials.map((sn) => ({
        ...item,
        quantity:       1,
        _uiKey:         `${baseKey}:${sn}`,
        _serialNumber:  sn,
        _baseItemId:    baseKey,
      }));
    }
    const singleSerial = serials[0] || null;
    return [{
      ...item,
      _uiKey:        singleSerial ? `${baseKey}:${singleSerial}` : `${baseKey}`,
      _serialNumber: singleSerial,
      _baseItemId:   baseKey,
    }];
  });
}

const displayItems = computed(() => expandOrderItems(order.value?.items || []));

// ── Search ────────────────────────────────────────────────────────────
async function handleSearch() {
  if (!searchId.value.trim()) return;
  loading.value        = true;
  order.value          = null;
  existingReturn.value = null;
  returnDetail.value   = null;
  selectedItems.value  = [];
  returnReason.value   = '';
  reasonError.value    = '';
  Object.keys(returnQuantities).forEach(k => delete returnQuantities[k]);

  try {
    const input = searchId.value.trim();
    const isNumeric     = /^\d+$/.test(input);
    const isOrderNumber = input.toUpperCase().startsWith('ORD-');
    const isSerial      = !isNumeric && !isOrderNumber;
    let orderData       = null;

    if (isNumeric) {
      const res = await ordersApi.getById(input);
      orderData = res?.data?.data ?? res?.data ?? null;

    } else if (isOrderNumber) {
      try {
        const res = await ordersApi.getByOrderNumber(input);
        const candidate = res?.data?.data ?? res?.data ?? null;
        if (candidate) {
          candidate.id = candidate.id ?? candidate.orderId;
          orderData = candidate;
        }
      } catch {
        const res = await ordersApi.filter(null, null, null, null, null);
        const raw  = res?.data?.data ?? res?.data ?? [];
        const list = raw?.content ?? (Array.isArray(raw) ? raw : []);
        const found = list.find(o => (o.orderNumber || '').toLowerCase() === input.toLowerCase()) ?? null;
        if (found?.id) {
          const detailRes = await ordersApi.getById(found.id);
          orderData = detailRes?.data?.data ?? detailRes?.data ?? found;
        }
      }

    } else if (isSerial) {
      try {
        const res = await ordersApi.findBySerial(input);
        const candidate = res?.data?.data ?? res?.data ?? null;
        if (candidate) {
          candidate.id = candidate.id ?? candidate.orderId;
          orderData = candidate;
          toast(`Tìm thấy đơn hàng từ serial: ${input}`, 'info');
        }
      } catch (e) {
        if (e?.response?.status === 404) {
          throw new Error(`Serial "${input}" chưa được gán với đơn hàng nào`);
        }
        throw new Error(`Không tìm thấy serial: "${input}"`);
      }
    }

    if (!orderData) throw new Error(`Không tìm thấy đơn hàng: "${input}"`);

    const orderId = orderData.id ?? orderData.orderId;
    if (!orderId) throw new Error('Đơn hàng không có ID hợp lệ');
    orderData.id = orderId;
    order.value  = orderData;

    expandOrderItems(orderData.items || []).forEach(item => {
      const k = getRowKey(item);
      if (k != null) returnQuantities[k] = item.quantity;
    });

    // Kiểm tra return hiện có
    try {
      const retRes  = await returnsApi.getByOrder(orderId);
      const retData = retRes?.data?.data ?? retRes?.data ?? null;
      const singleReturn = Array.isArray(retData)
        ? retData.sort((a, b) => b.id - a.id)[0]
        : retData;
      existingReturn.value = singleReturn || null;
      if (singleReturn?.status === 'PENDING')   fetchReturnDetail(singleReturn.id);
      if (singleReturn?.status === 'COMPLETED') fetchReturnDetail(singleReturn.id);
    } catch (retErr) {
      if (retErr?.response?.status !== 404) {
        toast('Không thể kiểm tra trạng thái trả hàng', 'warning');
      }
      existingReturn.value = null;
    }

  } catch (err) {
    toast(err.response?.data?.message || err.message || 'Lỗi hệ thống', 'error');
    order.value = null;
  } finally {
    loading.value = false;
  }
}

// ── Create Return ──────────────────────────────────────────────────────
async function initReturn() {
  if (selectedItems.value.length === 0) return;
  if (!returnReason.value.trim()) {
    reasonError.value = 'Vui lòng nhập lý do trả hàng';
    return;
  }
  if (order.value?.status !== 'DELIVERED') {
    toast('Chỉ có thể tạo yêu cầu trả hàng khi đơn đã giao (DELIVERED)', 'warning');
    return;
  }

  const invalidItems = selectedItems.value.filter(i => !getItemKey(i));
  if (invalidItems.length > 0) {
    toast(`Không xác định được mã sản phẩm: ${invalidItems.map(i => i.productName).join(', ')}`, 'error');
    return;
  }

  // ✅ Tỉ lệ discount thực tế của đơn
  const subtotal    = order.value?.subtotal    || 0;
  const totalAmount = order.value?.totalAmount || 0;
  const ratio       = subtotal > 0 ? totalAmount / subtotal : 1;

  const summary = selectedItems.value
    .map(i => {
      const rowKey = getRowKey(i);
      const qty    = (rowKey != null ? returnQuantities[rowKey] : null) ?? i.quantity;
      const serialTag = i._serialNumber ? ` (${i._serialNumber})` : '';
      return `• ${i.productName}${serialTag} × ${qty} — ${formatMoney((i.price ?? 0) * qty * ratio)}`;
    })
    .join('\n');

  const ok = await confirmModal(
    `Xác nhận tạo yêu cầu trả hàng?\n\n${summary}\n\nTổng hoàn dự kiến: ${formatMoney(estimatedRefund.value)}`,
    'Khởi tạo trả hàng'
  );
  if (!ok) return;

  submitting.value = true;
  let successCount = 0;
  let lastError    = null;

  try {
    for (const item of selectedItems.value) {
      const rowKey       = getRowKey(item);
      const qty          = (rowKey != null ? returnQuantities[rowKey] : null) ?? item.quantity;
      const orderItemId  = item._baseItemId ?? getItemKey(item);

      // ✅ Tính refundAmount theo tỉ lệ thực tế (đã discount)
      const refundAmount = Math.round((item.price ?? 0) * qty * ratio);

      const payload = {
        orderId:      order.value.id,
        orderItemId:  orderItemId,
        quantity:     qty,
        reason:       returnReason.value.trim(),
        refundAmount: refundAmount,
      };

      // ✅ Gửi FormData để kèm ảnh
      const formData = new FormData();
      formData.append('data', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
      if (returnImage.value) {
        formData.append('image', returnImage.value);
      }

      try {
        await returnsApi.create(formData);
        successCount++;
      } catch (itemErr) {
        lastError = itemErr;
        console.error('Lỗi tạo yêu cầu cho item', item.productName, itemErr?.response?.data);
      }
    }

    if (successCount > 0) {
      toast(
        successCount === selectedItems.value.length
          ? 'Đã khởi tạo yêu cầu thành công'
          : `Đã tạo ${successCount}/${selectedItems.value.length} yêu cầu. Có lỗi xảy ra.`,
        successCount === selectedItems.value.length ? 'success' : 'warning'
      );
      if (searchId.value) handleSearch();
    } else {
      toast(lastError?.response?.data?.message || 'Tất cả yêu cầu đều thất bại', 'error');
    }
  } finally {
    submitting.value = false;
  }
}

// ── Fetch Detail ───────────────────────────────────────────────────────
async function fetchReturnDetail(id) {
  try {
    const res = await returnsApi.getDetail(id);
    returnDetail.value = res?.data?.data || res?.data;
  } catch {
    toast('Không thể tải chi tiết yêu cầu', 'error');
  }
}

// ── Misc ───────────────────────────────────────────────────────────────
function handleSelectionChange(val) {
  selectedItems.value = val;
  val.forEach(item => {
    const key = getRowKey(item);
    if (key !== null && returnQuantities[key] === undefined) {
      returnQuantities[key] = item.quantity;
    }
  });
}

function canSelectItem() {
  return !existingReturn.value || existingReturn.value.status === 'REJECTED';
}

function reset() {
  order.value          = null;
  existingReturn.value = null;
  returnDetail.value   = null;
  selectedItems.value  = [];
  returnReason.value   = '';
  reasonError.value    = '';
  returnImage.value    = null;
  searchId.value       = '';
  Object.keys(returnQuantities).forEach(k => delete returnQuantities[k]);
}
</script>

<style scoped>
.sales-return  { padding: 0; min-height: 100vh; background: #f5f7fa; }
.header        { padding: 10px 0; margin-bottom: 16px; }
.steps-bar     { margin-bottom: 24px; background: #fff; padding: 20px 24px; border-radius: 4px; border: 1px solid #dcdfe6; }
.search-wrap   { text-align: center; padding: 60px 40px; }
.mb-20         { margin-bottom: 20px; }
.mb-10         { margin-bottom: 10px; }
.mt-10         { margin-top: 10px; }
.ml-8          { margin-left: 8px; }
.product-thumb { width: 40px; height: 40px; flex-shrink: 0; }
.footer-actions { margin-top: 24px; }
</style>