<!-- \src\pages\customer\OrderCreate.vue -->
<template>
  <div class="checkout-body">

    <!-- ══ LEFT COLUMN ══ -->
    <div class="checkout-left">

      <!-- Cart items -->
      <el-card shadow="never" style="margin-bottom: 14px;">
        <template #header>
          <el-space :size="10" align="center">
            <el-icon><ShoppingCart /></el-icon>
            <el-text tag="b">Sản phẩm đã chọn</el-text>
            <el-badge v-if="form.items.length > 0" :value="form.items.length" type="primary" />
          </el-space>
        </template>

        <el-empty v-if="form.items.length === 0" :image-size="60" description="Giỏ hàng của bạn đang trống">
          <el-button type="primary" plain @click="$router.push('/')">Khám phá ngay</el-button>
        </el-empty>

        <el-space v-else direction="vertical" fill :size="0" style="width: 100%;">
          <div
            v-for="(item, idx) in form.items"
            :key="idx"
            style="display: flex; align-items: center; gap: 12px; padding: 12px 0;
                   border-bottom: 1px solid var(--el-border-color-lighter);"
          >
            <div class="item-img-wrap">
              <img v-if="item.imageUrl" :src="fixImageUrl(item.imageUrl)" :alt="item.productName" class="item-img" />
              <el-icon v-else :size="20" style="color: var(--el-text-color-placeholder);"><Picture /></el-icon>
            </div>
            <el-text style="flex: 1; min-width: 0; font-weight: 500;">{{ item.productName }}</el-text>
            <el-space :size="8" align="center" style="flex-shrink: 0;">
              <el-text type="info" size="small">×{{ item.quantity }}</el-text>
              <el-text type="primary" style="font-weight: 700;">
                {{ formatMoney((item.price ?? item.unitPrice ?? 0) * (item.quantity ?? 1)) }}
              </el-text>
            </el-space>
          </div>
        </el-space>
      </el-card>

      <!-- Delivery method -->
      <el-card shadow="never" style="margin-bottom: 14px;">
        <template #header>
          <el-space :size="10" align="center">
            <el-icon><Van /></el-icon>
            <el-text tag="b">Hình thức giao hàng</el-text>
          </el-space>
        </template>
        <el-radio-group v-model="form.channel" style="width: 100%;">
          <el-space direction="vertical" fill :size="10" style="width: 100%;">
            <el-card
              shadow="never"
              :body-style="{ padding: '14px 16px' }"
              :style="form.channel === 'OFFLINE' ? 'border-color: var(--el-color-primary); background: var(--el-color-primary-light-9);' : ''"
              style="cursor: pointer;"
              @click="form.channel = 'OFFLINE'"
            >
              <el-row align="middle" justify="space-between">
                <el-space :size="12">
                  <el-radio value="OFFLINE" />
                  <span style="font-size: 20px;">🏬</span>
                  <el-space direction="vertical" :size="2">
                    <el-text tag="b">Nhận tại cửa hàng</el-text>
                    <el-text size="small" type="info">Đến nhận trực tiếp tại quầy — không mất phí</el-text>
                  </el-space>
                </el-space>
                <el-tag type="success" effect="plain" size="small">Miễn phí</el-tag>
              </el-row>
            </el-card>

            <!-- ✅ FIX: Bỏ "+70.000 ₫", thay bằng "Miễn phí" -->
            <el-card
              shadow="never"
              :body-style="{ padding: '14px 16px' }"
              :style="form.channel === 'ONLINE' ? 'border-color: var(--el-color-primary); background: var(--el-color-primary-light-9);' : ''"
              style="cursor: pointer;"
              @click="form.channel = 'ONLINE'"
            >
              <el-row align="middle" justify="space-between">
                <el-space :size="12">
                  <el-radio value="ONLINE" />
                  <span style="font-size: 20px;">🚚</span>
                  <el-space direction="vertical" :size="2">
                    <el-text tag="b">Giao tại nhà</el-text>
                    <el-text size="small" type="info">Giao hàng nhanh 2–4 giờ trong nội thành</el-text>
                  </el-space>
                </el-space>
                <el-tag type="success" effect="plain" size="small">Miễn phí</el-tag>
              </el-row>
            </el-card>
          </el-space>
        </el-radio-group>
      </el-card>

      <el-card v-if="form.channel === 'ONLINE'" shadow="never" style="margin-bottom: 14px;">
        <template #header>
          <el-row justify="space-between" align="middle">
            <el-space :size="10" align="center">
              <el-icon><Location /></el-icon>
              <el-text tag="b">Địa chỉ nhận hàng</el-text>
            </el-space>
            <el-button type="primary" link @click="openAddressModal">
              Thay đổi
            </el-button>
          </el-row>
        </template>
        <div v-if="form.shippingAddress" style="line-height: 1.5; font-size: 15px;">
          <el-text>{{ form.shippingAddress }}</el-text>
        </div>
        <div v-else>
          <el-text type="danger">Vui lòng cập nhật địa chỉ giao hàng để tiếp tục!</el-text>
        </div>
      </el-card>

      <!-- Notes -->
      <el-card shadow="never">
        <template #header>
          <el-space :size="10" align="center">
            <el-icon><Document /></el-icon>
            <el-text tag="b">Ghi chú đơn hàng</el-text>
            <el-tag size="small" type="info" effect="plain">Tùy chọn</el-tag>
          </el-space>
        </template>
        <el-input
          v-model="form.notes"
          type="textarea"
          :rows="3"
          placeholder="Ví dụ: Gọi trước khi giao, để hàng ở bảo vệ..."
        />
      </el-card>
    </div>

    <!-- ══ RIGHT COLUMN ══ -->
    <div class="checkout-right">

      <!-- Payment method -->
      <el-card shadow="never" style="margin-bottom: 14px;">
        <template #header>
          <el-space :size="10" align="center">
            <el-icon><CreditCard /></el-icon>
            <el-text tag="b">Phương thức thanh toán</el-text>
          </el-space>
        </template>
        <el-radio-group v-model="form.paymentMethod" style="width: 100%;">
          <el-space direction="vertical" fill :size="8" style="width: 100%;">
            <el-card
              v-for="opt in paymentOptions"
              :key="opt.value"
              shadow="never"
              :body-style="{ padding: '12px 16px' }"
              :style="form.paymentMethod === opt.value ? 'border-color: var(--el-color-primary); background: var(--el-color-primary-light-9);' : ''"
              style="cursor: pointer;"
              @click="form.paymentMethod = opt.value"
            >
              <el-row align="middle" justify="space-between">
                <el-space :size="10">
                  <el-radio :value="opt.value" />
                  <span style="font-size: 18px;">{{ opt.icon }}</span>
                  <el-text style="font-weight: 500;">{{ opt.label }}</el-text>
                </el-space>
                <el-icon v-if="form.paymentMethod === opt.value" style="color: var(--el-color-primary);"><CircleCheck /></el-icon>
              </el-row>
            </el-card>
          </el-space>
        </el-radio-group>

        <!-- ✅ FIX: TRANSFER chỉ thông báo, không cho tự xác nhận -->
        <el-alert
          v-if="form.paymentMethod === 'TRANSFER'"
          type="info"
          :closable="false"
          show-icon
          style="margin-top: 12px;"
        >
          <template #title>Thanh toán chuyển khoản</template>
          <template #default>
            Mã QR sẽ hiển thị sau khi đặt hàng. Sau khi chuyển khoản, vui lòng chờ admin xác nhận — đơn hàng sẽ được cập nhật tự động.
          </template>
        </el-alert>
      </el-card>

      <!-- Voucher -->
      <el-card shadow="never" style="margin-bottom: 14px;">
        <template #header>
          <el-space :size="10" align="center">
            <el-icon><Ticket /></el-icon>
            <el-text tag="b">Mã khuyến mãi</el-text>
          </el-space>
        </template>

        <el-card
          v-if="selectedVoucher"
          shadow="never"
          :body-style="{ padding: '12px 16px' }"
          style="background: var(--el-color-success-light-9); border-color: var(--el-color-success-light-5);"
        >
          <el-row align="middle" justify="space-between">
            <el-space :size="10">
              <el-icon style="color: var(--el-color-success);"><CircleCheck /></el-icon>
              <el-space direction="vertical" :size="2">
                <el-space :size="8">
                  <el-text tag="b">{{ selectedVoucher.code }}</el-text>
                  <el-text type="danger" style="font-weight: 700;">−{{ formatMoney(promoResult?.discountAmount) }}</el-text>
                </el-space>
                <el-text size="small" type="info">{{ selectedVoucher.name }}</el-text>
              </el-space>
            </el-space>
            <el-button size="small" plain @click="openVoucherModal">Đổi mã</el-button>
          </el-row>
        </el-card>

        <el-button v-else style="width: 100%;" plain @click="openVoucherModal">
          🎟️&nbsp; Chọn mã giảm giá
          <el-text v-if="availableVouchers.length > 0" size="small" type="primary" style="margin-left: 8px;">
            {{ availableVouchers.length }} mã
          </el-text>
          <el-icon style="margin-left: auto;"><ArrowRight /></el-icon>
        </el-button>

        <el-alert
          v-if="promoError"
          type="error"
          :title="promoError"
          :closable="false"
          show-icon
          style="margin-top: 10px;"
        />
      </el-card>

      <!-- Summary -->
      <el-card shadow="never" style="margin-bottom: 14px;">
        <template #header>
          <el-space :size="10" align="center">
            <el-icon><List /></el-icon>
            <el-text tag="b">Tóm tắt đơn hàng</el-text>
          </el-space>
        </template>

        <el-space direction="vertical" fill :size="10" style="width: 100%;">
          <el-row justify="space-between">
            <el-text size="small">Tạm tính ({{ form.items.length }} sản phẩm)</el-text>
            <el-text size="small">{{ formatMoney(subtotal) }}</el-text>
          </el-row>
          <el-row v-if="selectedVoucher && promoResult" justify="space-between">
            <el-text size="small">
              <el-icon><Ticket /></el-icon>
              Mã <el-tag size="small" type="primary" effect="plain">{{ selectedVoucher.code }}</el-tag>
            </el-text>
            <el-text type="danger" size="small" style="font-weight: 600;">−{{ formatMoney(promoResult.discountAmount) }}</el-text>
          </el-row>
          <!-- ✅ FIX: Luôn hiển thị "Miễn phí" cho phí vận chuyển -->
          <el-row justify="space-between" align="middle">
            <el-text size="small">{{ form.channel === 'ONLINE' ? '🚚 Phí vận chuyển' : '🏬 Nhận tại cửa hàng' }}</el-text>
            <el-tag type="success" effect="plain" size="small">Miễn phí</el-tag>
          </el-row>
        </el-space>

        <el-divider style="margin: 14px 0;" />

        <el-row justify="space-between" align="middle">
          <el-space direction="vertical" :size="2">
            <el-text style="font-weight: 600;">Tổng thanh toán</el-text>
            <el-text size="small" type="info">Đã bao gồm VAT (nếu có)</el-text>
          </el-space>
          <el-text type="primary" style="font-size: 22px; font-weight: 700;">{{ formatMoney(totalAfterDiscount) }}</el-text>
        </el-row>

        <el-alert
          v-if="selectedVoucher && promoResult"
          type="success"
          :closable="false"
          style="margin-top: 12px;"
        >
          🎉 Bạn tiết kiệm được <strong>{{ formatMoney(promoResult.discountAmount) }}</strong> cho đơn hàng này!
        </el-alert>
      </el-card>

      <el-alert v-if="alert" type="error" :title="alert" show-icon :closable="false" style="margin-bottom: 14px;" />

      <el-button
        type="primary"
        size="large"
        style="width: 100%;"
        :loading="loading"
        :disabled="form.items.length === 0 || (form.channel === 'ONLINE' && (!form.shippingAddress || form.shippingAddress.trim() === ''))"
        @click="submit"
      >
        <el-icon v-if="!loading"><Right /></el-icon>
        {{ loading ? 'Đang xử lý đơn hàng...' : `Đặt hàng ngay · ${formatMoney(totalAfterDiscount)}` }}
      </el-button>
    </div>
  </div>

  <!-- ══ VOUCHER DIALOG ══ -->
  <el-dialog v-model="voucherModalOpen" title="🎟️ Chọn Voucher" width="520px" align-center>
    <template #header>
      <el-space :size="8" align="center">
        <el-text tag="b" style="font-size: 16px;">🎟️ Chọn Voucher</el-text>
        <el-tag type="primary" effect="plain" size="small">{{ availableVouchers.length }} mã khả dụng</el-tag>
      </el-space>
    </template>

    <div v-if="voucherLoading" style="text-align: center; padding: 32px 0;">
      <el-icon class="is-loading" :size="28"><Loading /></el-icon>
      <el-text type="info" style="display: block; margin-top: 12px;">Đang tải mã khuyến mãi...</el-text>
    </div>

    <el-empty
      v-else-if="availableVouchers.length === 0"
      :image-size="60"
      description="Hiện không có mã khuyến mãi phù hợp cho bạn"
    />

    <el-scrollbar v-else max-height="400px">
      <el-space direction="vertical" fill :size="10" style="width: 100%; padding-right: 4px;">
        <el-card
          v-for="v in availableVouchers"
          :key="v.id"
          shadow="never"
          :body-style="{ padding: '14px 16px' }"
          :style="[
            tempSelectedId === v.id ? 'border-color: var(--el-color-primary); background: var(--el-color-primary-light-9);' : '',
            !isVoucherApplicable(v) ? 'opacity: 0.55;' : 'cursor: pointer;'
          ]"
          @click="isVoucherApplicable(v) && selectVoucher(v.id)"
        >
          <el-row align="middle" :gutter="12">
            <el-col flex="none">
              <el-tag
                :type="v.isCombo ? 'warning' : v.discountType === 'PERCENT' ? 'primary' : 'success'"
                effect="dark"
                style="font-size: 15px; font-weight: 800; height: 48px; padding: 0 12px; display: flex; align-items: center;"
              >
                <span v-if="!v.isCombo">{{ v.discountType === 'PERCENT' ? v.discountValue : formatShort(v.discountValue) }}{{ v.discountType === 'PERCENT' ? '%' : '₫' }}</span>
                <span v-else>{{ v.buyQty }}+{{ v.getQty }}</span>
              </el-tag>
            </el-col>
            <el-col flex="1" style="min-width: 0;">
              <el-row align="middle" justify="space-between">
                <el-text tag="b">{{ v.code }}</el-text>
                <el-icon v-if="tempSelectedId === v.id" style="color: var(--el-color-primary);"><CircleCheck /></el-icon>
              </el-row>
              <el-text tag="div" size="small" style="margin: 2px 0;">{{ v.name }}</el-text>
              <el-space :size="6" style="flex-wrap: wrap;">
                <el-text size="small" type="info">
                  {{ Number(v.minOrderAmount) > 0 ? `Đơn từ ${formatMoney(v.minOrderAmount)}` : '' }}
                </el-text>
                <el-text size="small" type="info">· HSD: {{ formatDate(v.expiresAt) }}</el-text>
              </el-space>
              <el-text v-if="!isVoucherApplicable(v)" type="warning" size="small" tag="div" style="margin-top: 4px;">
                Cần thêm {{ formatMoney(Number(v.minOrderAmount) - subtotal) }} nữa
              </el-text>
            </el-col>
          </el-row>
        </el-card>
      </el-space>
    </el-scrollbar>

    <template #footer>
      <el-row justify="space-between">
        <el-button plain @click="clearAndClose">
          <el-icon><Close /></el-icon> Không dùng mã
        </el-button>
        <el-button
          type="primary"
          :loading="voucherLoading"
          :disabled="!tempSelectedId"
          @click="applySelectedVoucher"
        >
          Áp dụng
          <el-text v-if="tempSelectedId && tempPreview" size="small" style="margin-left: 6px; color: inherit; opacity: 0.85;">
            − {{ formatMoney(tempPreview) }}
          </el-text>
        </el-button>
      </el-row>
    </template>
  </el-dialog>

  <el-dialog v-model="addressModalOpen" title="📍 Địa chỉ giao hàng" width="480px" align-center>
    <el-input
      v-model="tempAddress"
      type="textarea"
      :rows="3"
      placeholder="Nhập địa chỉ giao hàng của bạn (Số nhà, Đường, Phường/Xã, Quận/Huyện, Tỉnh/Thành phố)"
    />
    <template #footer>
      <div style="text-align: right;">
        <el-button @click="addressModalOpen = false">Hủy</el-button>
        <el-button type="primary" @click="confirmAddress" :disabled="!tempAddress.trim()">
          Xác nhận
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import {
  ArrowRight, CircleCheck, Close, CreditCard, Document,
  List, Loading, Monitor, Right, ShoppingCart, Ticket, Van, Location, Picture 
} from "@element-plus/icons-vue";
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { ordersApi } from "../../api/orders.api";
import { customersApi } from "../../api/customers.api";
import { cartApi } from "../../api/cart.api";
import { promotionsApi } from "../../api/promotions.api";
import { useCartStore } from "../../stores/cart";
import { toast } from "../../ui/toast";

const router = useRouter();
const cartStore = useCartStore();

const loading = ref(false);
const alert = ref("");

const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

function fixImageUrl(url) {
  if (!url) return "";
  if (url.startsWith("http")) return url;
  return `${BASE_URL}${url}`;
}

const form = reactive({
  customerId: null,
  paymentMethod: "CASH",
  channel: "OFFLINE",
  notes: "",
  items: [],
  shippingAddress: "", // <-- Đặt nó nằm chung nhà với tụi kia thế này thôi
});

const voucherModalOpen = ref(false);
const voucherLoading = ref(false);
const availableVouchers = ref([]);
const selectedVoucher = ref(null);
const tempSelectedId = ref(null);
const promoResult = ref(null);
const promoError = ref("");

const ALL_PAYMENT_OPTIONS = [
  { value: "CASH", label: "Tiền mặt", icon: "💵" },
  { value: "TRANSFER", label: "Chuyển khoản", icon: "🏦" },
];

const paymentOptions = computed(() => ALL_PAYMENT_OPTIONS);

const subtotal = computed(() =>
  form.items.reduce((sum, i) => {
    const price = i.price ?? i.unitPrice ?? 0;
    return sum + Number(price) * (i.quantity ?? 1);
  }, 0),
);

const promoDiscount = computed(() =>
  selectedVoucher.value && promoResult.value
    ? Number(promoResult.value.discountAmount ?? 0)
    : 0,
);

// ✅ FIX: Phí vận chuyển = 0 (bỏ phí ship toàn hệ thống)
const shippingFee = computed(() => 0);

const totalAfterDiscount = computed(() =>
  Math.max(0, subtotal.value - promoDiscount.value),
);

const tempPreview = computed(() => {
  if (!tempSelectedId.value) return null;
  const v = availableVouchers.value.find((x) => x.id === tempSelectedId.value);
  if (!v || v.isCombo) return null;
  if (v.discountType === "PERCENT")
    return Math.min(subtotal.value, Math.round((subtotal.value * Number(v.discountValue)) / 100));
  return Math.min(subtotal.value, Number(v.discountValue));
});

// Thêm biến cho Popup Địa chỉ
const addressModalOpen = ref(false);
const tempAddress = ref("");

function openAddressModal() {
  tempAddress.value = form.shippingAddress; // Lấy địa chỉ hiện tại vào ô nhập
  addressModalOpen.value = true;
}

function confirmAddress() {
  form.shippingAddress = tempAddress.value;
  addressModalOpen.value = false;
}

function isVoucherApplicable(v) {
  const min = Number(v.minOrderAmount ?? 0);
  return !(min > 0 && subtotal.value < min);
}

function selectVoucher(id) {
  tempSelectedId.value = tempSelectedId.value === id ? null : id;
}

async function loadAvailableVouchers() {
  voucherLoading.value = true;
  try {
    const res = await ordersApi.suggestVouchers(form.customerId, subtotal.value);
    availableVouchers.value = res?.data?.data ?? res?.data ?? [];
  } catch {
    availableVouchers.value = [];
  } finally {
    voucherLoading.value = false;
  }
}
watch(subtotal, () => {
  if (voucherModalOpen.value) loadAvailableVouchers();
});

function openVoucherModal() {
  tempSelectedId.value = selectedVoucher.value?.id ?? null;
  voucherModalOpen.value = true;
  loadAvailableVouchers();
}

function clearAndClose() {
  selectedVoucher.value = null;
  promoResult.value = null;
  promoError.value = "";
  tempSelectedId.value = null;
  voucherModalOpen.value = false;
}

async function applySelectedVoucher() {
  if (!tempSelectedId.value) return;
  const voucher = availableVouchers.value.find((v) => v.id === tempSelectedId.value);
  if (!voucher) return;
  promoError.value = "";
  voucherLoading.value = true;
  try {
    const res = await ordersApi.validatePromoCode(voucher.code, subtotal.value);
    const data = res?.data?.data ?? res?.data ?? res;
    if (data?.valid) {
      selectedVoucher.value = voucher;
      promoResult.value = data;
      promoError.value = "";
      voucherModalOpen.value = false;
      toast(`✅ Áp dụng mã "${voucher.code}" — giảm ${formatMoney(data.discountAmount)}`, "success");
    } else {
      promoError.value = data?.message || "Mã không hợp lệ";
      selectedVoucher.value = null;
      promoResult.value = null;
      voucherModalOpen.value = false;
    }
  } catch (e) {
    promoError.value = e?.response?.data?.message || "Không thể kiểm tra mã";
    selectedVoucher.value = null;
    promoResult.value = null;
    voucherModalOpen.value = false;
  } finally {
    voucherLoading.value = false;
  }
}

function buildNotes() {
  const parts = [];
  if (form.notes?.trim()) parts.push(form.notes.trim());
  if (form.paymentMethod === "TRANSFER") parts.push("Thanh toán chuyển khoản");
  parts.push(form.channel === "ONLINE" ? "Giao tại nhà" : "Nhận tại cửa hàng");
  return parts.join(" | ");
}

function pickOrderId(payload) {
  const root = payload?.data ?? payload;
  return root?.id ?? root?.orderId ?? root?.data?.id ?? root?.data?.orderId ?? null;
}

async function submit() {
  if (form.channel === "ONLINE" && (!form.shippingAddress || form.shippingAddress.trim() === "")) {
    toast("Vui lòng nhập địa chỉ giao hàng để tiếp tục!", "warning");
    return;
  }
  alert.value = "";
  loading.value = true;
  try {
    const payload = {
      customerId: Number(form.customerId),
      paymentMethod: form.paymentMethod,
      channel: form.channel,
      notes: buildNotes(),
      items: form.items,
      shippingFee: 0, // ✅ FIX: luôn gửi 0
      shippingAddress: form.channel === "ONLINE" ? form.shippingAddress : "",
    };
    if (selectedVoucher.value) payload.promotionCode = selectedVoucher.value.code;
    const res = await ordersApi.create(payload);
    const orderId = pickOrderId(res?.data);
    toast("Đặt hàng thành công! 🎉", "success");
    await cartStore.clearCart();
    if (orderId) router.push(`/orders/${orderId}`);
    else toast("Không tìm thấy mã đơn hàng", "warning");
  } catch (e) {
    const msg = e?.response?.data?.message || e?.message || "";
    alert.value = msg || "Đặt hàng thất bại";
  } finally {
    loading.value = false;
  }
}

function formatShort(val) {
  const n = Number(val);
  if (n >= 1000000) return Math.round(n / 1000000) + "M";
  if (n >= 1000) return Math.round(n / 1000) + "K";
  return n;
}

function formatDate(iso) {
  if (!iso) return "—";
  try { return new Date(iso).toLocaleDateString("vi-VN"); }
  catch { return iso; }
}

function formatMoney(value) {
  if (value == null) return "—";
  return Number(value).toLocaleString("vi-VN") + " ₫";
}

onMounted(async () => {
  try {
    const profile = await customersApi.getProfile();
    const custData = profile?.data?.data ?? profile?.data;
    form.customerId = custData?.id;
    form.shippingAddress = custData?.address || ""; // <-- Lấy địa chỉ từ DB đổ ra
    
    const cart = await cartApi.getItems();
    form.items = cart?.data ?? [];
  } catch {
    alert.value = "Không thể tải dữ liệu đơn hàng";
  }
  loadAvailableVouchers();
});
</script>

<style scoped>
.checkout-body {
  max-width: 1140px;
  margin: 0 auto;
  padding: 28px 24px 64px;
  display: grid;
  grid-template-columns: 1fr 396px;
  gap: 20px;
  align-items: start;
}

@media (max-width: 860px) {
  .checkout-body { grid-template-columns: 1fr; }
}

.item-img-wrap {
  width: 44px;
  height: 44px;
  border-radius: 6px;
  background: var(--el-fill-color-lighter);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px solid var(--el-border-color-extra-light);
  flex-shrink: 0;
}
.item-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>