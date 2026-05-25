<!-- src/pages/customer/VnPayResult.vue -->
<template>
  <div class="auth-page">
    <div style="width:100%; max-width:640px;">

      <!-- ── LOADING ── -->
      <el-card v-if="fetchingOrder" shadow="never" class="profile-card" style="width: 100%; max-width: 640px;">
        <div style="text-align:center; padding:40px 0;">
          <el-icon class="is-loading" :size="48" color="var(--el-color-primary)"><Loading /></el-icon>
          <el-text tag="div" style="margin-top:16px; font-size:15px; font-weight:600;">Đang xử lý kết quả thanh toán</el-text>
          <el-text tag="div" size="small" type="info" style="margin-top:4px;">
            {{ statusMessage || 'Vui lòng chờ trong giây lát...' }}
          </el-text>
        </div>
      </el-card>

      <!-- ── RESULT ── -->
      <el-card v-else shadow="never" class="profile-card" style="width: 100%; max-width: 640px;">

        <el-result
          :icon="isSuccess ? 'success' : 'error'"
          :title="isSuccess ? 'Thanh toán thành công! 🎉' : 'Thanh toán thất bại'"
          :sub-title="isSuccess
            ? 'Cảm ơn bạn đã mua hàng. Đơn hàng đang được xử lý.'
            : responseMessage || 'Giao dịch không thành công.'"
        >
          <template v-if="isSuccess" #extra>
            <el-card shadow="never" class="profile-card" style="background: var(--el-color-success-light-9); border-color: var(--el-color-success-light-5); text-align: center; padding: 8px 24px;">
              <div style="font-size: 12px; color: var(--el-text-color-secondary); margin-bottom: 6px; letter-spacing: 0.05em;">
                SỐ TIỀN ĐÃ THANH TOÁN
              </div>
              <div style="font-size: 32px; font-weight: 800; color: var(--el-color-success-dark-2); line-height: 1.2;">
                {{ formatMoney(displayAmount) }}
              </div>
            </el-card>
          </template>

        </el-result>

        <el-divider content-position="left">
          <el-text size="small" type="info">Chi tiết giao dịch</el-text>
        </el-divider>

        <!-- 2 cột nằm ngang -->
        <el-descriptions :column="2" border size="small" style="margin-bottom:20px;">
          <el-descriptions-item label="Mã đơn hàng">
            <el-text style="font-family:monospace; font-weight:600;">{{ q.vnp_TxnRef || '—' }}</el-text>
          </el-descriptions-item>
          <el-descriptions-item label="Mã GD VNPay">
            <el-text type="primary" style="font-family:monospace;">{{ q.vnp_TransactionNo || '—' }}</el-text>
          </el-descriptions-item>
          <el-descriptions-item label="Ngân hàng">
            <el-tag size="small" effect="plain">{{ q.vnp_BankCode || '—' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Hình thức">
            <el-tag size="small" :type="q.vnp_CardType === 'QRCODE' ? 'success' : 'primary'" effect="light">
              {{ q.vnp_CardType === 'ATM' ? '💳 ATM/Nội địa' : q.vnp_CardType === 'QRCODE' ? '📱 QR Code' : (q.vnp_CardType || '—') }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Thời gian">{{ formatVnpayDate(q.vnp_PayDate) }}</el-descriptions-item>
          <el-descriptions-item label="Trạng thái">
            <el-tag :type="isSuccess ? 'success' : 'danger'" effect="dark" size="small">
              {{ isSuccess ? '✔ Thành công' : `✖ Lỗi ${q.vnp_ResponseCode}` }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Nội dung" :span="2">
            <el-text type="info" size="small">{{ q.vnp_OrderInfo || '—' }}</el-text>
          </el-descriptions-item>
        </el-descriptions>

        <el-alert v-if="!isSuccess" type="info" :closable="false" show-icon style="margin-bottom:20px;">
          Đơn hàng <strong>chưa bị hủy</strong> — bạn có thể quay lại trang chi tiết đơn để thử thanh toán lại.
        </el-alert>

        <!-- Buttons -->
        <el-space style="width:100%; justify-content:center;" :size="12">
          <el-button :type="isSuccess ? 'primary' : 'warning'" size="large" @click="goToOrder">
            <el-icon><Document /></el-icon>
            {{ isSuccess ? 'Xem chi tiết đơn hàng' : 'Thử thanh toán lại' }}
          </el-button>
          <el-button plain size="large" @click="router.push('/')">Về trang chủ</el-button>
        </el-space>

      </el-card>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  background: var(--el-bg-color-page);
  padding: 40px 16px;
  gap: 16px;
}
</style>

<script setup>
import { Document, Loading } from '@element-plus/icons-vue';
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ordersApi } from '../../api/orders.api';
import { paymentsApi } from '../../api/payments';

const route  = useRoute();
const router = useRouter();

const q          = computed(() => route.query);
const isSuccess  = computed(() => q.value.vnp_ResponseCode === '00');

// VNPay trả vnp_Amount đơn vị VND × 100 (ví dụ 1777500000 = 17.775.000đ)
const vnpAmount  = computed(() => Number(q.value.vnp_Amount || 0) / 100);

const fetchingOrder   = ref(true);
const resolvedOrderId = ref(null);
const resolvedOrder   = ref(null);   // lưu full order để lấy totalAmount chính xác
const statusMessage   = ref('');

// Số tiền hiển thị: ưu tiên totalAmount từ đơn hàng, fallback về vnp_Amount từ URL
const displayAmount = computed(() => {
  // 1. Thử lấy từ order đã fetch (kiểm tra nhiều tên field API có thể trả)
  const order = resolvedOrder.value;
  if (order) {
    const fromOrder = order.totalAmount ?? order.total ?? order.amount ?? order.grandTotal;
    if (fromOrder != null && fromOrder > 0) return fromOrder;
  }
  // 2. Fallback: lấy từ vnp_Amount trong URL (VNPay gửi đơn vị VND × 100)
  const raw = q.value.vnp_Amount ?? q.value.vnp_amount;   // phòng case khác
  const fromUrl = raw ? Number(raw) / 100 : 0;
  return fromUrl;
});

// Helper: lấy id từ order object (backend có thể trả id hoặc orderId)
function extractId(order) {
  return order?.orderId ?? order?.id ?? null;
}

onMounted(async () => {
  const txnRef = q.value.vnp_TxnRef;
  console.log('[VNP-DEBUG] Full query params:', JSON.stringify(route.query));
  console.log('[VNP-DEBUG] vnp_Amount raw:', q.value.vnp_Amount);
  if (!txnRef) { fetchingOrder.value = false; return; }

  try {
    // ── BƯỚC 1: Xác nhận chữ ký VNPay + tạo payment ──────────────────
    statusMessage.value = 'Đang xác nhận thanh toán...';
    try {
      await paymentsApi.confirmVnpay(route.query);
    } catch (err) {
      const s = err?.response?.status;
      // 409/400 = đã xác nhận trước đó → bỏ qua, không phải lỗi
      if (s !== 409 && s !== 400) {
        console.error('[VnPayResult] confirmVnpay lỗi:', err?.response?.data || err?.message);
      }
    }

    // ── BƯỚC 2: Tìm orderId & order data để navigate và hiển thị ──────
    statusMessage.value = 'Đang tải thông tin đơn hàng...';

    // Thử 1: nếu txnRef là số thuần → getById trực tiếp
    if (/^\d+$/.test(txnRef)) {
      try {
        const res   = await ordersApi.getById(txnRef);
        const order = res?.data?.data ?? res?.data;
        const oid   = extractId(order);
        if (oid) {
          resolvedOrderId.value = oid;
          resolvedOrder.value   = order;
          return;
        }
      } catch (e) { /* không tìm được, thử cách khác */ }
    }

    // Thử 2: getByOrderNumber (txnRef dạng ORD-YYYYMMDD-XXXXXX)
    try {
      const res   = await ordersApi.getByOrderNumber(txnRef);
      const order = res?.data?.data ?? res?.data;
      console.log('[VNP-DEBUG] getByOrderNumber raw response:', JSON.stringify(order));
      const oid   = extractId(order);
      if (oid) {
        resolvedOrderId.value = oid;
        resolvedOrder.value   = order;
        console.log('[VNP-DEBUG] resolvedOrder.totalAmount =', order?.totalAmount);
        return;
      }
    } catch (e) { console.warn('[VNP-DEBUG] getByOrderNumber failed:', e?.message); }

    // Thử 3: fallback – quét danh sách đơn hàng của khách
    const res  = await ordersApi.getMyOrders();
    const raw  = res?.data?.data ?? res?.data ?? [];
    const list = Array.isArray(raw) ? raw : (raw.content ?? raw.items ?? []);
    const found = list.find(o =>
      String(o.id) === String(txnRef) ||
      String(o.orderId) === String(txnRef) ||
      o.orderNumber?.replace(/-/g, '') === txnRef ||
      o.orderNumber === txnRef
    );
    if (found) {
      resolvedOrderId.value = extractId(found);
      resolvedOrder.value   = found;
    }

  } catch (err) {
    console.warn('[VnPayResult] Lỗi tổng:', err?.message);
  } finally {
    fetchingOrder.value = false;
    statusMessage.value = '';
  }
});

function goToOrder() {
  if (resolvedOrderId.value) {
    router.push(`/orders/${resolvedOrderId.value}`);
  } else {
    router.push('/my-orders');
  }
}

function formatMoney(val) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);
}

function formatVnpayDate(str) {
  if (!str || str.length < 14) return '—';
  return `${str.slice(8,10)}:${str.slice(10,12)} · ${str.slice(6,8)}/${str.slice(4,6)}/${str.slice(0,4)}`;
}

const ERROR_CODES = {
  '07': 'Giao dịch bị nghi ngờ gian lận.',
  '09': 'Thẻ/Tài khoản chưa đăng ký Internet Banking.',
  '10': 'Xác thực thông tin thẻ quá 3 lần.',
  '11': 'Hết hạn chờ thanh toán — vui lòng thực hiện lại.',
  '12': 'Thẻ/Tài khoản bị khóa.',
  '13': 'Mật khẩu OTP không đúng.',
  '24': 'Giao dịch bị hủy bởi khách hàng.',
  '51': 'Tài khoản không đủ số dư.',
  '65': 'Tài khoản vượt hạn mức giao dịch trong ngày.',
  '75': 'Ngân hàng đang bảo trì.',
  '79': 'Nhập sai mật khẩu thanh toán quá số lần.',
  '99': 'Lỗi không xác định.',
};

const responseMessage = computed(() => {
  const code = q.value.vnp_ResponseCode;
  if (!code || code === '00') return '';
  return ERROR_CODES[code] ?? `Giao dịch thất bại (mã lỗi: ${code}).`;
});
</script>
