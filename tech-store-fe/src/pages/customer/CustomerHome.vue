<template>
  <div class="ch-wrap">
    <TierProgressBar v-if="isCustomer" ref="tierProgressRef" style="margin-bottom: 20px;" />
<PromoBanner />
    <div class="ch-banners">
      <transition-group name="banner-list" tag="div">
        <el-card
          v-for="notif in welcomeNotifications"
          :key="notif.id"
          shadow="never"
          style="margin-bottom: 10px;"
        >
          <el-row align="middle" :gutter="16" style="flex-wrap: nowrap;">
            <el-col flex="none"><span style="font-size: 32px;">🎁</span></el-col>
            <el-col flex="1" style="min-width: 0;">
              <el-text size="small" type="warning" style="text-transform: uppercase; letter-spacing: 0.08em;">✨ Dành riêng cho bạn</el-text>
              <div><el-text tag="b">{{ notif.title }}</el-text></div>
              <el-text size="small" type="info"><span v-html="formatMessage(notif.message)" /></el-text>
            </el-col>
            <el-col flex="none">
              <el-space :size="8">
                <el-button type="primary" plain size="small" @click="handleWelcomeShop(notif.id)">🛍️ Mua ngay</el-button>
                <el-button plain size="small" @click="openNotificationsPanel">Thông báo</el-button>
                <el-button text size="small" @click="markNotificationAsRead(notif.id)">✕</el-button>
              </el-space>
            </el-col>
          </el-row>
        </el-card>
      </transition-group>

      <transition-group name="banner-list" tag="div">
        <el-card
          v-for="notif in birthdayNotifications"
          :key="notif.id"
          shadow="never"
          style="margin-bottom: 10px;"
        >
          <el-row align="top" :gutter="16" style="flex-wrap: nowrap;">
            <el-col flex="none"><span style="font-size: 32px;">🎂</span></el-col>
            <el-col flex="1" style="min-width: 0;">
              <div><el-text tag="b">{{ notif.title }}</el-text></div>
              <el-text size="small" type="info"><span v-html="formatMessage(notif.message)" /></el-text>
              <el-space style="margin-top: 10px;">
                <el-button type="primary" plain size="small" @click="markNotificationAsRead(notif.id)">Cảm ơn! 🎉</el-button>
                <el-button plain size="small" @click="openNotificationsPanel">Xem tất cả</el-button>
              </el-space>
            </el-col>
            <el-col flex="none">
              <el-button text size="small" @click="markNotificationAsRead(notif.id)">✕</el-button>
            </el-col>
          </el-row>
        </el-card>
      </transition-group>

      <transition-group name="banner-list" tag="div">
        <el-card
          v-for="notif in reminderNotifications"
          :key="notif.id"
          shadow="never"
          style="margin-bottom: 10px;"
        >
          <el-row align="top" :gutter="16" style="flex-wrap: nowrap;">
            <el-col flex="none"><span style="font-size: 32px;">{{ notif.type === 'WINBACK' ? '💝' : '🛒' }}</span></el-col>
            <el-col flex="1" style="min-width: 0;">
              <div><el-text tag="b">{{ notif.title }}</el-text></div>
              <el-text size="small" type="info"><span v-html="formatMessage(notif.message)" /></el-text>
              <el-space style="margin-top: 10px;">
                <el-button type="warning" plain size="small" @click="handleReminderClick(notif.id)">Xem ngay 🛍️</el-button>
                <el-button plain size="small" @click="markNotificationAsRead(notif.id)">Để sau</el-button>
              </el-space>
            </el-col>
            <el-col flex="none">
              <el-button text size="small" @click="markNotificationAsRead(notif.id)">✕</el-button>
            </el-col>
          </el-row>
        </el-card>
      </transition-group>

      <transition-group name="banner-list" tag="div">
        <el-card
          v-for="bonus in spinExpiryBonuses"
          :key="bonus.customerId"
          shadow="never"
          style="margin-bottom: 10px;"
        >
          <el-progress
            :percentage="Math.round((bonus.hoursLeft / 24) * 100)"
            :stroke-width="4"
            :show-text="false"
            :status="getSpinUrgencyLevel(bonus.hoursLeft) === 'critical' ? 'exception' : ''"
            style="margin-bottom: 8px;"
          />
          <el-row align="middle" :gutter="16" style="flex-wrap: nowrap;">
            <el-col flex="none"><span style="font-size: 32px;">🎡</span></el-col>
            <el-col flex="1" style="min-width: 0;">
              <div><el-text tag="b">Ưu đãi vòng quay sắp hết hạn!</el-text></div>
              <el-text size="small" type="info">
                Giảm <strong>{{ bonus.discountBonus }}%</strong> — còn
                <strong>{{ bonus.hoursLeft }} giờ</strong> · Hết hạn {{ formatDate(bonus.expiresAt) }}
              </el-text>
            </el-col>
            <el-col flex="none">
              <el-space :size="8">
                <el-button type="primary" plain size="small" @click="handleSpinExpiryUseNow(bonus)">Dùng ngay</el-button>
                <el-button text size="small" @click="dismissSpinExpiry(bonus.customerId)">✕</el-button>
              </el-space>
            </el-col>
          </el-row>
        </el-card>
      </transition-group>
    </div>

    <div v-if="topProducts.length > 0" style="margin-bottom: 32px;">
      <el-row align="middle" justify="space-between" style="margin-bottom: 14px;">
        <el-space :size="8" align="center">
          <el-text tag="b" style="font-size: 15px; letter-spacing: -0.01em;">🔥 Bán chạy nhất</el-text>
        </el-space>
        <el-text size="small" type="info">Được yêu thích nhiều nhất</el-text>
      </el-row>

      <div class="top-scroll">
        <el-card
          v-for="p in topProducts"
          :key="'top-' + p.id"
          shadow="never"
          :body-style="{ padding: 0 }"
          class="top-card"
          :class="{ 'is-out-of-stock': p.isOutOfStock }"
          @click="$router.push('/product/' + p.id)"
        >
          <div class="product-img-wrap">
            <img :src="p.imageUrl" :alt="p.name" class="product-img" loading="lazy" />
            <div v-if="p.isOutOfStock" class="badge-center">
              <el-tag type="danger" effect="dark">HẾT HÀNG</el-tag>
            </div>
            <div v-else-if="p.isNew" class="badge-tl">
              <el-tag type="danger" size="small">NEW</el-tag>
            </div>
          </div>
          <div class="product-body">
            <el-text class="product-name" tag="div">{{ p.name }}</el-text>
            <el-text class="product-desc" size="small" type="info">{{ p.description || '—' }}</el-text>
            <el-divider style="margin: 8px 0;" />
            <el-row align="middle" justify="space-between">
              <el-text tag="b" type="primary">{{ p.priceText }}</el-text>
            </el-row>
            <el-button
              plain
              size="small"
              style="width: 100%; margin-top: 8px;"
              :disabled="!isCustomer || !p.defaultVariantId || p.isOutOfStock"
              @click.stop="goOrder(p)"
            >
              <el-icon v-if="!p.isOutOfStock"><ShoppingCart /></el-icon>
              {{ p.isOutOfStock ? 'Hết hàng' : 'Thêm vào giỏ' }}
            </el-button>
          </div>
        </el-card>
      </div>
    </div>

    <div class="ch-layout">

      <aside class="ch-sidebar">
        <div style="position: sticky; top: 24px; display: flex; flex-direction: column; gap: 16px; max-height: calc(100vh - 48px); overflow-y: auto; padding-bottom: 20px;">
          
          <el-button v-if="hasActiveFilters" type="danger" plain size="small" @click="resetFilters">
            Xóa tất cả bộ lọc
          </el-button>

          <el-card shadow="never">
            <template #header>
              <el-row justify="space-between" align="middle">
                <el-text size="small" style="text-transform: uppercase; letter-spacing: 0.1em; font-weight: 600;">Danh mục</el-text>
              </el-row>
            </template>
            <el-select v-model="activeKey" style="width: 100%;" @change="onSelectCategory" placeholder="Chọn danh mục">
              <el-option label="Tất cả" value="all" />
              <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="String(c.id)" />
            </el-select>
          </el-card>

          <el-card shadow="never">
            <template #header>
              <el-text size="small" style="text-transform: uppercase; letter-spacing: 0.1em; font-weight: 600;">Thương hiệu</el-text>
            </template>
            <el-select v-model="selectedBrand" style="width: 100%;" @change="onSelectBrand" placeholder="Chọn hãng">
              <el-option label="Tất cả hãng" value="all" />
              <el-option v-for="b in brands" :key="b" :label="b" :value="b" />
            </el-select>
          </el-card>

          <el-card shadow="never">
            <template #header>
              <el-text size="small" style="text-transform: uppercase; letter-spacing: 0.1em; font-weight: 600;">Khoảng giá</el-text>
            </template>
            <div style="display: flex; flex-direction: column; gap: 10px;">
              <el-input-number v-model="minPrice" :min="0" :step="1000000" placeholder="Từ (VNĐ)" size="small" style="width: 100%;" :controls="false" />
              <el-input-number v-model="maxPrice" :min="0" :step="1000000" placeholder="Đến (VNĐ)" size="small" style="width: 100%;" :controls="false" />
              <el-button type="primary" plain size="small" style="width: 100%;" @click="applyPriceFilter">Áp dụng giá</el-button>
            </div>
          </el-card>

        </div>
      </aside>

      <main class="ch-main">
        <el-row justify="space-between" align="bottom" style="margin-bottom: 14px; flex-wrap: wrap; gap: 10px;">
          <el-space direction="vertical" :size="2">
            <el-text size="small" type="info" style="text-transform: uppercase; letter-spacing: 0.1em;">Sản phẩm</el-text>
            <el-text tag="b" style="font-size: 18px; letter-spacing: -0.02em;">{{ titleText }}</el-text>
            <el-text type="info" size="small">
              Trang {{ page + 1 }}
              <span v-if="searchTerm"> · "<em>{{ searchTerm }}</em>"</span>
            </el-text>
          </el-space>
          <el-space wrap>
            <el-select v-model="sortBy" style="width: 156px;" @change="onSelectSort" placeholder="Mặc định">
              <el-option label="Mới nhất"       value="newest_arrival" />
              <el-option label="Cũ nhất"        value="oldest" />
              <el-option label="Bán chạy nhất"  value="best_selling" />
              <el-option label="Giá tăng dần"   value="price_asc" />
              <el-option label="Giá giảm dần"   value="price_desc" />
              <el-option label="Tên: A → Z"     value="name_asc" />
              <el-option label="Tên: Z → A"     value="name_desc" />
            </el-select>
            <el-tooltip content="Làm mới" placement="top">
              <el-button plain :loading="loading" @click="reloadProducts">
                <el-icon v-if="!loading"><Refresh /></el-icon>
              </el-button>
            </el-tooltip>
          </el-space>
        </el-row>
        <el-divider style="margin: 0 0 20px;" />

        <div v-if="loading" class="product-grid">
          <el-card v-for="i in 6" :key="i" shadow="never" :body-style="{ padding: 0 }">
            <el-skeleton animated>
              <template #template>
                <el-skeleton-item variant="image" style="width: 100%; height: 170px;" />
                <div style="padding: 14px;">
                  <el-skeleton-item variant="p" style="width: 75%; margin-bottom: 8px;" />
                  <el-skeleton-item variant="p" style="width: 50%; margin-bottom: 8px;" />
                  <el-skeleton-item variant="p" style="width: 40%; margin-top: 4px;" />
                </div>
              </template>
            </el-skeleton>
          </el-card>
        </div>

        <template v-else>
          <el-empty
            v-if="products.length === 0"
            description="Không có sản phẩm nào phù hợp"
            :image-size="80"
          >
            <el-text type="info">Thử điều chỉnh lại bộ lọc giá, thương hiệu hoặc danh mục.</el-text>
            <br/>
            <el-button v-if="hasActiveFilters" type="primary" plain style="margin-top: 10px;" @click="resetFilters">Xóa bộ lọc</el-button>
          </el-empty>

          <div v-else class="product-grid">
            <el-card
              v-for="p in products"
              :key="p.id"
              shadow="never"
              :body-style="{ padding: 0 }"
              class="product-card"
              :class="{ 'is-out-of-stock': p.isOutOfStock }"
              @click="$router.push('/product/' + p.id)"
            >
              <div class="product-img-wrap">
                <img :src="p.imageUrl" :alt="p.name" class="product-img" loading="lazy" />
                <div v-if="p.isOutOfStock" class="badge-center">
                  <el-tag type="danger" effect="dark">HẾT HÀNG</el-tag>
                </div>
                <div v-else-if="p.isNew" class="badge-tl">
                  <el-tag type="danger" size="small">NEW</el-tag>
                </div>
              </div>
              <div class="product-body">
                <el-text class="product-name" tag="div">{{ p.name }}</el-text>
                <el-text class="product-desc" size="small" type="info">{{ p.description || '—' }}</el-text>
                <el-divider style="margin: 8px 0;" />
                <el-text tag="b" type="primary">{{ p.priceText }}</el-text>
                <el-button
                  plain
                  size="small"
                  style="width: 100%; margin-top: 8px;"
                  :disabled="!isCustomer || !p.defaultVariantId || p.isOutOfStock"
                  @click.stop="goOrder(p)"
                >
                  <el-icon v-if="!p.isOutOfStock"><ShoppingCart /></el-icon>
                  {{ p.isOutOfStock ? 'Hết hàng' : 'Thêm vào giỏ' }}
                </el-button>
              </div>
            </el-card>
          </div>

          <el-row justify="center" style="margin-top: 36px;">
            <el-pagination
              background
              layout="prev, pager, next"
              :total="totalElements"
              :page-size="20"
              :current-page="page + 1"
              :hide-on-single-page="true"
              @current-change="onServerPageChange"
            />
          </el-row>
        </template>
      </main>
    </div>

    <AiChatWidget />
  </div>
 <Footerapp />
</template>

<script setup>
import { Refresh, ShoppingCart } from "@element-plus/icons-vue";
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { categoriesApi } from "../../api/categories.api";
import AiChatWidget from '../../components/Aichatwidget.vue';
import { productsApi } from "../../api/products.api";
import { useAuthStore } from "../../stores/auth";
import { toast } from "../../ui/toast";
import { useCartStore } from "../../stores/cart";
import http from "../../api/http";
import TierProgressBar from "../../components/TierProgressBar.vue";
import PromoBanner from '../../components/Promobanner.vue';
import Footerapp from "../../components/Footerapp.vue";

const auth = useAuthStore();
const isCustomer = computed(() => auth.isCustomer);
const cartStore = useCartStore();

const loading = ref(false);
const activeOnly = ref(true);
const categories = ref([]);
const products = ref([]);
const activeKey = ref("all");
const categoryId = ref(null);

const brands = ref([]);
const selectedBrand = ref("all");
const minPrice = ref(null);
const maxPrice = ref(null);

const sortBy = ref("newest_arrival");

const page = ref(0);
const totalElements = ref(0);
const searchTerm = ref("");
const totalPages = computed(() =>
  Math.max(1, Math.ceil(totalElements.value / 20)),
);

const hasActiveFilters = computed(() => {
  return (selectedBrand.value && selectedBrand.value !== "all") 
      || minPrice.value != null 
      || maxPrice.value != null
      || categoryId.value != null;
});

const welcomeNotifications = ref([]);
const birthdayNotifications = ref([]);
const reminderNotifications = ref([]);
const tierProgressRef = ref(null);
const spinExpiryBonuses = ref([]);
const spinExpiryDismissed = ref(new Set());

const topProducts = ref([]);

function confettiStyle(i) {
  const colors = ["#f59e0b","#ef4444","#8b5cf6","#3b82f6","#10b981","#ec4899","#f97316","#06b6d4"];
  return { "--c": colors[i % colors.length], "--delay": i * 0.13 + "s", "--x": Math.sin(i * 1.2) * 60 + "px" };
}

const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

function fixImageUrl(url) {
  if (!url) return "https://via.placeholder.com/300?text=No+Image";
  if (url.startsWith("http")) return url;
  return `${BASE_URL}${url}`;
}

async function loadSpinExpiryBonuses() {
  if (!isCustomer.value) return;
  try {
    const customerId = auth.user?.customerId || auth.customerId;
    if (!customerId) return;
    const res = await http.get(`/api/spin-wheel/status/${customerId}`);
    const data = res.data?.data ?? res.data ?? res;
    const bonus = Number(data?.currentBonus ?? data?.spinDiscountBonus ?? 0);
    const expiresAt = data?.bonusExpiresAt ?? data?.spinBonusExpiresAt ?? null;
    if (bonus > 0 && expiresAt) {
      const hoursLeft = Math.floor((new Date(expiresAt) - new Date()) / (1000 * 60 * 60));
      spinExpiryBonuses.value = hoursLeft > 0 && hoursLeft <= 24
        ? [{ customerId, discountBonus: bonus, hoursLeft, expiresAt }] : [];
    } else {
      spinExpiryBonuses.value = [];
    }
  } catch { spinExpiryBonuses.value = []; }
}

function dismissSpinExpiry(customerId) {
  spinExpiryDismissed.value.add(customerId);
  spinExpiryBonuses.value = spinExpiryBonuses.value.filter(b => b.customerId !== customerId);
}

function handleSpinExpiryUseNow(bonus) {
  dismissSpinExpiry(bonus.customerId);
  window.location.href = "/orders/new";
}

function getSpinUrgencyLevel(hoursLeft) {
  if (hoursLeft <= 3) return "critical";
  if (hoursLeft <= 12) return "warning";
  return "normal";
}

function getSpinProgressWidth(hoursLeft) {
  return `${Math.max(0, Math.min(100, (hoursLeft / 24) * 100))}%`;
}

function normalizeProducts(list) {
  return (list || []).map((p, idx) => {
    const id = p?.id ?? p?.productId ?? idx + 1;
    const price = p?.finalPrice ?? p?.price ?? p?.minPrice ?? null;
    const stockCount = p.totalStock !== undefined ? p.totalStock : 1;
    const isOutOfStock = p.inStock === false || stockCount <= 0 || p.isVisible === false;
    return {
      id,
      name: p?.name ?? p?.title ?? `Product ${id}`,
      description: p.description || "",
      imageUrl: fixImageUrl(p.imageUrl || p.thumbnailUrl),
      priceText: typeof price === "number"
        ? new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(price)
        : price != null ? String(price) : "—",
      defaultVariantId: p?.defaultVariantId ?? p?.variantId ?? p?.variants?.[0]?.id ?? null,
      isNew: p.isNew,
      isOutOfStock,
      raw: p,
    };
  });
}

function normalizeCategories(list) {
  const unique = [], names = new Set();
  (list || []).forEach(c => {
    if (!names.has(c.name)) {
      unique.push({ id: c?.id ?? unique.length + 1, name: c?.name ?? `Category ${unique.length + 1}`, raw: c });
      names.add(c.name);
    }
  });
  return unique;
}

const titleText = computed(() => {
  if (!categoryId.value) return "Tất cả sản phẩm";
  return categories.value.find(x => String(x.id) === String(categoryId.value))?.name || `Danh mục #${categoryId.value}`;
});

async function reloadCategories() {
  try {
    const res = await categoriesApi.list(activeOnly.value);
    const root = res.data?.data || res.data || [];
    categories.value = normalizeCategories(Array.isArray(root) ? root : root.content).filter(c => c.id !== 6);
  } catch { categories.value = []; }
}

async function loadBrands() {
  try {
    const res = await http.get('/api/products/brands');
    brands.value = res.data || [];
  } catch (e) {
    console.error("Không lấy được danh sách hãng", e);
  }
}

async function loadTopProducts() {
  try {
    const res = await productsApi.list({ sortBy: 'best_selling', page: 0, size: 10 });
    const data = res.data?.data || res.data;
    topProducts.value = normalizeProducts(data.content || []).slice(0, 7);
  } catch (e) { console.error("Lỗi tải Top Bán Chạy", e); }
}

async function reloadProducts() {
  loading.value = true;
  try {
    const params = { 
      page: page.value, 
      categoryIds: categoryId.value ? String(categoryId.value) : undefined, 
      keyword: searchTerm.value || undefined, 
      isFaulty: false, 
      sortBy: sortBy.value,
      brand: (selectedBrand.value && selectedBrand.value !== "all") ? selectedBrand.value : undefined,
      minPrice: minPrice.value != null ? minPrice.value : undefined,
      maxPrice: maxPrice.value != null ? maxPrice.value : undefined
    };
    
    const res = await productsApi.list(params);
    const data = res.data?.data || res.data;
    products.value = normalizeProducts(data.content || []);
    totalElements.value = data.totalElements || 0;
  } catch {
    products.value = [];
    toast("Failed to load products.", "error");
  } finally { loading.value = false; }
}

async function reloadAll() {
  page.value = 0;
  await reloadCategories();
  await loadBrands();
  await reloadProducts();
  await loadTopProducts();
}

function onSelectCategory(key) {
  activeKey.value = key;
  categoryId.value = key === "all" ? null : Number(key);
  page.value = 0;
  reloadProducts();
}

function onSelectBrand(val) {
  page.value = 0;
  reloadProducts();
}

function applyPriceFilter() {
  if (minPrice.value != null && maxPrice.value != null && minPrice.value > maxPrice.value) {
    toast("Giá tối thiểu không thể lớn hơn giá tối đa", "warning");
    return;
  }
  page.value = 0;
  reloadProducts();
}

function resetFilters() {
  selectedBrand.value = "all";
  minPrice.value = null;
  maxPrice.value = null;
  activeKey.value = "all";
  categoryId.value = null;
  searchTerm.value = "";
  page.value = 0;
  reloadProducts();
}

function onSelectSort(val) {
  sortBy.value = val;
  page.value = 0;
  reloadProducts();
}

function onServerPageChange(page1Based) {
  page.value = Math.max(0, Number(page1Based || 1) - 1);
  reloadProducts();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

async function goOrder(p) {
  try {
    await cartStore.addToCart(p.defaultVariantId, 1);
    toast("Đã thêm vào giỏ hàng", "success");
    if (tierProgressRef.value) tierProgressRef.value.refresh();
  } catch { toast("Không thể thêm vào giỏ hàng", "error"); }
}

async function loadNotifications() {
  if (!isCustomer.value) return;
  try {
    const unreadRes = await http.get("/api/auth/notifications/my?unreadOnly=true");
    const unreadNotifs = unreadRes.data || [];
    welcomeNotifications.value  = unreadNotifs.filter(n => n.type === "WELCOME");
    birthdayNotifications.value = unreadNotifs.filter(n => n.type === "BIRTHDAY");
    reminderNotifications.value = unreadNotifs.filter(n => ["PURCHASE_REMINDER", "WINBACK"].includes(n.type));
  } catch (e) { console.error(e); }
}

function openNotificationsPanel() {
  window.dispatchEvent(new CustomEvent("header:open-notifications"));
}

async function markNotificationAsRead(id) {
  try {
    await http.put(`/api/auth/notifications/${id}/read`);
    await loadNotifications();
    window.dispatchEvent(new CustomEvent("notifications:updated"));
  } catch { toast("Không thể đánh dấu đã đọc", "error"); }
}

async function handleReminderClick(id) {
  await markNotificationAsRead(id);
  window.scrollTo({ top: 400, behavior: "smooth" });
}

async function handleWelcomeShop(id) {
  await markNotificationAsRead(id);
  document.querySelector(".ch-main")?.scrollIntoView({ behavior: "smooth", block: "start" });
}

function formatMessage(msg) { return msg ? msg.replace(/\n/g, "<br>") : ""; }
function formatDate(d) { return new Date(d).toLocaleDateString("vi-VN"); }

onMounted(async () => {
  window.addEventListener("products:search", (e) => {
    searchTerm.value = String(e?.detail || "");
    page.value = 0;
    reloadProducts();
  });
  await reloadAll();
  if (isCustomer.value) {
    cartStore.refreshCount();
    loadNotifications();
    loadSpinExpiryBonuses();
    const notifInterval = setInterval(loadNotifications, 30000);
    onBeforeUnmount(() => clearInterval(notifInterval));
  }
});
</script>

<style scoped>
/* ── Wrap ──────────────────────────────────────────── */
.ch-wrap {
  max-width: 1400px;
  margin: 0 auto;
  padding: 28px 24px 60px;
}

/* ── Top Selling Horizontal Scroll ─────────────────── */
.top-scroll {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  padding-bottom: 8px;
  scrollbar-width: thin;
  scrollbar-color: var(--el-border-color) transparent;
}

.top-card {
  flex: 0 0 200px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.top-card:hover { box-shadow: var(--el-box-shadow-light); }

/* ── Layout ────────────────────────────────────────── */
.ch-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 24px;
  align-items: start;
}

@media (max-width: 900px) {
  .ch-layout { grid-template-columns: 1fr; }
  .ch-sidebar { display: none; }
}

/* ── Product Image ─────────────────────────────────── */
.product-img-wrap {
  position: relative;
  overflow: hidden;
}

.product-img {
  width: 100%;
  height: 180px;
  object-fit: cover;
  display: block;
  transition: transform 0.25s ease;
}

.product-card:hover .product-img,
.top-card:hover .product-img {
  transform: scale(1.04);
}

.is-out-of-stock .product-img { filter: grayscale(80%) opacity(0.75); }

.badge-center {
  position: absolute;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  z-index: 10;
  pointer-events: none;
}

.badge-tl {
  position: absolute;
  top: 10px; left: 10px;
}

/* ── Product Body ──────────────────────────────────── */
.product-body {
  padding: 12px 14px 14px;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.product-name {
  font-size: 13.5px;
  font-weight: 600;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 4px;
}

.product-desc {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

/* ── Product Grid ──────────────────────────────────── */
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.product-card {
  cursor: pointer;
  display: flex;
  flex-direction: column;
  transition: box-shadow 0.2s;
}

.product-card:hover { box-shadow: var(--el-box-shadow-light); }

.product-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.el-radio {
  margin-right: 0 !important;
  margin-bottom: 4px;
}
</style>