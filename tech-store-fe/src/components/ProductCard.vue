<template>
  <el-card
    class="product-card"
    shadow="hover"
    :body-style="{ padding: '0px', display: 'flex', flexDirection: 'column', height: '100%' }"
    @click="goToDetail"
  >
    <!-- Image Wrap -->
    <div class="img-wrap">
      <el-image
        :src="product.imageUrl || '/placeholder.png'"
        :alt="product.name"
        fit="contain"
        lazy
        class="main-img"
      >
        <template #placeholder>
          <div class="image-slot">
            <el-icon class="is-loading"><Loading /></el-icon>
          </div>
        </template>
        <template #error>
          <div class="image-slot">
            <el-icon><Picture /></el-icon>
          </div>
        </template>
      </el-image>

      <!-- Badges -->
      <div v-if="product.isOutOfStock" class="badge out-of-stock">
        HẾT HÀNG
      </div>
      <div v-else class="badges-tl">
        <el-tag v-if="discountPercent > 0" type="danger" effect="dark" size="small" style="font-weight: bold; border: none; padding: 0 6px;">
          -{{ discountPercent }}%
        </el-tag>
        <el-tag v-else-if="product.isNew" type="success" effect="dark" size="small" style="font-weight: bold; border: none; padding: 0 6px;">
          MỚI
        </el-tag>
      </div>
    </div>

    <!-- Info Body -->
    <div class="info-body">
      <!-- Title & Desc -->
      <div class="title-desc">
        <el-text class="p-title" tag="h3" line-clamp="2" :title="product.name">
          {{ product.name }}
        </el-text>
        <el-text class="p-desc" size="small" type="info" line-clamp="2">
          {{ product.description || 'Laptop chính hãng phân phối bởi TechStore.' }}
        </el-text>
      </div>

      <!-- Pricing -->
      <div class="price-box">
        <template v-if="product.price">
          <el-row align="middle" style="gap: 8px;">
            <el-text tag="b" type="danger" class="sale-price">
              {{ formatPrice(product.price) }}
            </el-text>
            <el-text v-if="originalPrice > product.price" tag="del" type="info" size="small">
              {{ formatPrice(originalPrice) }}
            </el-text>
          </el-row>
          <el-text v-if="product.price >= 3000000" size="small" type="success" style="display: flex; align-items: center; gap: 4px; margin-top: 4px;">
            <el-icon><Ticket /></el-icon> Trả góp 0%
          </el-text>
        </template>
        <template v-else>
          <el-text tag="b" type="primary" class="sale-price">{{ product.priceText || 'Liên hệ' }}</el-text>
        </template>
      </div>

      <!-- Actions -->
      <div class="action-box">
        <el-button
          type="primary"
          :disabled="!isCustomer || !product.defaultVariantId || product.isOutOfStock"
          class="btn-add-cart"
          @click.stop="onAddToCart"
        >
          <el-icon><ShoppingCart /></el-icon>
          <span style="margin-left: 6px; font-weight: 600;">{{ product.isOutOfStock ? 'HẾT HÀNG' : 'THÊM VÀO GIỎ' }}</span>
        </el-button>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { Picture, Loading, ShoppingCart, Ticket } from '@element-plus/icons-vue';
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const props = defineProps({
  product: {
    type: Object,
    required: true,
  }
});

const emit = defineEmits(['add-to-cart']);
const router = useRouter();
const auth = useAuthStore();
const isCustomer = computed(() => auth.isCustomer);

// MOCK: Giả lập giá cũ cao hơn để làm psychology pricing nếu API chưa trả về
const originalPrice = computed(() => {
  if (!props.product.price) return 0;
  return props.product.originalPrice || Math.round(props.product.price * 1.15 / 10000) * 10000;
});

const discountPercent = computed(() => {
  if (originalPrice.value > props.product.price && props.product.price > 0) {
    return Math.round((originalPrice.value - props.product.price) / originalPrice.value * 100);
  }
  return 0;
});

function formatPrice(val) {
  if (!val) return 'Liên hệ';
  return new Intl.NumberFormat('vi-VN').format(val) + 'đ';
}

function goToDetail() {
  router.push('/product/' + props.product.id);
}

function onAddToCart() {
  emit('add-to-cart', props.product);
}
</script>

<style scoped>
.product-card {
  height: 100%;
  cursor: pointer;
  border-radius: 12px;
  border: 1px solid var(--el-border-color-lighter);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  overflow: hidden;
  background: #fff;
  position: relative;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 32px -12px rgba(0, 0, 0, 0.15) !important;
  border-color: var(--el-color-primary-light-5);
  z-index: 2;
}

.img-wrap {
  position: relative;
  width: 100%;
  padding-top: 100%; /* 1:1 Aspect ratio */
  overflow: hidden;
  background-color: #f8fafc;
}

.main-img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  padding: 24px;
  box-sizing: border-box;
  transition: transform 0.4s ease;
}

.product-card:hover .main-img {
  transform: scale(1.08);
}

.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  color: var(--el-text-color-secondary);
  font-size: 24px;
}

.badges-tl {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  z-index: 2;
}

.badge.out-of-stock {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 8px 16px;
  border-radius: 4px;
  font-weight: bold;
  font-size: 14px;
  letter-spacing: 1px;
  z-index: 3;
}

.info-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.title-desc {
  flex: 1;
  margin-bottom: 12px;
}

.p-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0 0 6px;
  line-height: 1.4;
  word-break: break-word;
}

.p-desc {
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.price-box {
  margin-bottom: 16px;
}

.sale-price {
  font-size: 18px;
  letter-spacing: -0.5px;
}

.action-box {
  margin-top: auto;
}

.btn-add-cart {
  width: 100%;
  border-radius: 8px;
  height: 40px;
  transition: all 0.3s;
}

/* Hover state on desktop */
@media (hover: hover) {
  .btn-add-cart {
    background-color: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
    border-color: var(--el-color-primary-light-8);
  }
  .product-card:hover .btn-add-cart:not(.is-disabled) {
    background-color: var(--el-color-primary);
    color: white;
  }
}
</style>
