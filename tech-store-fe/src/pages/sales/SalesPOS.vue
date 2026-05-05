<template>
  <el-container class="pos-container">
    <!-- ══════ LEFT: Search & Results (Grey Background) ══════ -->
    <el-main class="pos-main">
      <div class="main-content-wrapper">
        <!-- Compact Search Bar -->
        <el-row :gutter="10" class="mb-15">
          <el-col :span="24">
            <el-input
              ref="searchInput"
              v-model="searchQuery"
              placeholder="Quét hoặc nhập mã serial... (F2)"
              @input="onSearchInput"
              @keyup.enter="doSearch"
              clearable
              size="large"
            >
              <template #prefix><el-icon><Monitor /></el-icon></template>
              <template #append>
                <el-button @click="doSearch" :loading="searchLoading" type="primary">TÌM KIẾM</el-button>
              </template>
            </el-input>
          </el-col>
        </el-row>

        <!-- Status Messages -->
        <el-alert v-if="searchError" :title="searchError" type="error" show-icon :closable="false" class="mb-15" />

        <div v-if="searchLoading" class="loading-box">
          <el-icon class="is-loading" :size="24"><Loading /></el-icon>
          <div class="mt-10"><el-text color="#909399">Đang tra cứu kho hàng...</el-text></div>
        </div>

        <!-- Results List -->
        <div v-else-if="productGroups.length">
          <div v-for="product in productGroups" :key="product.name" class="product-result-card mb-15">
            <!-- Card header -->
            <div class="prc-header">
              <div class="prc-name">{{ product.name }}</div>
              <el-tag v-if="product.id" size="small" type="info" effect="plain">ID: {{ product.id }}</el-tag>
            </div>

            <!-- Variants -->
            <div v-for="variant in product.variants" :key="variant.name" class="variant-block">
              <!-- Variant meta row -->
              <div class="variant-meta">
                <div class="variant-left">
                  <span class="variant-name">{{ variant.name }}</span>
                  <span class="variant-price">{{ formatMoney(variant.price) }}</span>
                </div>
                <div class="variant-right">
                  <el-tag
                    v-if="variant.remainingCount > 0 && variant.remainingCount < 5"
                    type="danger" effect="dark" size="small" class="blink-tag"
                  >SẮP HẾT HÀNG</el-tag>
                  <el-tag
                    :type="variant.remainingCount > 0 ? 'success' : 'danger'"
                    effect="light" size="small"
                  >
                    {{ variant.remainingCount }} trong kho
                  </el-tag>
                </div>
              </div>

              <!-- Serial filter (always show when > 3 serials) -->
              <div v-if="variant.serials.length > 3" class="serial-filter-wrap">
                <el-input
                  v-model="variantSerialFilter[variant.id]"
                  :placeholder="`Tìm trong ${variant.serials.length} serial…`"
                  size="small"
                  clearable
                >
                  <template #prefix><el-icon><Search /></el-icon></template>
                </el-input>
                <span class="serial-filter-hint">
                  {{ filteredSerials(variant).length }} / {{ variant.serials.length }}
                </span>
              </div>

              <!-- Serial grid -->
              <div class="serial-grid-wrap">
                <button
                  v-for="s in filteredSerials(variant)"
                  :key="s.id"
                  class="serial-pill"
                  :class="{
                    'is-matched': isMatchedSerial(s) && !alreadyInCart(s.id),
                    'is-in-cart': alreadyInCart(s.id),
                  }"
                  :disabled="alreadyInCart(s.id)"
                  @click="addToCart(s, variant, product.name)"
                  :title="serialCode(s)"
                >
                  <span v-if="alreadyInCart(s.id)" class="pill-icon">✓</span>
                  <span v-else-if="isMatchedSerial(s)" class="pill-icon">&#9889;</span>
                  {{ serialCode(s) }}
                </button>
                <span v-if="filteredSerials(variant).length === 0" class="serial-empty">Không có serial khớp</span>
              </div>
            </div>
          </div>
        </div>

        <el-empty v-else-if="!searchLoading" :description="hasSearched ? 'Không tìm thấy máy nào khớp' : 'Sẵn sàng bán hàng (F2 để tìm)'" />
      </div>
    </el-main>

    <!-- ══════ RIGHT: Cart (White Background) ══════ -->
    <el-aside width="400px" class="pos-sidebar">
      <div class="sidebar-wrapper">
        <div class="sidebar-header">
          <el-row justify="space-between" align="middle" style="width: 100%">
            <el-space>
              <el-icon color="#409EFF"><ShoppingCart /></el-icon>
              <el-text tag="b">GIỎ HÀNG</el-text>
              <el-badge :value="cart.length" :hidden="!cart.length" type="primary" />
            </el-space>
            <el-button v-if="cart.length" type="danger" link @click="handleClearCart" :icon="Delete">Làm mới</el-button>
          </el-row>
        </div>

        <!-- Draft restore prompt -->
        <div v-if="showDraftPrompt" class="draft-prompt">
          <div class="draft-prompt-text">
            <el-icon><InfoFilled /></el-icon>
            Có đơn nháp ({{ draftCartPreview }}) từ lần trước
          </div>
          <div class="draft-prompt-actions">
            <el-button size="small" type="primary" plain @click="restoreDraft">Khôi phục</el-button>
            <el-button size="small" @click="dismissDraft">Bỏ qua</el-button>
          </div>
        </div>

        <!-- Cart list -->
        <div class="cart-list">
          <div v-if="!cart.length" class="cart-empty">
            <el-icon :size="32" color="#dcdfe6"><ShoppingCart /></el-icon>
            <span>Giỏ hàng trống</span>
          </div>
          <div v-for="item in cart" :key="item.serialId" class="cart-row">
            <div class="cart-row-info">
              <div class="cart-row-name">{{ item.productName }}</div>
              <div class="cart-row-sub">
                <span class="cart-row-variant">{{ item.variantName }}</span>
                <span class="cart-row-serial">{{ item.serialCode }}</span>
              </div>
            </div>
            <div class="cart-row-right">
              <div class="cart-row-price">{{ formatMoney(item.price) }}</div>
              <button class="cart-remove-btn" @click="removeFromCart(item.serialId)" title="Xóa">×</button>
            </div>
          </div>
        </div>

        <!-- Footer Section (Fixed Bottom) -->
        <div class="sidebar-footer">
          <div class="footer-block customer-section">
            <el-row :gutter="8">
              <el-col :span="18">
                <el-input v-model="cusQuery" placeholder="SĐT hoặc Email khách..." @keyup.enter="lookupCustomer" size="small" clearable>
                  <template #prefix><el-icon><User /></el-icon></template>
                </el-input>
              </el-col>
              <el-col :span="6">
                <el-button @click="lookupCustomer" :loading="cusLoading" size="small" style="width: 100%">Tìm</el-button>
              </el-col>
            </el-row>
            <div v-if="foundCustomer" class="selected-customer mt-5">
              <el-tag closable @close="removeCustomer" effect="light" class="w-full-tag custom-customer-tag">
                <div class="customer-info-box">
                  <span class="customer-name font-bold">{{ foundCustomer.fullName || foundCustomer.name }}</span>
                  <div class="customer-meta">
                    <el-text size="small" type="info">{{ foundCustomer.phone }}</el-text>
                    <el-tag v-if="customerTierName" size="small" effect="dark" type="warning" round class="tier-badge">{{ customerTierName }}</el-tag>
                  </div>
                </div>
              </el-tag>
            </div>
          </div>

          <!-- Mã giảm giá gợi ý -->
<div v-if="foundCustomer && showPromoList && !appliedPromo" class="footer-block promo-suggest-block">
  <div class="promo-suggest-header">
    <el-text size="extra-small" style="color: #7c3aed; font-weight: 500;">
      <el-icon><Present /></el-icon>
      Mã khả dụng ({{ availablePromos.length }})
    </el-text>
    <el-button link size="small" @click="showPromoList = false">
      <el-icon><Close /></el-icon>
    </el-button>
  </div>
  <div v-if="promosLoading" style="text-align:center; padding: 8px;">
    <el-icon class="is-loading"><Loading /></el-icon>
  </div>
  <div v-else class="promo-list-scroll">
    <div
      v-for="(promo, idx) in availablePromos"
      :key="promo.id"
      class="promo-chip"
      :class="{ 'is-best': idx === 0 }"
      @click="selectPromo(promo.code)"
    >
      <div style="display: flex; align-items: center; gap: 6px;">
        <span v-if="idx === 0" class="best-badge">TỐT NHẤT</span>
        <span class="pcode">{{ promo.code }}</span>
       <span class="pval">
  {{ promo.discountType === 'PERCENT'
      ? `-${promo.discountValue}%`
      : `-${formatMoney(promo.discountValue)}` }}
</span>
      </div>
      <div v-if="subtotal > 0 && previewDiscount(promo)" class="psave">
        Tiết kiệm ~{{ formatMoney(previewDiscount(promo)) }}
      </div>
      <div v-if="promo.endDate" class="pexp">
        HH: {{ new Date(promo.endDate).toLocaleDateString('vi-VN') }}
      </div>
    </div>
  </div>
</div>

          <!-- Promo info -->
          <div class="footer-block" v-if="cart.length">
            <el-row :gutter="8">
              <el-col :span="16">
                <el-input v-model="promoCode" placeholder="Mã giảm giá..." size="small" :disabled="!!appliedPromo" @keyup.enter="applyPromo" />
              </el-col>
              <el-col :span="8">
                <el-button v-if="!appliedPromo" @click="applyPromo" :loading="promoLoading" size="small" style="width:100%">Áp dụng</el-button>
                <el-button v-else @click="clearPromo" type="danger" link size="small">Bỏ mã</el-button>
              </el-col>
            </el-row>
            <el-text v-if="promoDiscount > 0" type="success" size="extra-small" class="mt-5 block">✓ Đã giảm {{ formatMoney(promoDiscount) }}</el-text>
          </div>

          <!-- Totals -->
          <div class="summary-details">
            <div class="summary-line">
              <span class="label">Tạm tính</span>
              <span class="value">{{ formatMoney(subtotal) }}</span>
            </div>

            <!-- VIP Discount -->
            <div v-if="vipDiscountPct > 0" class="summary-line discount-line vip-line">
              <span class="label">
                <el-icon style="vertical-align:middle;margin-right:3px"><Medal /></el-icon>
                VIP ({{ vipDiscountPct }}%)
              </span>
              <span class="value">-{{ formatMoney(vipDiscount) }}</span>
            </div>

            <!-- Spin Discount -->
            <div v-if="spinDiscountPct > 0" class="summary-line discount-line spin-line">
              <span class="label">
                <el-icon style="vertical-align:middle;margin-right:3px"><Present /></el-icon>
                Spin ({{ spinDiscountPct }}%)
                <el-tooltip v-if="spinBonusExpiry" :content="'Hết hạn: ' + new Date(spinBonusExpiry).toLocaleDateString('vi-VN')" placement="top">
                  <el-icon style="color:#e6a23c;margin-left:3px"><InfoFilled /></el-icon>
                </el-tooltip>
              </span>
              <span class="value">-{{ formatMoney(spinDiscount) }}</span>
            </div>

            <!-- Promo Code Discount -->
            <div v-if="promoDiscount > 0" class="summary-line discount-line">
              <span class="label">🏷️ Mã giảm giá</span>
              <span class="value">-{{ formatMoney(promoDiscount) }}</span>
            </div>

            <div class="summary-line total">
              <span class="label">TỔNG CỘNG</span>
              <span class="value">{{ formatMoney(totalAmount) }}</span>
            </div>
          </div>

          <el-button
            type="primary"
            size="large"
            class="pay-btn"
            :disabled="!cart.length || !foundCustomer"
            @click="openPaymentModal"
          >
            <el-icon class="mr-5"><Wallet /></el-icon> THANH TOÁN
          </el-button>
        </div>
      </div>
    </el-aside>

    <!-- ══════ Payment & Success Dialogs (Giữ nguyên logic cũ) ══════ -->
    <el-dialog v-model="showModal" title="Xác nhận thanh toán" width="450px" append-to-body>
      <el-space direction="vertical" fill style="width: 100%" :size="20">
        <div class="amount-card">
          <el-text size="small" type="info">SỐ TIỀN CẦN THU</el-text>
          <div class="price-big">{{ formatMoney(totalAmount) }}</div>
        </div>

        <el-radio-group v-model="payMethod" style="width: 100%" class="pay-method-group">
          <el-radio-button label="CASH">TIỀN MẶT</el-radio-button>
          <el-radio-button label="TRANSFER">CHUYỂN KHOẢN</el-radio-button>
        </el-radio-group>

        <div v-if="payMethod === 'CASH'">
          <el-text size="small" class="mb-5 block">Khách đưa:</el-text>
          <el-input-number v-model="cashIn" :min="0" style="width: 100%" :controls="false" size="large" />
          <el-row :gutter="5" class="mt-10">
            <el-col v-for="q in quickOptions" :key="q" :span="8">
              <el-button @click="cashIn = q" size="small" style="width: 100%">{{ formatShort(q) }}</el-button>
            </el-col>
          </el-row>
          <div class="change-box mt-15" v-if="cashIn >= totalAmount">
            <span>Tiền thừa:</span>
            <el-text type="success" tag="b" size="large">{{ formatMoney(cashIn - totalAmount) }}</el-text>
          </div>
        </div>

        <el-input v-model="orderNotes" type="textarea" placeholder="Ghi chú đơn hàng (nếu có)..." :rows="2" />
        <el-alert v-if="payError" :title="payError" type="error" show-icon :closable="false" />
        <el-text v-if="payLoading" type="primary" size="small" class="center block">{{ payStep }}</el-text>
      </el-space>

      <template #footer>
        <el-button @click="showModal = false" :disabled="payLoading">HỦY</el-button>
        <el-button type="primary" :loading="payLoading" @click="confirmPayment" :disabled="payMethod === 'CASH' && cashIn < totalAmount">XÁC NHẬN</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showDone" title="Hoàn tất" width="420px" :show-close="false" :close-on-click-modal="false">
      <el-result icon="success" title="Bán hàng thành công">
        <template #sub-title>
          <el-descriptions :column="1" border size="small" class="mt-10">
            <el-descriptions-item label="Số hóa đơn"><span class="mono">{{ orderDone?.orderNumber }}</span></el-descriptions-item>
            <el-descriptions-item label="Khách hàng">{{ foundCustomerSnapshot?.fullName || foundCustomerSnapshot?.name }}</el-descriptions-item>
            <el-descriptions-item label="Tổng tiền">{{ formatMoney(snapshotTotal) }}</el-descriptions-item>
          </el-descriptions>
        </template>
        <template #extra>
          <el-button type="primary" @click="resetAll">ĐƠN MỚI</el-button>
          <el-button @click="printReceipt" plain>IN HÓA ĐƠN</el-button>
        </template>
      </el-result>
    </el-dialog>
  </el-container>
  <ReceiptPrint
  ref="receiptRef"
  :order="orderDone"
  :customerTierName="customerTierName"
  :vipDiscountPct="vipDiscountPct"
  :vipDiscount="vipDiscount"
  :spinDiscountPct="spinDiscountPct"
  :spinDiscount="spinDiscount"
  :promoCode="appliedPromo?.code"
  :promoDiscount="promoDiscount"
  :payMethod="payMethod"
  :cashIn="cashIn"
/>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from "vue";
import { Monitor, Loading, ShoppingCart, Delete, Close, User, Wallet, InfoFilled, Medal, Present, Search } from "@element-plus/icons-vue";
import { productsApi } from "../../api/products.api";
import { serialsApi } from "../../api/serials.api";
import { promotionsApi } from "../../api/promotions.api";
import { ordersApi } from "../../api/orders.api";
import { paymentsApi } from "../../api/payments";
import { customersApi } from "../../api/customers.api";
import { spinWheelApi } from "../../api/spinWheel.api";
import { confirmModal } from "../../ui/confirm";
import { toast } from "../../ui/toast";
import ReceiptPrint from '../../components/Receiptprint.vue'

// ── Search & Cart ──
const searchInput = ref(null);
const searchQuery = ref("");
const searchLoading = ref(false);
const searchError = ref("");
const productGroups = ref([]);
const hasSearched = ref(false);
const variantSerialFilter = ref({});   // { [variantId]: filterString }
const searchedKeyword = ref('');        // keyword gốc để highlight serial khớp

const cart = ref([]);
const cusQuery = ref("");
const cusLoading = ref(false);
const cusError = ref("");
const foundCustomer = ref(null);
const orderNotes = ref("");
const hasDraft = ref(false);

// ── Promo & Payment ──
const promoCode = ref("");
const promoLoading = ref(false);
const promoError = ref("");
const appliedPromo = ref(null);
const promoDiscount = ref(0);

// ── VIP & Spin Discount ──
const vipDiscountPct = ref(0);   // % giảm theo hạng VIP (vd: 5 = 5%)
const spinDiscountPct = ref(0);  // % giảm từ spin wheel bonus (vd: 10 = 10%)
const spinBonusExpiry = ref(null); // Thời điểm hết hạn spin bonus
const customerTierName = ref(""); // Tên hạng VIP (ví dụ: Platinum, Diamond)
const discountsLoading = ref(false);

const showModal = ref(false);
const payMethod = ref("CASH");
const cashIn = ref(0);
const payLoading = ref(false);
const payError = ref("");
const payStep = ref("");
const availablePromos = ref([]);
const promosLoading = ref(false);
const showPromoList = ref(false);
const showDone = ref(false);
const orderDone = ref(null);
const snapshotTotal = ref(0);
const snapshotPayMethod = ref("");
const foundCustomerSnapshot = ref(null);
const receiptRef = ref(null)

// ── Computed ──
const subtotal = computed(() => cart.value.reduce((s, i) => s + i.price, 0));
const vipDiscount = computed(() => Math.round(subtotal.value * vipDiscountPct.value / 100));
const spinDiscount = computed(() => Math.round(subtotal.value * spinDiscountPct.value / 100));
const totalAmount = computed(() => Math.max(0, subtotal.value - vipDiscount.value - spinDiscount.value - promoDiscount.value));
const quickOptions = computed(() => {
  const t = totalAmount.value;
  const base = Math.ceil(t / 10000) * 10000;
  return [...new Set([base, base + 50000, base + 100000, base + 200000])].filter(v => v >= t);
});

function openPaymentModal() {
  cashIn.value = totalAmount.value;
  showModal.value = true;
}

// ── Helpers ──
const SOLD_STATUSES = ["sold", "used", "inactive", "disabled", "deleted", "reserved"];
const formatMoney = (v) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(v || 0);
const formatShort = (v) => v >= 1000000 ? (v / 1000000).toFixed(1) + "Tr" : (v / 1000).toFixed(0) + "K";
const serialCode = (s) => s.serialNumber ?? s.serialCode ?? s.code ?? "";

// ── Draft Order Logic ────────────────────────────────────
// Lưu có chọn lọc: chỉ lưu cart + customerQuery, không lưu full customer object
watch(cart, () => {
  if (cart.value.length > 0) {
    localStorage.setItem('pos_draft', JSON.stringify({
      cart:          cart.value,
      customerQuery: cusQuery.value,
      timestamp:     Date.now(),
    }));
  } else {
    localStorage.removeItem('pos_draft');
    showDraftPrompt.value = false;
  }
}, { deep: true });

function loadDraft() {
  try {
    const raw = localStorage.getItem('pos_draft');
    if (!raw) return;
    const draft = JSON.parse(raw);
    // Chỉ khôi phục nếu dưới 4 tiếng
    if (Date.now() - draft.timestamp > 4 * 60 * 60 * 1000) {
      localStorage.removeItem('pos_draft');
      return;
    }
    if (draft.cart?.length) {
      const count   = draft.cart.length;
      const preview = draft.cart.slice(0, 2).map(i => i.productName).join(', ');
      draftCartPreview.value = `${count} sản phẩm: ${preview}${count > 2 ? '…' : ''}`;
      showDraftPrompt.value = true;
    }
  } catch { localStorage.removeItem('pos_draft'); }
}

function restoreDraft() {
  try {
    const draft = JSON.parse(localStorage.getItem('pos_draft') || '{}');
    if (draft.cart?.length) {
      cart.value = draft.cart;
      if (draft.customerQuery) cusQuery.value = draft.customerQuery;
      toast(`Đã khôi phục đơn nháp (${draft.cart.length} sản phẩm)`, 'success');
    }
  } catch { /* ignore */ }
  showDraftPrompt.value = false;
}

function dismissDraft() {
  localStorage.removeItem('pos_draft');
  showDraftPrompt.value = false;
}

// ── Business Logic (Price + Search) ──
let searchTimer = null;
function onSearchInput() {
  clearTimeout(searchTimer);
  if (!searchQuery.value.trim()) { productGroups.value = []; hasSearched.value = false; return; }
  searchTimer = setTimeout(() => doSearch(), 400);
}

async function doSearch() {
  const q = searchQuery.value.trim();
  if (!q) return;
  hasSearched.value = true;
  searchLoading.value = true;
  searchError.value = "";
  productGroups.value = [];
  variantSerialFilter.value = {};
  searchedKeyword.value = q;
  try {
    const res = await serialsApi.list({ keyword: q, size: 50 });
    const rawData = res.data?.data ?? res.data;
    const items = Array.isArray(rawData?.content) ? rawData.content : (Array.isArray(rawData) ? rawData : []);
    
    if (!items.length) { searchError.value = `Không tìm thấy serial "${q}".`; return; }

    const available = items.filter(s => !SOLD_STATUSES.includes((s.status || "").toLowerCase()));
    if (!available.length) { searchError.value = `Serial "${q}" đã bán hoặc không khả dụng.`; return; }

    const groupMap = {};
    for (const serial of available) {
      const pName = serial.productName || "Sản phẩm";
      const vName = serial.variantName || "Mặc định";
      if (!groupMap[pName]) groupMap[pName] = { name: pName, variants: {} };
      if (!groupMap[pName].variants[vName]) {
        const vId = serial.variantId || serial.productVariantId || serial.variant?.id || serial.productVariant?.id;
        groupMap[pName].variants[vName] = { id: vId, name: vName, price: 0, remainingCount: 0, serials: [], matchedSerialIds: new Set() };
      }
      // Đánh dấu serial này là matched (từ tìm kiếm)
      groupMap[pName].variants[vName].matchedSerialIds.add(serial.id);
    }

    const groups = Object.values(groupMap).map(g => ({ ...g, variants: Object.values(g.variants) }));
    productGroups.value = groups;

    // Fetch giá + toàn bộ serial IN_STOCK thực tế của từng variant
    await Promise.all(groups.map(async (group) => {
      try {
        const pRes = await productsApi.list({ keyword: group.name, size: 5 });
        const pData = pRes.data?.data ?? pRes.data;
        const pList = Array.isArray(pData?.content) ? pData.content : (Array.isArray(pData) ? pData : []);
        const product = pList.find(p => p.name === group.name);
        
        if (product?.id) {
          group.id = product.id;
          const vRes = await productsApi.getVariants(product.id);
          const vList = vRes.data?.data || vRes.data || [];

          await Promise.all(group.variants.map(async (v) => {
            const match = vList.find(vo => vo.id === v.id || vo.variantName === v.name || vo.name === v.name);
            if (match) {
              if (!v.id) v.id = match.id;
              if (match.price) v.price = Number(match.price);
            }

            // Load toàn bộ serial IN_STOCK của variant này
            if (v.id) {
              try {
                const sRes = await productsApi.getSerials(v.id);
                const allSerials = sRes.data?.data ?? sRes.data ?? [];
                const inStock = allSerials.filter(s => !SOLD_STATUSES.includes((s.status || '').toLowerCase()) && s.status?.toUpperCase() !== 'SOLD');
                // Sắp xếp: matched serial lên đầu
                inStock.sort((a, b) => (v.matchedSerialIds.has(b.id) ? 1 : 0) - (v.matchedSerialIds.has(a.id) ? 1 : 0));
                v.serials = inStock;
                v.remainingCount = inStock.length;
              } catch { /* giữ nguyên matched serials nếu lỗi */ }
            }
          }));
        }
      } catch (err) { console.warn("Bỏ qua lỗi lấy giá cho:", group.name); }
    }));
  } catch (e) { 
    console.error(e);
    searchError.value = "Lỗi kết nối máy chủ khi tra cứu."; 
  } finally { searchLoading.value = false; }
}


// ── Actions ──
const alreadyInCart = (sid) => cart.value.some(i => i.serialId === sid);
const isMatchedSerial = (s) => s.serialNumber?.toLowerCase().includes(searchedKeyword.value.toLowerCase());
const filteredSerials = (variant) => {
  const f = (variantSerialFilter.value[variant.id] || '').toLowerCase();
  if (!f) return variant.serials;
  return variant.serials.filter(s => serialCode(s).toLowerCase().includes(f));
};

function addToCart(serial, variant, productName) {
  const sid = serial.id;
  if (alreadyInCart(sid)) return toast("Serial đã có trong đơn", "warning");
  cart.value.push({ serialId: sid, serialCode: serialCode(serial), variantId: variant.id, productName, variantName: variant.name, price: variant.price });
  toast(`Đã thêm ${serialCode(serial)}`, "success");
}

const removeFromCart = (sid) => cart.value = cart.value.filter(i => i.serialId !== sid);
async function handleClearCart() { if (await confirmModal("Xóa toàn bộ giỏ hàng?", "Làm mới", "Xóa", true)) resetAll(); }

async function lookupCustomer() {
  const q = cusQuery.value.trim().toLowerCase();
  if (!q) return;
  cusLoading.value = true; cusError.value = "";
  try {
    const res = await customersApi.listAll();
    const list = res.data?.data || res.data || [];
    
    // Logic so khớp chính xác (Strict Match)
    const found = list.find(c => {
      // 1. So khớp SĐT (Chuẩn hoá về chỉ gồm số)
      const qPhone = q.replace(/\D/g, "");
      const cPhone = (c.phone || "").replace(/\D/g, "");
      if (qPhone && (cPhone === qPhone || cPhone.endsWith(qPhone) && qPhone.length >= 9)) return true;

      // 2. So khớp Email chính xác
      if (q.includes("@") && (c.email || "").toLowerCase() === q) return true;

      // 3. So khớp Tên (Chỉ allow nếu nhập chuỗi đủ dài và khớp chính xác hoặc bắt đầu bằng)
      if (q.length >= 3) {
        const cName = (c.fullName || c.name || "").toLowerCase();
        return cName === q || cName.startsWith(q + " ");
      }

      return false;
    });

    if (found) {
      foundCustomer.value = found;
      toast(`Chào khách hàng: ${found.fullName || found.name}`, "success");
      fetchCustomerDiscounts(found.id);
      fetchAvailablePromos(found);
    } else {
      cusError.value = "Không tìm thấy khách hàng. Vui lòng nhập đầy đủ SĐT hoặc Email.";
    }
  } catch (err) {
    cusError.value = "Lỗi hệ thống khi tra cứu khách hàng.";
  } finally { cusLoading.value = false; }
}

/**
 * Lấy % giảm VIP (tier discount) và % spin bonus của khách vừa tìm thấy.
 */
async function fetchCustomerDiscounts(customerId) {
  discountsLoading.value = true;
  vipDiscountPct.value = 0;
  spinDiscountPct.value = 0;
  spinBonusExpiry.value = null;
  try {
    // 1. VIP tier discount
    const tierRes = await customersApi.getCustomerTierProgress(customerId);
    const tierData = tierRes.data?.data ?? tierRes.data ?? {};
    // Backend có thể trả `currentDiscountRate` là decimal (0.03)
    const rate = Number(tierData.currentDiscountRate ?? tierData.discountPercentage ?? tierData.discount ?? tierData.vipDiscountPct ?? 0);
    vipDiscountPct.value = (rate > 0 && rate < 1) ? rate * 100 : rate;
    customerTierName.value = tierData.currentTierDisplay || tierData.currentTier || "";

    // 2. Spin wheel bonus
    const spinRes = await spinWheelApi.getStatus(customerId);
    const spinData = spinRes.data?.data ?? spinRes.data ?? {};
    
    // Dựa vào currentBonus do BackEnd tính toán. Nếu = 0 tức là đã dùng hoặc hết hạn.
    const activeBonus = Number(spinData.currentBonus ?? spinData.bonusPercent ?? spinData.bonus ?? 0);
    
    if (activeBonus > 0) {
      spinDiscountPct.value = activeBonus;
      // Trích xuất thêm ngày hết hạn từ lastSpin (nếu khớp)
      if (spinData.lastSpin && Number(spinData.lastSpin.discountBonus) === activeBonus) {
        spinBonusExpiry.value = spinData.lastSpin.expiresAt;
      } else {
        spinBonusExpiry.value = spinData.bonusExpiresAt ?? spinData.expiresAt ?? null;
      }
    } else {
      spinDiscountPct.value = 0;
      spinBonusExpiry.value = null;
    }

    if (vipDiscountPct.value > 0 || spinDiscountPct.value > 0) {
      toast(
        `Áp dụng: VIP ${vipDiscountPct.value}% + Spin ${spinDiscountPct.value}%`,
        "success"
      );
    }
  } catch (err) {
    console.warn("Không lấy được thông tin ưu đãi:", err);
  } finally {
    discountsLoading.value = false;
  }
}

const removeCustomer = () => {
  foundCustomer.value = null;
  cusQuery.value = "";
  vipDiscountPct.value = 0;
  spinDiscountPct.value = 0;
  spinBonusExpiry.value = null;
  customerTierName.value = "";
  availablePromos.value = []; 
  showPromoList.value = false; 
};

async function applyPromo() {
  const code = promoCode.value.trim().toUpperCase();
  if (!code) return;
  promoLoading.value = true;
  try {
    const res = await ordersApi.validatePromoCode(code, subtotal.value);
    const data = res.data?.data ?? res.data;

    if (data?.valid) {
      appliedPromo.value = { code };

      // Thử nhiều tên field vì backend có thể dùng tên khác nhau
      const rawDiscount =
        data.discount        ??
        data.discountAmount  ??
        data.discountValue   ??
        data.amount          ??
        0;

      // Phân biệt: tỉ lệ decimal (0.1 = 10%), % nguyên có flag, hay số tiền tuyệt đối
      if (rawDiscount > 0 && rawDiscount <= 1) {
        promoDiscount.value = Math.round(subtotal.value * rawDiscount);
      } else if (rawDiscount > 1 && rawDiscount <= 100 && data.isPercentage) {
        promoDiscount.value = Math.round(subtotal.value * rawDiscount / 100);
      } else {
        promoDiscount.value = Number(rawDiscount);
      }

      toast(`Áp dụng mã giảm: -${formatMoney(promoDiscount.value)}`, "success");
    } else {
      promoError.value = data?.message || "Mã giảm giá không hợp lệ.";
    }
  } catch (e) {
    promoError.value = e.response?.data?.message || "Không thể xác thực mã giảm giá.";
  } finally { promoLoading.value = false; }
}
const clearPromo = () => { appliedPromo.value = null; promoDiscount.value = 0; promoCode.value = ""; };

async function confirmPayment() {
  payLoading.value = true; payError.value = "";
  try {
    payStep.value = "Đang tạo đơn...";
    const oRes = await ordersApi.create({
      customerId: foundCustomer.value?.id,
      paymentMethod: payMethod.value,
      channel: "OFFLINE",
      notes: orderNotes.value,
      promotionCode: appliedPromo.value?.code,
      // Truyền thêm discount VIP và Spin để backend ghi nhận
      vipDiscountPercent: vipDiscountPct.value || undefined,
      vipDiscountAmount: vipDiscount.value || undefined,
      spinDiscountPercent: spinDiscountPct.value || undefined,
      spinDiscountAmount: spinDiscount.value || undefined,
      items: cart.value.map(i => ({ variantId: i.variantId, serialId: i.serialId, quantity: 1 }))
    });
    const respData = oRes.data?.data || oRes.data || {};
    const orderId = respData.orderId || respData.id || respData.order_id || respData;

    if (!orderId || isNaN(Number(orderId))) {
      throw new Error(`Lỗi phân tích ID đơn hàng từ hệ thống: ${JSON.stringify(respData).substring(0, 50)}`);
    }

    payStep.value = "Ghi nhận thanh toán...";
    await paymentsApi.create({ 
      orderId: Number(orderId), 
      method: payMethod.value, 
      amount: totalAmount.value, 
      paymentStatus: "PAID",
      transactionRef: `TXN-POS-${Date.now()}`
    });

    payStep.value = "Cập nhật trạng thái giao hàng...";
    await ordersApi.markAsProcessing(orderId).catch(() => {});
    await ordersApi.markAsShipping(orderId).catch(() => {});
    await ordersApi.markAsDelivered(orderId);

    payStep.value = "Cập nhật kho...";
    await Promise.allSettled(cart.value.map(i => serialsApi.updateStatus(i.serialId, "SOLD")));

    snapshotTotal.value = totalAmount.value;
    foundCustomerSnapshot.value = foundCustomer.value;
    orderDone.value = oRes.data?.data || oRes.data;
    showModal.value = false; showDone.value = true;
    toast("Thanh toán thành công!", "success");
    resetAll();
  } catch (e) { payError.value = e.response?.data?.message || "Lỗi thanh toán."; }
  finally { payLoading.value = false; }
}

function resetAll() {
  cart.value = []; foundCustomer.value = null; cusQuery.value = "";
  orderNotes.value = ""; searchQuery.value = ""; clearPromo();
  vipDiscountPct.value = 0; spinDiscountPct.value = 0; spinBonusExpiry.value = null; customerTierName.value = "";
  localStorage.removeItem('pos_draft');
  showDraftPrompt.value = false;
}
async function fetchAvailablePromos(customer) {
  promosLoading.value = true;
  availablePromos.value = [];
  try {
    const res = await promotionsApi.getAvailableForCustomer(customer.id); // ← đổi dòng này
    const list = Array.isArray(res.data?.data) ? res.data.data : (res.data ?? []);

    availablePromos.value = list.sort((a, b) => {
      const est = (p) => p.discountType === 'PERCENT'
        ? (subtotal.value || 10_000_000) * p.discountValue / 100
        : Number(p.discountValue ?? 0);
      return est(b) - est(a);
    });

    showPromoList.value = availablePromos.value.length > 0;
  } catch (e) {
    console.warn("Không lấy được mã giảm giá:", e);
  } finally {
    promosLoading.value = false;
  }
}

function selectPromo(code) {
  if (appliedPromo.value) return;
  promoCode.value = code;
  applyPromo();
}

function previewDiscount(promo) {
  if (!subtotal.value) return null;
  if (promo.discountType === 'PERCENT')  // ← đổi PERCENTAGE → PERCENT
    return Math.round(subtotal.value * promo.discountValue / 100);
  return Number(promo.discountValue ?? 0);  // AMOUNT → trả thẳng số tiền
}

function printReceipt() {
  receiptRef.value?.handlePrint()
}
const onKey = (e) => { if (e.key === "F2") { e.preventDefault(); searchInput.value?.focus(); } };
onMounted(() => { window.addEventListener("keydown", onKey); loadDraft(); });
onUnmounted(() => window.removeEventListener("keydown", onKey));
</script>

<style scoped>
.pos-container { height: 100%; min-height: 0; background: #f0f2f5; overflow: hidden; display: flex; }
.pos-main { padding: 20px; overflow-y: auto; flex: 1; min-height: 0; }
.pos-sidebar { height: 100%; background: #fff; border-left: 1px solid #dcdfe6; position: relative; flex-shrink: 0; }
.main-content-wrapper { max-width: 1000px; margin: 0 auto; }
.sidebar-wrapper { display: flex; flex-direction: column; height: 100%; }
.sidebar-header { padding: 12px 15px; border-bottom: 1px solid #f0f0f0; background: #fafafa; }
.cart-container { flex: 1; overflow-y: auto; padding: 0; }

/* ── Draft prompt ──────────────────────────────── */
.draft-prompt {
  margin: 10px 12px 0;
  padding: 10px 12px;
  background: #ecf5ff;
  border: 1px solid #b3d8ff;
  border-radius: 6px;
  font-size: 12px;
  color: #409eff;
}
.draft-prompt-text {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}
.draft-prompt-actions { display: flex; gap: 6px; }

/* ── Cart list ────────────────────────────────── */
.cart-list { flex: 1; overflow-y: auto; padding: 8px 0; }
.cart-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 40px 0;
  color: #c0c4cc;
  font-size: 13px;
}
.cart-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px;
  border-bottom: 1px solid #f5f7fa;
  gap: 8px;
  transition: background 0.1s;
}
.cart-row:hover { background: #fafafa; }
.cart-row:last-child { border-bottom: none; }
.cart-row-info { flex: 1; min-width: 0; }
.cart-row-name {
  font-size: 12.5px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.cart-row-sub {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
}
.cart-row-variant {
  font-size: 11px;
  color: #909399;
}
.cart-row-serial {
  font-family: 'Courier New', monospace;
  font-size: 11px;
  color: #e6a23c;
  font-weight: 600;
}
.cart-row-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.cart-row-price {
  font-size: 12.5px;
  font-weight: 700;
  color: #303133;
  white-space: nowrap;
}
.cart-remove-btn {
  width: 20px;
  height: 20px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: #c0c4cc;
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.12s, color 0.12s;
  padding: 0;
}
.cart-remove-btn:hover { background: #fef0f0; color: #f56c6c; }
.sidebar-footer { padding: 15px; border-top: 1px solid #ebeef5; background: #fff; box-shadow: 0 -4px 10px rgba(0,0,0,0.03); }

/* ── Product result card ─────────────────────────── */
.product-result-card {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 6px rgba(0,0,0,0.07);
  overflow: hidden;
}
.prc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: linear-gradient(90deg, #f0f7ff 0%, #fff 100%);
  border-bottom: 1px solid #e8f0fe;
}
.prc-name {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a2e;
  letter-spacing: -0.01em;
}

/* ── Variant block ───────────────────────────────── */
.variant-block {
  padding: 14px 16px;
  border-bottom: 1px solid #f2f3f5;
}
.variant-block:last-child { border-bottom: none; }

.variant-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  gap: 8px;
}
.variant-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.variant-name {
  font-size: 12px;
  font-weight: 600;
  color: #606266;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.variant-price {
  font-size: 17px;
  font-weight: 800;
  color: #f56c6c;
  letter-spacing: -0.02em;
}
.variant-right {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

/* ── Serial filter row ───────────────────────────── */
.serial-filter-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.serial-filter-wrap .el-input { flex: 1; }
.serial-filter-hint {
  font-size: 11px;
  color: #909399;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

/* ── Serial grid ─────────────────────────────────── */
.serial-grid-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 180px;
  overflow-y: auto;
  padding: 2px 0;
}
.serial-grid-wrap::-webkit-scrollbar { width: 4px; }
.serial-grid-wrap::-webkit-scrollbar-thumb { background: #dcdfe6; border-radius: 4px; }

.serial-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border-radius: 4px;
  font-family: 'JetBrains Mono', 'Courier New', monospace;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid #dcdfe6;
  background: #fafafa;
  color: #303133;
  transition: background 0.12s, border-color 0.12s, color 0.12s;
  user-select: none;
  white-space: nowrap;
}
.serial-pill:hover:not(:disabled) {
  border-color: #409eff;
  background: #ecf5ff;
  color: #409eff;
}
.serial-pill.is-matched {
  border-color: #e6a23c;
  background: #fdf6ec;
  color: #b76e00;
}
.serial-pill.is-matched:hover:not(:disabled) {
  background: #faecd8;
  border-color: #c87d00;
}
.serial-pill.is-in-cart {
  border-color: #67c23a;
  background: #f0f9eb;
  color: #67c23a;
  cursor: default;
  opacity: 0.85;
}
.serial-pill:disabled { cursor: not-allowed; }
.pill-icon { font-size: 10px; }
.serial-empty { font-size: 12px; color: #909399; font-style: italic; align-self: center; }

/* Compact Styles */
.compact-table :deep(.el-table__cell) { padding: 4px 0; }
.cart-item-info { display: flex; flex-direction: column; }
.cart-item-price { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; }

/* Zoning & Aesthetics */
.card-header-compact { display: flex; justify-content: space-between; align-items: center; }
.amount-card { background: #f0f7ff; padding: 15px; border-radius: 8px; text-align: center; border: 1px solid #d9ecff; }
.price-big { font-size: 28px; font-weight: bold; color: #409eff; margin-top: 5px; }
.summary-details { margin: 15px 0; border-top: 1px dashed #dcdfe6; padding-top: 10px; }
.summary-line { display: flex; justify-content: space-between; margin-bottom: 6px; font-size: 13px; color: #606266; }
.summary-line.total { margin-top: 10px; padding-top: 10px; border-top: 1px solid #ebeef5; }
.summary-line.total .value { font-size: 20px; color: #f56c6c; font-weight: bold; }
.discount-line .value { color: #67c23a; font-weight: 600; }
.vip-line .label { color: #8b5cf6; font-weight: 600; }
.spin-line .label { color: #f59e0b; font-weight: 600; }
.pay-btn { width: 100%; height: 50px; font-weight: bold; font-size: 16px; margin-top: 10px; }

/* Effects */
.blink-tag { animation: blink 1s infinite; }
@keyframes blink { 0% { opacity: 1; } 50% { opacity: 0.5; } 100% { opacity: 1; } }
.draft-tip { margin-bottom: 8px; text-align: center; }

.custom-customer-tag { height: auto !important; padding: 6px 8px; }
.custom-customer-tag :deep(.el-tag__content) { flex: 1; width: 100%; }
.customer-info-box { display: flex; flex-direction: column; align-items: flex-start; line-height: 1.3; }
.customer-name { font-size: 13px; color: #303133; }
.customer-meta { display: flex; align-items: center; gap: 8px; margin-top: 4px; }
.tier-badge { height: 20px; font-weight: bold; font-size: 11px; padding: 0 6px; }

.w-full-tag { width: 100%; display: flex; justify-content: space-between; align-items: center; }
.mono { font-family: monospace; }
.flex-row { display: flex; align-items: center; }
.ml-10 { margin-left: 10px; }
.mt-5 { margin-top: 5px; }
.mt-10 { margin-top: 10px; }
.mt-15 { margin-top: 15px; }
.mb-5 { margin-bottom: 5px; }
.mb-15 { margin-bottom: 15px; }
.block { display: block; }
.center { text-align: center; }
.loading-box { display: flex; flex-direction: column; align-items: center; padding: 60px 0; }
.promo-suggest-block { background: #f5f0ff; border-radius: 8px; padding: 10px; margin-bottom: 8px; }
.promo-suggest-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.promo-list-scroll { display: flex; flex-direction: column; gap: 5px; max-height: 150px; overflow-y: auto; }
.promo-chip { background: #fff; border: 1px solid #dcdfe6; border-radius: 6px; padding: 7px 10px; cursor: pointer; transition: border-color 0.15s; }
.promo-chip:hover { border-color: #9f7aea; }
.promo-chip.is-best { border: 1.5px solid #7c3aed; }
.best-badge { background: #7c3aed; color: #fff; font-size: 9px; font-weight: 600; padding: 2px 5px; border-radius: 3px; }
.pcode { font-family: monospace; font-weight: 600; font-size: 12px; flex: 1; color: #303133; }
.pval { font-size: 12px; font-weight: 600; color: #f56c6c; }
.psave { font-size: 10px; color: #67c23a; margin-top: 2px; }
.pexp { font-size: 10px; color: #909399; margin-top: 1px; }
</style>