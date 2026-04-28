<!-- \src\pages\customer\CartPage.vue -->
<template>
  <div style="max-width: 1100px; margin: 0 auto; padding: 32px 24px 80px;">

    <!-- Header -->
    <el-row align="middle" justify="space-between" style="margin-bottom: 32px;">
      <el-space direction="vertical" :size="4">
        <el-text size="small" type="primary" style="text-transform: uppercase; letter-spacing: 0.12em; font-weight: 700;">Giỏ hàng</el-text>
        <el-space :size="10" align="center">
          <el-text tag="div" style="font-size: 28px; font-weight: 700; letter-spacing: -0.02em; line-height: 1.2;">Shopping Cart</el-text>
          <el-badge v-if="items.length" :value="items.length" type="primary" />
        </el-space>
      </el-space>
      <el-button plain @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon> Tiếp tục mua
      </el-button>
    </el-row>

    <!-- Loading skeleton -->
    <el-card v-if="loading" shadow="never" :body-style="{ padding: 0 }">
      <div v-for="i in 3" :key="i">
        <el-skeleton animated style="padding: 18px 22px;">
          <template #template>
            <el-row :gutter="16" align="middle">
              <el-col flex="none"><el-skeleton-item variant="image" style="width: 58px; height: 58px; border-radius: 10px;" /></el-col>
              <el-col flex="1">
                <el-skeleton-item variant="p" style="width: 60%; margin-bottom: 8px;" />
                <el-skeleton-item variant="p" style="width: 35%;" />
              </el-col>
              <el-col flex="none"><el-skeleton-item variant="p" style="width: 80px; height: 13px;" /></el-col>
              <el-col flex="none"><el-skeleton-item variant="button" style="width: 100px; height: 34px; border-radius: 10px;" /></el-col>
              <el-col flex="none"><el-skeleton-item variant="p" style="width: 80px; height: 13px;" /></el-col>
              <el-col flex="none"><el-skeleton-item variant="button" style="width: 34px; height: 34px; border-radius: 8px;" /></el-col>
            </el-row>
          </template>
        </el-skeleton>
        <el-divider v-if="i < 3" style="margin: 0;" />
      </div>
    </el-card>

    <!-- Empty -->
    <el-empty
      v-else-if="items.length === 0"
      :image-size="80"
      description="Giỏ hàng trống"
      style="padding: 80px 24px;"
    >
      <el-text type="info" style="display: block; margin-bottom: 20px;">Hãy thêm sản phẩm vào giỏ để tiếp tục</el-text>
      <el-button type="primary" @click="$router.push('/')">Khám phá sản phẩm</el-button>
    </el-empty>

    <!-- Cart content -->
    <div v-else class="cart-layout">

      <!-- Left: items -->
      <el-card shadow="never" :body-style="{ padding: 0 }" style="overflow: hidden;">
        <!-- Table head -->
        <div class="cart-head">
          <el-text size="small" type="info" style="text-transform: uppercase; letter-spacing: 0.08em; font-weight: 700;">Sản phẩm</el-text>
          <el-text size="small" type="info" style="text-transform: uppercase; letter-spacing: 0.08em; font-weight: 700;">Đơn giá</el-text>
          <el-text size="small" type="info" style="text-transform: uppercase; letter-spacing: 0.08em; font-weight: 700; text-align: center;">Số lượng</el-text>
          <el-text size="small" type="info" style="text-transform: uppercase; letter-spacing: 0.08em; font-weight: 700; text-align: right;">Thành tiền</el-text>
          <span />
        </div>

        <!-- Items -->
        <transition-group name="item-list" tag="div">
          <div
            v-for="(row, idx) in items"
            :key="row.cartItemId"
            class="cart-item"
          >
            <!-- Product -->
            <el-space :size="14" align="center" style="min-width: 0;">
              <div class="item-img-wrap">
                <img v-if="row.imageUrl" :src="fixImageUrl(row.imageUrl)" :alt="row.productName" class="item-img" />
                <el-icon v-else :size="20" style="color: var(--el-text-color-placeholder);"><Picture /></el-icon>
              </div>
              <el-space direction="vertical" :size="3" style="min-width: 0;">
                <el-text class="item-name" tag="div">{{ row.productName }}</el-text>
                <el-text v-if="row.variantName" size="small" type="info">{{ row.variantName }}</el-text>
              </el-space>
            </el-space>

            <!-- Unit price -->
            <el-text size="small" type="info">{{ formatMoney(row.price) }}</el-text>

            <!-- Qty stepper -->
            <div style="display: flex; justify-content: center;">
              <el-input-number
                v-model="row.quantity"
                :min="1"
                :disabled="updatingId === row.cartItemId"
                size="small"
                controls-position="right"
                style="width: 100px;"
                @change="(val) => { row.quantity = val; updateQty(row); }"
              />
            </div>

            <!-- Subtotal -->
            <el-text type="primary" style="font-weight: 700; text-align: right; font-variant-numeric: tabular-nums;">
              {{ formatMoney((row.price || 0) * row.quantity) }}
            </el-text>

            <!-- Remove -->
            <el-tooltip content="Xóa" placement="top">
              <el-button
                :icon="Delete"
                circle
                plain
                type="danger"
                size="small"
                :loading="removingId === row.cartItemId"
                @click="removeItem(row)"
              />
            </el-tooltip>
          </div>
        </transition-group>
      </el-card>

      <!-- Right: summary -->
      <div style="position: sticky; top: 24px; display: flex; flex-direction: column; gap: 16px;">
        <el-card shadow="never">
          <el-text tag="div" style="font-size: 17px; font-weight: 600; margin-bottom: 20px;">Tổng đơn hàng</el-text>

          <el-space direction="vertical" fill :size="12" style="width: 100%; margin-bottom: 16px;">
            <el-row justify="space-between">
              <el-text size="small">Tạm tính ({{ items.length }} sản phẩm)</el-text>
              <el-text size="small">{{ formatMoney(subtotal) }}</el-text>
            </el-row>
            <!-- ✅ FIX: Phí vận chuyển = Miễn phí -->
            <el-row justify="space-between" align="middle">
              <el-text size="small" type="info">Phí vận chuyển</el-text>
              <el-tag size="small" type="success" effect="plain">Miễn phí</el-tag>
            </el-row>
          </el-space>

          <el-divider style="margin: 0 0 16px;" />

          <el-row justify="space-between" align="middle" style="margin-bottom: 22px;">
            <el-text style="font-weight: 600;">Tổng cộng</el-text>
            <el-text type="primary" style="font-size: 22px; font-weight: 700; font-variant-numeric: tabular-nums;">
              {{ formatMoney(subtotal) }}
            </el-text>
          </el-row>

          <el-space direction="vertical" fill :size="10" style="width: 100%;">
            <el-button
              type="primary"
              size="large"
              style="width: 100%;"
              :disabled="!items.length"
              @click="checkout"
            >
              <el-icon><Right /></el-icon> Thanh toán ngay
            </el-button>
          </el-space>
        </el-card>

        <!-- Trust badges -->
        <el-card shadow="never" :body-style="{ padding: '16px 18px' }">
          <el-space direction="vertical" fill :size="12" style="width: 100%;">
            <el-space :size="10" align="center">
              <el-icon type="primary" style="color: var(--el-color-primary); flex-shrink: 0;"><Lock /></el-icon>
              <el-text size="small" type="info">Thanh toán bảo mật</el-text>
            </el-space>
            <el-space :size="10" align="center">
              <el-icon style="color: var(--el-color-primary); flex-shrink: 0;"><Location /></el-icon>
              <el-text size="small" type="info">Giao hàng toàn quốc</el-text>
            </el-space>
            <el-space :size="10" align="center">
              <el-icon style="color: var(--el-color-primary); flex-shrink: 0;"><Refresh /></el-icon>
              <el-text size="small" type="info">Đổi trả dễ dàng</el-text>
            </el-space>
          </el-space>
        </el-card>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ArrowLeft, Delete, Location, Lock, Picture, Refresh, Right } from "@element-plus/icons-vue";
import { computed, onMounted, ref } from "vue";
import { cartApi } from "../../api/cart.api";
import { useRouter } from "vue-router";

const router = useRouter();
const items = ref([]);
const loading = ref(false);
const updatingId = ref(null);
const removingId = ref(null);

const subtotal = computed(() =>
  items.value.reduce((sum, i) => sum + (i.price || 0) * (i.quantity || 1), 0),
);

const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

function fixImageUrl(url) {
  if (!url) return "";
  if (url.startsWith("http")) return url;
  return `${BASE_URL}${url}`;
}

async function loadCart() {
  loading.value = true;
  try {
    const res = await cartApi.getItems();
    items.value = res.data || [];
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
}

async function changeQty(item, delta) {
  const newQty = Math.max(1, item.quantity + delta);
  if (newQty === item.quantity) return;
  updatingId.value = item.cartItemId;
  try {
    item.quantity = newQty;
    await cartApi.updateQuantity(item.cartItemId, newQty);
  } catch (e) {
    item.quantity -= delta;
  } finally {
    updatingId.value = null;
  }
}

async function updateQty(item) {
  updatingId.value = item.cartItemId;
  try {
    await cartApi.updateQuantity(item.cartItemId, item.quantity);
  } finally {
    updatingId.value = null;
  }
}

async function removeItem(item) {
  removingId.value = item.cartItemId;
  try {
    await cartApi.remove(item.cartItemId);
    await loadCart();
  } finally {
    removingId.value = null;
  }
}

function checkout() {
  router.push("/orders/new");
}

function formatMoney(value) {
  if (value == null) return "—";
  return Number(value).toLocaleString("vi-VN") + " ₫";
}

onMounted(loadCart);
</script>

<style scoped>
/* Layout */
.cart-layout {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 24px;
  align-items: start;
}

@media (max-width: 860px) {
  .cart-layout { grid-template-columns: 1fr; }
}

/* Table head */
.cart-head {
  display: grid;
  grid-template-columns: 2.5fr 1fr 1.3fr 1fr 40px;
  gap: 12px;
  align-items: center;
  padding: 12px 22px;
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color-light);
}

/* Cart item row */
.cart-item {
  display: grid;
  grid-template-columns: 2.5fr 1fr 1.3fr 1fr 40px;
  gap: 12px;
  align-items: center;
  padding: 16px 22px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  transition: background 0.15s;
}

.cart-item:last-child { border-bottom: none; }
.cart-item:hover { background: var(--el-fill-color-lighter); }

@media (max-width: 640px) {
  .cart-head { display: none; }
  .cart-item {
    grid-template-columns: 1fr auto;
    grid-template-rows: auto auto;
    gap: 10px;
    padding: 14px 16px;
  }
}

/* Item image */
.item-img-wrap {
  width: 58px;
  height: 58px;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  display: flex;
  align-items: center;
  justify-content: center;
}

.item-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.item-name {
  font-size: 13.5px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}

/* Transition */
.item-list-enter-active { transition: all 0.3s cubic-bezier(0.34, 1.2, 0.64, 1); }
.item-list-leave-active { transition: all 0.22s ease; }
.item-list-enter-from { opacity: 0; transform: translateX(-10px); }
.item-list-leave-to { opacity: 0; transform: translateX(10px); }
</style>