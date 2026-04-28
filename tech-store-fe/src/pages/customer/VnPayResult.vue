<!-- src/pages/customer/VnPayResult.vue -->
<template>
  <div style="min-height:100vh; display:flex; align-items:center; justify-content:center; background:var(--el-bg-color-page); padding:40px 16px;">
    <div style="width:100%; max-width:640px;">

      <!-- ── LOADING ── -->
      <el-card v-if="fetchingOrder" shadow="never">
        <div style="text-align:center; padding:40px 0;">
          <el-icon class="is-loading" :size="48" color="var(--el-color-primary)"><Loading /></el-icon>
          <p style="margin-top:16px; font-size:15px; font-weight:600;">Đang xử lý kết quả thanh toán</p>
          <p style="margin-top:4px; font-size:13px; color:var(--el-text-color-secondary);">
            {{ statusMessage || 'Vui lòng chờ trong giây lát...' }}
          </p>
        </div>
      </el-card>

      <!-- ── RESULT ── -->
      <el-card v-else shadow="never">

        <el-result
          :icon="isSuccess ? 'success' : 'error'"
          :title="isSuccess ? 'Thanh toán thành công! 🎉' : 'Thanh toán thất bại'"
          :sub-title="isSuccess
            ? 'Cảm ơn bạn đã mua hàng. Đơn hàng đang được xử lý.'
            : responseMessage || 'Giao dịch không thành công.'"
        >
          <template v-if="isSuccess" #extra>
            <div style="background:var(--el-color-success-light-9); border:1px solid var(--el-color-success-light-5); border-radius:10px; padding:14px 28px; text-align:center;">
              <el-text size="small" type="success" style="font-weight:700; letter-spacing:.06em; text-transform:uppercase;">Số tiền đã thanh toán</el-text>
              <div style="font-size:30px; font-weight:800; color:var(--el-color-success); margin-top:4px;">{{ formatMoney(vnpAmount) }}</div>
            </div>
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
const vnpAmount  = computed(() => Number(q.value.vnp_Amount || 0) / 100);

const fetchingOrder   = ref(true);
const resolvedOrderId = ref(null);
const statusMessage   = ref('');

onMounted(async () => {
  const txnRef = q.value.vnp_TxnRef;   // Có thể là orderId số HOẶC orderNumber chuỗi
  if (!txnRef) { fetchingOrder.value = false; return; }

  try {
    // ── BƯỚC 1: Gọi backend xác nhận chữ ký + tạo payment ─────────────
    statusMessage.value = 'Đang xác nhận thanh toán...';
    try {
      await paymentsApi.confirmVnpay(route.query);
      console.log('[VnPayResult] confirmVnpay OK');
    } catch (err) {
      const s = err?.response?.status;
      if (s === 409 || s === 400) {
        console.warn('[VnPayResult] Đã xác nhận trước đó, bỏ qua');
      } else {
        console.error('[VnPayResult] confirmVnpay lỗi:', err?.response?.data || err?.message);
      }
    }

    // ── BƯỚC 2: Tìm orderId để navigate ────────────────────────────────
    statusMessage.value = 'Đang tải thông tin đơn hàng...';

    console.log('[DEBUG] txnRef =', txnRef, '| isNumeric =', /^\d+$/.test(txnRef));

    // Thử 1: nếu txnRef là số → getById trực tiếp
    if (/^\d+$/.test(txnRef)) {
      try {
        const res   = await ordersApi.getById(txnRef);
        const order = res?.data?.data ?? res?.data;
        console.log('[DEBUG] getById result =', order);
        if (order?.id) { resolvedOrderId.value = order.id; return; }
      } catch (e) { console.warn('[DEBUG] getById failed:', e?.response?.status); }
    }

    // Thử 2: getByOrderNumber
    try {
      const res   = await ordersApi.getByOrderNumber(txnRef);
      const order = res?.data?.data ?? res?.data;
      console.log('[DEBUG] getByOrderNumber result =', order);
      if (order?.id) { resolvedOrderId.value = order.id; return; }
    } catch (e) { console.warn('[DEBUG] getByOrderNumber failed:', e?.response?.status); }

    // Thử 3: fallback getMyOrders
    const res    = await ordersApi.getMyOrders();
    const raw    = res?.data?.data ?? res?.data ?? res ?? [];
    const list   = Array.isArray(raw) ? raw : (raw.content ?? raw.items ?? []);
    console.log('[DEBUG] my-orders list length =', list.length);
    console.log('[DEBUG] first item sample =', list[0]);  // xem field names
    const found  = list.find(o =>
      String(o.id) === String(txnRef) ||
      o.orderNumber?.replace(/-/g, '') === txnRef ||
      o.orderNumber === txnRef
    );
    console.log('[DEBUG] found =', found);
    if (found?.id) resolvedOrderId.value = found.id;

    console.log('[VnPayResult] txnRef =', txnRef, '→ resolvedOrderId =', resolvedOrderId.value);

  } catch (err) {
    console.warn('[VnPayResult] Lỗi tổng:', err?.message);
  } finally {
    fetchingOrder.value = false;
    statusMessage.value = '';
  }
});

function goToOrder() {
  router.push(resolvedOrderId.value ? `/orders/${resolvedOrderId.value}` : '/my-orders');
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