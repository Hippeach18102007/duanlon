<!-- src/pages/customer/PaymentSuccess.vue -->
<template>
  <div style="max-width: 900px; margin: 0 auto; padding: 32px 24px 80px;">

    <!-- Loading -->
    <el-skeleton v-if="loading" :rows="8" animated />

    <template v-else>
      <!-- Hero: el-result thay thế hoàn toàn custom hero-card -->
      <el-card shadow="never" class="profile-card" style="margin-bottom: 24px; text-align: center;">
        <el-result
          icon="success"
          title="Đặt hàng thành công! 🎉"
          sub-title="Cảm ơn bạn đã mua sắm. Đơn hàng của bạn đang được xử lý."
        />
      </el-card>

      <el-alert v-if="error" type="error" :title="error" show-icon style="margin-bottom: 20px;" />

      <el-row :gutter="20" align="top">

        <!-- LEFT -->
        <el-col :xs="24" :md="14">

          <!-- Payment info: el-descriptions -->
          <el-card v-if="payment" shadow="never" class="profile-card" style="margin-bottom: 16px;">
            <template #header>
              <el-space :size="8">
                <el-icon><CreditCard /></el-icon>
                <el-text tag="b">Thông tin thanh toán</el-text>
              </el-space>
            </template>

            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="Mã đơn hàng">
                <el-link type="primary" :href="`/orders/${payment.orderId}`">#{{ payment.orderId }}</el-link>
              </el-descriptions-item>
              <el-descriptions-item label="Số tiền">
                <el-text type="success" style="font-size: 18px; font-weight: 800;">{{ formatCurrency(payment.amount) }}</el-text>
              </el-descriptions-item>
              <el-descriptions-item label="Phương thức">
                <el-tag effect="plain" size="small">{{ formatMethod(payment.method) }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="Thời gian">
                <el-text size="small">{{ formatDate(payment.paidAt) }}</el-text>
              </el-descriptions-item>
              <el-descriptions-item v-if="payment.transactionRef" label="Mã giao dịch">
                <el-text size="small" style="font-family: monospace;">{{ payment.transactionRef }}</el-text>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- Loyalty info -->
          <el-card
            v-if="customerInfo"
            shadow="never"
            class="profile-card"
            style="background: var(--el-color-warning-light-9); border-color: var(--el-color-warning-light-5);"
          >
            <template #header>
              <el-row align="middle" justify="space-between">
                <el-space :size="8">
                  <el-icon color="var(--el-color-warning)"><Star /></el-icon>
                  <el-space direction="vertical" :size="2">
                    <el-text tag="b">Điểm thưởng tích lũy</el-text>
                    <el-text size="small" type="info">Đơn hàng này mang lại cho bạn</el-text>
                  </el-space>
                </el-space>
                <el-text type="warning" style="font-size: 26px; font-weight: 900;">+{{ pointsEarned }} <small style="font-size:13px; font-weight:600;">điểm</small></el-text>
              </el-row>
            </template>

            <el-row :gutter="0" justify="space-around" align="middle">
              <el-col :span="7" style="text-align: center;">
                <el-statistic title="Tổng điểm" :value="customerInfo.loyaltyPoints" />
              </el-col>
              <el-divider direction="vertical" style="height: 40px;" />
              <el-col :span="7" style="text-align: center;">
                <el-statistic title="Hạng thành viên">
                  <template #number>
                    <el-tag effect="dark" size="small">{{ customerInfo.vipTierDisplay }}</el-tag>
                  </template>
                </el-statistic>
              </el-col>
              <el-divider direction="vertical" style="height: 40px;" />
              <el-col :span="7" style="text-align: center;">
                <el-statistic title="Điểm lên hạng">
                  <template #number>
                    <el-text style="font-size: 18px; font-weight: 700;">
                      {{ customerInfo.pointsToNextTier > 0 ? customerInfo.pointsToNextTier : '—' }}
                    </el-text>
                  </template>
                </el-statistic>
              </el-col>
            </el-row>

            <el-alert
              v-if="customerInfo.pointsToNextTier === 0"
              type="warning"
              show-icon
              :closable="false"
              title="🏆 Bạn đã đạt hạng cao nhất!"
              style="margin-top: 14px;"
            />
          </el-card>

        </el-col>

        <!-- RIGHT: Actions -->
        <el-col :xs="24" :md="10">
          <el-card shadow="never" class="profile-card" style="margin-bottom: 16px;">
            <template #header>
              <el-space :size="8">
                <el-icon><ArrowRight /></el-icon>
                <el-text tag="b">Tiếp theo</el-text>
              </el-space>
            </template>

            <el-space direction="vertical" fill :size="10" style="width: 100%;">
              <el-button
                type="primary"
                size="large"
                style="width: 100%; justify-content: flex-start;"
                @click="$router.push(`/orders/${payment?.orderId}`)"
              >
                <el-icon><Document /></el-icon>
                <span style="flex: 1; text-align: left;">
                  <div>Xem chi tiết đơn hàng</div>
                  <div style="font-size: 11px; font-weight: 400; opacity: 0.8;">Theo dõi trạng thái vận chuyển</div>
                </span>
              </el-button>

              <el-button
                size="large"
                style="width: 100%; justify-content: flex-start;"
                @click="$router.push('/')"
              >
                <el-icon><Shop /></el-icon>
                <span style="flex: 1; text-align: left;">
                  <div>Tiếp tục mua sắm</div>
                  <div style="font-size: 11px; font-weight: 400; color: var(--el-text-color-secondary);">Khám phá thêm sản phẩm</div>
                </span>
              </el-button>

              <el-button
                size="large"
                style="width: 100%; justify-content: flex-start;"
                @click="printReceipt"
              >
                <el-icon><Printer /></el-icon>
                <span style="flex: 1; text-align: left;">
                  <div>In hóa đơn</div>
                  <div style="font-size: 11px; font-weight: 400; color: var(--el-text-color-secondary);">Lưu lại biên lai thanh toán</div>
                </span>
              </el-button>
            </el-space>
          </el-card>

          <el-alert
            type="success"
            :closable="false"
            show-icon
            title="Email xác nhận đơn hàng đã được gửi đến địa chỉ email của bạn."
          />
        </el-col>

      </el-row>
    </template>
  </div>
</template>

<script setup>
import { ArrowRight, CreditCard, Document, Printer, Shop, Star } from "@element-plus/icons-vue";
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
  const m = { CASH: "Tiền mặt", BANK_TRANSFER: "Chuyển khoản", TRANSFER: "Chuyển khoản", CREDIT_CARD: "Thẻ tín dụng", CARD: "Thẻ tín dụng" };
  return m[method] || method;
}

function printReceipt() { window.print(); }

onMounted(() => {
  if (paymentId.value) loadPaymentDetails();
  else error.value = "Không tìm thấy mã thanh toán.";
});
</script>

<style scoped>
@media print {
  .el-button, .el-alert { display: none; }
}
</style>