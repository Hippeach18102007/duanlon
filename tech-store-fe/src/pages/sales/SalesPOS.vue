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

        <!-- Results List (Compact Cards) -->
        <div v-else-if="productGroups.length">
          <el-row :gutter="15">
            <el-col v-for="product in productGroups" :key="product.name" :span="24" class="mb-15">
              <el-card shadow="hover" body-style="padding: 12px">
                <template #header>
                  <div class="card-header-compact">
                    <el-text tag="b" size="large">{{ product.name }}</el-text>
                    <el-tag v-if="product.id" size="small" type="info" effect="plain">ID: {{ product.id }}</el-tag>
                  </div>
                </template>

                <div v-for="variant in product.variants" :key="variant.name" class="variant-item-compact">
                  <el-row justify="space-between" align="middle">
                    <el-space :size="5">
                      <el-text tag="b" size="small" color="#606266">{{ variant.name }}</el-text>
                      <el-text type="danger" tag="b">{{ formatMoney(variant.price) }}</el-text>
                    </el-space>
                    
                    <el-space>
                      <!-- Cảnh báo tồn kho thấp (< 5) -->
                      <el-tag v-if="variant.remainingCount > 0 && variant.remainingCount < 5" type="danger" effect="dark" size="small" class="blink-tag">
                        SẮP HẾT HÀNG
                      </el-tag>
                      <el-tag :type="variant.remainingCount > 0 ? 'success' : 'info'" size="small">
                        Kho: {{ variant.remainingCount }}
                      </el-tag>
                    </el-space>
                  </el-row>

                  <div class="serial-grid mt-10">
                    <el-button
                      v-for="s in variant.serials"
                      :key="s.id"
                      size="small"
                      :type="alreadyInCart(s.id) ? 'success' : 'default'"
                      :disabled="alreadyInCart(s.id)"
                      @click="addToCart(s, variant, product.name)"
                      class="serial-btn"
                    >
                      {{ serialCode(s) }}
                    </el-button>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
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

        <!-- Compact Cart Table -->
        <div class="cart-container">
          <el-table :data="cart" size="small" class="compact-table" :show-header="false">
            <el-table-column>
              <template #default="{ row }">
                <div class="cart-item-info">
                  <el-text tag="b" size="small" truncated>{{ row.productName }}</el-text>
                  <div class="flex-row">
                    <el-text size="extra-small" type="info">{{ row.variantName }}</el-text>
                    <el-text size="extra-small" type="warning" class="mono ml-10">{{ row.serialCode }}</el-text>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column align="right" width="100">
              <template #default="{ row }">
                <div class="cart-item-price">
                  <el-text tag="b" size="small">{{ formatMoney(row.price) }}</el-text>
                  <el-button :icon="Close" circle size="extra-small" @click="removeFromCart(row.serialId)" type="danger" plain />
                </div>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!cart.length" description="Giỏ hàng trống" :image-size="40" />
        </div>

        <!-- Footer Section (Fixed Bottom) -->
        <div class="sidebar-footer">
          <!-- Draft Indicator -->
          <div v-if="hasDraft" class="draft-tip">
            <el-text size="extra-small" type="info"><el-icon><InfoFilled /></el-icon> Đã lưu bản nháp tự động</el-text>
          </div>

          <!-- Customer info -->
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
import { Monitor, Loading, ShoppingCart, Delete, Close, User, Wallet, InfoFilled, Medal, Present } from "@element-plus/icons-vue";
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

// ── [NEW] Draft Order Logic ──
watch([cart, foundCustomer], () => {
  if (cart.value.length > 0 || foundCustomer.value) {
    localStorage.setItem("pos_draft_cart", JSON.stringify(cart.value));
    localStorage.setItem("pos_draft_customer", JSON.stringify(foundCustomer.value));
    hasDraft.value = true;
  } else {
    localStorage.removeItem("pos_draft_cart");
    localStorage.removeItem("pos_draft_customer");
    hasDraft.value = false;
  }
}, { deep: true });

function loadDraft() {
  const sCart = localStorage.getItem("pos_draft_cart");
  const sCus = localStorage.getItem("pos_draft_customer");
  if (sCart) {
    cart.value = JSON.parse(sCart);
    hasDraft.value = true;
  }
  if (sCus) foundCustomer.value = JSON.parse(sCus);
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
  try {
    const res = await serialsApi.list({ keyword: q, size: 50 });
    const rawData = res.data?.data ?? res.data;
    // Hỗ trợ cả Page object (.content) và mảng phẳng
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
        groupMap[pName].variants[vName] = { id: vId, name: vName, price: 0, remainingCount: 0, serials: [] };
      }
      groupMap[pName].variants[vName].serials.push(serial);
      groupMap[pName].variants[vName].remainingCount++;
    }

    productGroups.value = Object.values(groupMap).map(g => ({ ...g, variants: Object.values(g.variants) }));

    // Fetch variant price - Robust name matching
    await Promise.all(productGroups.value.map(async (group) => {
      try {
        const pRes = await productsApi.list({ keyword: group.name, size: 5 });
        const pData = pRes.data?.data ?? pRes.data;
        const pList = Array.isArray(pData?.content) ? pData.content : (Array.isArray(pData) ? pData : []);
        const product = pList.find(p => p.name === group.name);
        
        if (product?.id) {
          group.id = product.id;
          const vRes = await productsApi.getVariants(product.id);
          const vList = vRes.data?.data || vRes.data || [];
          group.variants.forEach(v => {
            const match = vList.find(vo => vo.id === v.id || vo.variantName === v.name || vo.name === v.name);
            if (match) {
              if (!v.id) v.id = match.id;
              if (match.price) v.price = Number(match.price);
            }
          });
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
  localStorage.removeItem("pos_draft_cart"); localStorage.removeItem("pos_draft_customer");
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
.pos-container { height: 100vh; background: #f0f2f5; overflow: hidden; }
.pos-main { padding: 15px; overflow-y: auto; }
.pos-sidebar { height: 100vh; background: #fff; border-left: 1px solid #dcdfe6; position: relative; }
.main-content-wrapper { max-width: 1000px; margin: 0 auto; }
.sidebar-wrapper { display: flex; flex-direction: column; height: 100%; }
.sidebar-header { padding: 12px 15px; border-bottom: 1px solid #f0f0f0; background: #fafafa; }
.cart-container { flex: 1; overflow-y: auto; padding: 0 5px; }
.sidebar-footer { padding: 15px; border-top: 1px solid #ebeef5; background: #fff; box-shadow: 0 -4px 10px rgba(0,0,0,0.03); }

/* Compact Styles */
.compact-table :deep(.el-table__cell) { padding: 4px 0; }
.cart-item-info { display: flex; flex-direction: column; }
.cart-item-price { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; }
.variant-item-compact { padding: 8px 0; border-bottom: 1px dotted #ebeef5; }
.variant-item-compact:last-child { border-bottom: none; }
.serial-grid { display: flex; flex-wrap: wrap; gap: 6px; }
.serial-btn { margin: 0 !important; font-family: monospace; font-size: 11px; }

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