<!-- \src\pages\customer\PaymentSuccess.vue -->
<template>
  <div class="success-page">

    <!-- Loading -->
    <div v-if="loading" class="loading-wrap">
      <div class="spinner" />
      <p>Đang tải thông tin...</p>
    </div>

    <template v-else>
      <!-- Hero -->
      <div class="hero-card">
        <div class="confetti-row">🎉</div>
        <div class="check-circle">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
        </div>
        <h1>Đặt hàng thành công!</h1>
        <p>Cảm ơn bạn đã mua sắm. Đơn hàng của bạn đang được xử lý.</p>
      </div>

      <div class="content-layout">
        <!-- LEFT -->
        <div class="left-col">

          <!-- Payment info -->
          <div v-if="payment" class="section-card">
            <div class="section-label">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="1" y="4" width="22" height="16" rx="2"/><path d="M1 10h22"/></svg>
              Thông tin thanh toán
            </div>

            <div class="info-grid">
              <div class="info-row">
                <span class="info-key">Mã đơn hàng</span>
                <router-link :to="`/orders/${payment.orderId}`" class="info-link">#{{ payment.orderId }}</router-link>
              </div>
              <div class="info-row">
                <span class="info-key">Số tiền</span>
                <span class="info-amount">{{ formatCurrency(payment.amount) }}</span>
              </div>
              <div class="info-row">
                <span class="info-key">Phương thức</span>
                <span class="method-tag" :class="payment.method?.toLowerCase()">{{ formatMethod(payment.method) }}</span>
              </div>
              <div class="info-row">
                <span class="info-key">Thời gian</span>
                <span class="info-val">{{ formatDate(payment.paidAt) }}</span>
              </div>
              <div v-if="payment.transactionRef" class="info-row">
                <span class="info-key">Mã giao dịch</span>
                <code class="info-code">{{ payment.transactionRef }}</code>
              </div>
            </div>
          </div>

          <!-- Loyalty -->
          <div v-if="customerInfo" class="section-card loyalty-card">
            <div class="loyalty-header">
              <div class="loyalty-icon">⭐</div>
              <div>
                <div class="loyalty-title">Điểm thưởng tích lũy</div>
                <div class="loyalty-sub">Đơn hàng này mang lại cho bạn</div>
              </div>
              <div class="points-earned">+{{ pointsEarned }} <span>điểm</span></div>
            </div>

            <div class="loyalty-stats">
              <div class="stat-item">
                <div class="stat-val">{{ customerInfo.loyaltyPoints }}</div>
                <div class="stat-key">Tổng điểm</div>
              </div>
              <div class="stat-divider" />
              <div class="stat-item">
                <div class="stat-val">
                  <span class="tier-badge" :class="customerInfo.vipTier?.toLowerCase()">{{ customerInfo.vipTierDisplay }}</span>
                </div>
                <div class="stat-key">Hạng thành viên</div>
              </div>
              <div class="stat-divider" />
              <div class="stat-item">
                <div class="stat-val">{{ customerInfo.pointsToNextTier > 0 ? customerInfo.pointsToNextTier : '—' }}</div>
                <div class="stat-key">Điểm lên hạng</div>
              </div>
            </div>

            <div v-if="customerInfo.pointsToNextTier === 0" class="max-tier">
              🏆 Bạn đã đạt hạng cao nhất!
            </div>
          </div>

        </div>

        <!-- RIGHT: Actions -->
        <div class="right-col">
          <div class="section-card actions-card">
            <div class="section-label">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
              Tiếp theo
            </div>

            <div class="action-list">
              <button class="action-btn primary" @click="$router.push(`/orders/${payment?.orderId}`)">
                <div class="action-icon blue">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
                </div>
                <div class="action-text">
                  <div class="action-title">Xem chi tiết đơn hàng</div>
                  <div class="action-sub">Theo dõi trạng thái vận chuyển</div>
                </div>
              </button>

              <button class="action-btn" @click="$router.push('/')">
                <div class="action-icon gray">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8M12 17v4"/></svg>
                </div>
                <div class="action-text">
                  <div class="action-title">Tiếp tục mua sắm</div>
                  <div class="action-sub">Khám phá thêm sản phẩm</div>
                </div>
              </button>

              <button class="action-btn" @click="printReceipt">
                <div class="action-icon gray">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 6 2 18 2 18 9"/><path d="M6 18H4a2 2 0 01-2-2v-5a2 2 0 012-2h16a2 2 0 012 2v5a2 2 0 01-2 2h-2"/><rect x="6" y="14" width="12" height="8"/></svg>
                </div>
                <div class="action-text">
                  <div class="action-title">In hóa đơn</div>
                  <div class="action-sub">Lưu lại biên lai thanh toán</div>
                </div>
              </button>
            </div>
          </div>

          <!-- Note -->
          <div class="note-card">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            Email xác nhận đơn hàng đã được gửi đến địa chỉ email của bạn.
          </div>
        </div>
      </div>

      <div v-if="error" class="alert-box">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        {{ error }}
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRoute } from "vue-router";
import { paymentsApi } from "../../api/payments";
import { toast } from "../../ui/toast";

const route = useRoute();
const paymentId = computed(() => route.params.paymentId);

const loading = ref(false);
const error = ref("");
const payment = ref(null);
const customerInfo = ref(null);

const pointsEarned = computed(() => {
  if (!payment.value?.amount) return 0;
  return Math.floor(payment.value.amount / 10000);
});

async function loadPaymentDetails() {
  loading.value = true;
  error.value = "";
  try {
    const { data } = await paymentsApi.getById(paymentId.value);
    payment.value = data;
    if (data.customerId) {
      customerInfo.value = {
        loyaltyPoints: (pointsEarned.value || 0) + 100,
        vipTier: "BRONZE",
        vipTierDisplay: "Bronze",
        pointsToNextTier: 50,
      };
    }
  } catch (e) {
    const msg = e?.response?.data?.message || e?.message || "Không thể tải thông tin";
    error.value = msg;
    toast(msg, "error");
  } finally {
    loading.value = false;
  }
}

function formatCurrency(amount) {
  if (!amount && amount !== 0) return "₫0";
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(amount);
}

function formatDate(date) {
  if (!date) return "N/A";
  return new Date(date).toLocaleString("vi-VN");
}

function formatMethod(method) {
  const m = { CASH: "Tiền mặt", BANK_TRANSFER: "Chuyển khoản", TRANSFER: "Chuyển khoản", CREDIT_CARD: "Thẻ tín dụng", CARD: "Thẻ tín dụng"};
  return m[method] || method;
}

function printReceipt() { window.print(); }

onMounted(() => {
  if (paymentId.value) loadPaymentDetails();
  else error.value = "Không tìm thấy mã thanh toán.";
});
</script>

<style scoped>
.success-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 32px 24px 80px;
  font-family: 'Be Vietnam Pro', 'Segoe UI', sans-serif;
}

/* Loading */
.loading-wrap { text-align: center; padding: 80px 0; color: #9ca3af; }
.loading-wrap p { margin-top: 16px; font-size: 14px; }
.spinner {
  width: 36px; height: 36px;
  border: 3px solid #e5e7eb;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin .7s linear infinite;
  margin: 0 auto;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* Hero */
.hero-card {
  text-align: center;
  padding: 48px 24px 40px;
  background: white;
  border: 1.5px solid #f0f0f0;
  border-radius: 20px;
  margin-bottom: 24px;
}
.confetti-row { font-size: 28px; margin-bottom: 20px; }
.check-circle {
  width: 72px; height: 72px;
  background: linear-gradient(135deg, #16a34a, #22c55e);
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 20px;
  box-shadow: 0 8px 24px rgba(34,197,94,.3);
  animation: popIn .5s cubic-bezier(.34,1.56,.64,1);
}
@keyframes popIn { from { transform: scale(0); opacity: 0; } to { transform: scale(1); opacity: 1; } }
.hero-card h1 { font-size: 26px; font-weight: 800; color: #111827; margin: 0 0 8px; }
.hero-card p { font-size: 14px; color: #6b7280; margin: 0; }

/* Layout */
.content-layout {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 20px;
  align-items: start;
}
@media (max-width: 768px) { .content-layout { grid-template-columns: 1fr; } }

/* Cards */
.section-card {
  background: white;
  border: 1.5px solid #f0f0f0;
  border-radius: 16px;
  padding: 22px;
  margin-bottom: 16px;
}
.section-label {
  display: flex; align-items: center; gap: 7px;
  font-size: 11px; font-weight: 700; text-transform: uppercase;
  letter-spacing: .07em; color: #9ca3af; margin-bottom: 18px;
}

/* Info grid */
.info-grid { display: flex; flex-direction: column; gap: 14px; }
.info-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.info-key { font-size: 13px; color: #9ca3af; flex-shrink: 0; }
.info-val { font-size: 13px; color: #374151; font-weight: 500; }
.info-link { font-size: 13px; font-weight: 700; color: #2563eb; text-decoration: none; }
.info-link:hover { text-decoration: underline; }
.info-amount { font-size: 18px; font-weight: 800; color: #16a34a; }
.info-code { font-size: 12px; background: #f9fafb; padding: 3px 8px; border-radius: 6px; color: #6b7280; font-family: monospace; }
.method-tag { font-size: 12px; font-weight: 600; padding: 3px 10px; border-radius: 50px; }
.method-tag.cash { background: #dcfce7; color: #16a34a; }
.method-tag.transfer, .method-tag.bank_transfer { background: #dbeafe; color: #2563eb; }
.method-tag.card, .method-tag.credit_card { background: #fef9c3; color: #b45309; }
.method-tag.e_wallet { background: #ede9fe; color: #7c3aed; }

/* Loyalty */
.loyalty-card { background: linear-gradient(135deg, #fffbeb, #fefce8); border-color: #fde68a; }
.loyalty-header { display: flex; align-items: center; gap: 14px; margin-bottom: 20px; }
.loyalty-icon { font-size: 28px; }
.loyalty-title { font-size: 14px; font-weight: 700; color: #111827; }
.loyalty-sub { font-size: 12px; color: #9ca3af; margin-top: 2px; }
.points-earned { margin-left: auto; font-size: 26px; font-weight: 900; color: #d97706; line-height: 1; }
.points-earned span { font-size: 13px; font-weight: 600; }

.loyalty-stats { display: flex; align-items: center; background: white; border-radius: 12px; padding: 16px; gap: 0; }
.stat-item { flex: 1; text-align: center; }
.stat-val { font-size: 18px; font-weight: 800; color: #111827; }
.stat-key { font-size: 11px; color: #9ca3af; margin-top: 3px; }
.stat-divider { width: 1px; height: 36px; background: #f0f0f0; }
.tier-badge { font-size: 12px; font-weight: 700; padding: 3px 10px; border-radius: 50px; }
.tier-badge.bronze { background: #fde8d8; color: #c2410c; }
.tier-badge.silver { background: #f1f5f9; color: #475569; }
.tier-badge.gold { background: #fef9c3; color: #b45309; }
.tier-badge.diamond { background: #ede9fe; color: #7c3aed; }
.max-tier { text-align: center; font-size: 13px; font-weight: 700; color: #d97706; margin-top: 14px; }

/* Actions */
.actions-card { padding: 22px; }
.action-list { display: flex; flex-direction: column; gap: 10px; }
.action-btn {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 16px;
  border: 1.5px solid #f3f4f6;
  border-radius: 12px;
  background: white;
  cursor: pointer;
  transition: all .15s;
  text-align: left;
  width: 100%;
}
.action-btn:hover { border-color: #bfdbfe; background: #f8faff; transform: translateX(2px); }
.action-btn.primary { border-color: #2563eb; background: #eff6ff; }
.action-btn.primary:hover { background: #dbeafe; }
.action-icon {
  width: 38px; height: 38px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.action-icon.blue { background: #2563eb; color: white; }
.action-icon.gray { background: #f3f4f6; color: #374151; }
.action-title { font-size: 13px; font-weight: 700; color: #111827; }
.action-sub { font-size: 11px; color: #9ca3af; margin-top: 2px; }

/* Note */
.note-card {
  display: flex; align-items: flex-start; gap: 8px;
  padding: 14px 16px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 12px;
  font-size: 12px;
  color: #16a34a;
  line-height: 1.5;
}

/* Alert */
.alert-box {
  display: flex; align-items: center; gap: 8px;
  padding: 12px 16px; margin-top: 16px;
  background: #fef2f2; border: 1px solid #fecaca;
  border-radius: 12px; font-size: 13px; color: #dc2626;
}

/* Print */
@media print {
  .action-list, .note-card { display: none; }
  .hero-card { border: none; }
}
</style>