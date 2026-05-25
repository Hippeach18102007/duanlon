<template>
  <div style="max-width: 1200px; margin: 0 auto; padding: 16px 20px 40px;">
    <el-breadcrumb separator="/" style="margin-bottom: 20px;">
      <el-breadcrumb-item :to="{ path: '/' }">Trang chủ</el-breadcrumb-item>
      <el-breadcrumb-item>Chi tiết sản phẩm</el-breadcrumb-item>
      <el-breadcrumb-item v-if="product">{{ product.name }}</el-breadcrumb-item>
    </el-breadcrumb>

    <el-skeleton v-if="loading" :rows="10" animated />

    <el-row v-else-if="product" :gutter="24">
      <!-- Gallery -->
      <el-col :xs="24" :sm="10">
        <el-card shadow="never" :body-style="{ padding: '16px', textAlign: 'center', position: 'relative', background: 'var(--el-fill-color-lighter)' }" style="margin-bottom: 12px;">
          <el-image
            :src="mainImage"
            :preview-src-list="galleryImages"
            fit="contain"
            style="width: 100%; height: 350px; border-radius: 8px;"
            :style="currentStock === 0 ? 'opacity: 0.5;' : ''"
          />
          <div v-if="currentStock === 0" class="oos-watermark">
            <el-tag type="danger" effect="dark" style="font-size: 18px; font-weight: 900; letter-spacing: 2px; padding: 8px 20px; height: auto;">
              HẾT HÀNG
            </el-tag>
          </div>
        </el-card>

        <el-space wrap :size="8" style="padding-bottom: 8px;">
          <div
            v-for="(img, idx) in galleryImages"
            :key="idx"
            class="thumb"
            :class="{ 'thumb--active': mainImage === img }"
            @click="mainImage = img"
          >
            <el-image :src="img" fit="cover" style="width: 60px; height: 60px; border-radius: 6px;" />
          </div>
        </el-space>
      </el-col>

      <!-- Info -->
      <el-col :xs="24" :sm="14">
        <div style="margin-bottom: 24px;">
          <el-text type="primary" size="small" style="font-weight: 700; letter-spacing: 1px; text-transform: uppercase;">
            {{ product.brand }}
          </el-text>
          <h1 style="font-size: 32px; font-weight: 800; color: #0f172a; margin: 4px 0 8px; line-height: 1.2;">
            {{ product.name }}
          </h1>
          <el-text v-if="product.tags?.length" type="info" size="small" style="font-style: italic;">
            {{ product.tags[0] }} — Hiệu năng đỉnh cao, thiết kế sang trọng
          </el-text>
        </div>

        <el-space wrap :size="12" style="margin-bottom: 20px;">
          <el-tag type="success" effect="plain" round style="padding: 6px 12px; height: auto;">
            <div style="display: flex; align-items: center; gap: 6px; font-size: 13px; white-space: nowrap;">
              <el-icon size="16"><CircleCheckFilled /></el-icon> <span>Chính hãng 100%</span>
            </div>
          </el-tag>
          <el-tag type="warning" effect="plain" round style="padding: 6px 12px; height: auto;">
            <div style="display: flex; align-items: center; gap: 6px; font-size: 13px; white-space: nowrap;">
              <el-icon size="16"><Service /></el-icon> <span>Bảo hành 12 tháng</span>
            </div>
          </el-tag>
          <el-tag v-if="currentStock > 10" type="info" effect="plain" round style="padding: 6px 12px; height: auto;">
            <div style="display: flex; align-items: center; gap: 6px; font-size: 13px; white-space: nowrap;">
              <el-icon size="16"><Van /></el-icon> <span>Miễn phí vận chuyển</span>
            </div>
          </el-tag>
        </el-space>

        <el-divider />

        <el-card shadow="never" style="margin-bottom: 24px; background-color: var(--el-fill-color-light); border: none;">
          <el-space align="center" :size="20">
            <el-text type="danger" style="font-size: 32px; font-weight: 700; line-height: 1;">
              {{ formatPrice(currentPrice) }}
            </el-text>
            <!-- <div style="display: flex; flex-direction: column; gap: 4px; align-items: flex-start;">
              <el-text type="info" style="text-decoration: line-through;">
                {{ formatPrice(originalPrice) }}
              </el-text>
              <el-tag type="danger" effect="dark" size="small" style="font-weight: 600;">
                -{{ discountPercent }}%
              </el-tag>
            </div> -->
          </el-space>
        </el-card>

        <div v-if="variants.length > 0" style="margin-bottom: 20px;">
          <el-text tag="div" style="font-weight: 600; margin-bottom: 8px;">Chọn phiên bản / cấu hình:</el-text>
          <el-space wrap :size="8">
            <el-button
              v-for="v in variants"
              :key="v.id"
              :type="selectedVariant?.id === v.id ? 'primary' : 'default'"
              :plain="selectedVariant?.id !== v.id"
              @click="selectVariant(v)"
            >
              {{ v.variantName }}
            </el-button>
          </el-space>
        </div>

        <el-card shadow="never" style="background: var(--el-fill-color-lighter); margin-bottom: 20px;">
          <el-row align="middle" justify="space-between" style="margin-bottom: 16px;">
            <el-text style="font-weight: 600;" type="info">Trạng thái kho:</el-text>
            <el-tag :type="currentStock > 0 ? 'success' : 'danger'" effect="dark" size="large">
              {{ currentStock > 0 ? `Còn hàng (${currentStock} máy)` : 'Hết hàng' }}
            </el-tag>
          </el-row>

          <el-row align="middle" :gutter="12">
            <el-col :span="24" style="margin-bottom: 12px;">
              <el-alert
                v-if="currentStock > 0 && currentStock <= 5"
                title="Sắp cháy hàng!"
                type="warning"
                description="Chỉ còn một vài sản phẩm cuối cùng trong kho. Đừng bỏ lỡ!"
                show-icon
                :closable="false"
              />
            </el-col>
            <el-col :span="10">
              <el-input-number
                v-model="quantity"
                :min="1"
                :max="currentStock > 0 ? currentStock : 1"
                :disabled="currentStock === 0"
                style="width: 100%;"
                size="large"
              />
            </el-col>
            <el-col :span="14">
              <el-button
                size="large"
                style="width: 100%; font-weight: 700;"
                :icon="ShoppingCart"
                :disabled="currentStock === 0 || !selectedVariant"
                :loading="addingToCart"
                @click="handleAddToCart"
              >
                THÊM VÀO GIỎ
              </el-button>
            </el-col>
            <el-col :span="24" style="margin-top: 12px;">
              <el-button
                type="danger"
                size="large"
                style="width: 100%; height: 56px; font-size: 18px; font-weight: 900; letter-spacing: 1px;"
                :disabled="currentStock === 0 || !selectedVariant"
                @click="handleBuyNow"
              >
                MUA NGAY
              </el-button>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-else description="Không tìm thấy thông tin sản phẩm" />

    <div v-if="product" style="margin-top: 40px;">
      <el-row :gutter="24">
        <el-col :md="16">
          <el-card shadow="never" style="margin-bottom: 24px; border-radius: 12px;">
            <el-tabs v-model="activeTab">
              <el-tab-pane label="Đặc điểm nổi bật" name="description">
                <div style="padding: 12px; white-space: pre-wrap; line-height: 1.8; color: #475569;">
                  {{ displayDescription }}
                </div>
              </el-tab-pane>

              <el-tab-pane label="Thông số kỹ thuật" name="specs">
                <div style="padding: 12px;">
                  <el-table :data="attributes" border stripe style="width: 100%;">
                    <el-table-column prop="name" label="Thông số" width="200" />
                    <el-table-column prop="value" label="Chi tiết" />
                    <template #empty>
                      <el-empty description="Chưa cập nhật thông số kỹ thuật" :image-size="60" />
                    </template>
                  </el-table>
                </div>
              </el-tab-pane>
            </el-tabs>
          </el-card>

          <!-- Related Products Section -->
          <div v-if="relatedProducts.length" style="margin-top: 40px;">
            <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px;">
              <h2 style="font-size: 24px; font-weight: 800; color: #1e293b; margin: 0;">Sản phẩm tương tự</h2>
              <el-button link type="primary" style="font-weight: 600;">Xem tất cả</el-button>
            </div>
            <el-row :gutter="16">
              <el-col v-for="p in relatedProducts" :key="p.id" :xs="12" :sm="6">
                <ProductCard :product="p" />
              </el-col>
            </el-row>
          </div>
        </el-col>

        <el-col :md="8">
          <el-card shadow="never" style="border-radius: 12px; margin-bottom: 24px;">
            <template #header>
              <span style="font-weight: 800; color: #1e293b;">Yên tâm mua sắm tại TechStore</span>
            </template>
            <div class="commitment-item">
              <el-icon color="var(--el-color-success)"><CircleCheckFilled /></el-icon>
              <div>
                <div class="commitment-title">Chính hãng 100%</div>
                <div class="commitment-desc">Hoàn tiền nếu phát hiện hàng giả</div>
              </div>
            </div>
            <el-divider />
            <div class="commitment-item">
              <el-icon color="var(--el-color-primary)"><Van /></el-icon>
              <div>
                <div class="commitment-title">Giao nhanh toàn quốc</div>
                <div class="commitment-desc">Miễn phí vận chuyển</div>
              </div>
            </div>
            <el-divider />
            <div class="commitment-item">
              <el-icon color="var(--el-color-warning)"><Refresh /></el-icon>
              <div>
                <div class="commitment-title">24h đổi trả</div>
                <div class="commitment-desc">Lỗi là đổi mới, thủ tục đơn giản</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- Sticky Buy Bar -->
    <el-affix position="bottom" :offset="0">
      <transition name="el-fade-in-linear">
        <div v-if="product && !loading && isStickyVisible" class="sticky-bar">
          <div class="sticky-bar-container">
            <div class="sticky-info">
              <el-image :src="mainImage" fit="cover" class="sticky-img" />
              <div class="sticky-text">
                <div class="sticky-name">{{ product.name }}</div>
                <div class="sticky-price">{{ formatPrice(currentPrice) }}</div>
              </div>
            </div>
            <div class="sticky-actions">
              <el-button 
                type="danger" 
                size="large" 
                style="font-weight: 800; padding: 0 40px;"
                :disabled="currentStock === 0"
                @click="handleBuyNow"
              >
                MUA NGAY
              </el-button>
            </div>
          </div>
        </div>
      </transition>
    </el-affix>
  </div>
</template>

<script setup>
import { ShoppingCart, CircleCheckFilled, Service, Van, Refresh } from "@element-plus/icons-vue";
import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { productsApi } from "../../api/products.api";
import { useCartStore } from "../../stores/cart";
import { toast } from "../../ui/toast";
import ProductCard from "../../components/ProductCard.vue";

const route = useRoute();
const router = useRouter();
const cartStore = useCartStore();

const loading = ref(true);
const addingToCart = ref(false);
const product = ref(null);
const variants = ref([]);

// THÊM: Biến lưu trữ thông số kỹ thuật gốc của sản phẩm
const baseAttributes = ref([]); 
const attributes = ref([]); // Biến này sẽ hiển thị ra màn hình

const galleryImages = ref([]);
const mainImage = ref("");
const selectedVariant = ref(null);
const quantity = ref(1);
const activeTab = ref("description");
const relatedProducts = ref([]);
const isStickyVisible = ref(false);

const mainInfoRef = ref(null);

const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

const currentStock = computed(() => selectedVariant.value?.stockQuantity || 0);

// Tính toán giá gốc và phần trăm giảm giá (giả lập nếu không có từ API)
const currentPrice = computed(() => selectedVariant.value ? selectedVariant.value.price : product.value?.minPrice || 0);
const originalPrice = computed(() => currentPrice.value / 0.9); // Giả định giảm 10%
const discountPercent = computed(() => 10);

const displayDescription = computed(() => {
  if (!product.value?.description) return "Chưa có bài viết mô tả cho sản phẩm này.";
  return product.value.description.split("--- THÔNG SỐ KỸ THUẬT ---")[0].trim();
});

function formatPrice(val) {
  if (val == null) return "Liên hệ";
  return new Intl.NumberFormat("vi-VN", { style:"currency", currency:"VND" }).format(val);
}

function fixImageUrl(url) {
  if (!url) return "https://via.placeholder.com/600?text=No+Image";
  if (url.startsWith("http")) return url;
  return `${BASE_URL}${url}`;
}

// SỬA: Cập nhật lại logic khi chọn phiên bản
function selectVariant(v) {
  selectedVariant.value = v;
  quantity.value = 1;

  let mergedAttrs = JSON.parse(JSON.stringify(baseAttributes.value));

  if (v && v.attributesJson) {
    try {
      const variantParsed = JSON.parse(v.attributesJson);
      const variantAttrs = Array.isArray(variantParsed)
        ? variantParsed
        : Object.keys(variantParsed).map(key => ({ name: key, value: variantParsed[key] }));

      // TỪ ĐIỂN MAP KEY: Tự động dịch các từ tiếng Anh lôm côm sang chuẩn tiếng Việt của gốc
      const keyMapping = {
        'cpu': 'CPU',
        'ram': 'RAM',
        'storage': 'Ổ cứng (SSD)',
        'vga': 'Card màn hình (VGA)',
        'gpu': 'Card màn hình (VGA)',
        'screen': 'Màn hình',
        'display': 'Màn hình'
      };

      variantAttrs.forEach(vAttr => {
        // Chuẩn hóa cái tên mà thằng Admin nhập (bỏ khoảng trắng thừa, đưa về chữ thường để tra từ điển)
        let rawName = vAttr.name.trim();
        let lowerName = rawName.toLowerCase();
        
        // Nếu từ điển có từ này, dịch nó ra. Nếu không, giữ nguyên (nhưng viết hoa chữ cái đầu cho đẹp)
        let normalizedName = keyMapping[lowerName] || (rawName.charAt(0).toUpperCase() + rawName.slice(1));

        // Đi tìm xem trong baseAttributes có cái tên nào giống normalizedName hoặc giống chữ thường của nó không
        const existingIndex = mergedAttrs.findIndex(
          a => a.name.trim().toLowerCase() === normalizedName.toLowerCase() || 
               a.name.trim().toLowerCase() === lowerName
        );

        if (existingIndex >= 0) {
          // TÌM THẤY: Ghi đè giá trị (Ví dụ: đè AMD bằng Core i9)
          mergedAttrs[existingIndex].value = vAttr.value; 
        } else {
          // KHÔNG TÌM THẤY: Thêm mới thông số này vào bảng
          mergedAttrs.push({ name: normalizedName, value: vAttr.value });
        }
      });
    } catch (e) {
      console.error("Lỗi parse thông số riêng của biến thể", e);
    }
  }

  attributes.value = mergedAttrs;
}

async function loadProductData() {
  const productId = route.params.id;
  if (!productId) return;
  loading.value = true;
  try {
    const res = await productsApi.get(productId);
    const data = res.data?.data || res.data;
    product.value = data;

    if (data.images?.length > 0) {
      const sortedImages = [...data.images].sort((a,b) => (b.isPrimary ? 1 : 0) - (a.isPrimary ? 1 : 0));
      galleryImages.value = sortedImages.map(img => fixImageUrl(img.url));
      mainImage.value = galleryImages.value[0];
    } else {
      mainImage.value = fixImageUrl(null);
      galleryImages.value = [mainImage.value];
    }

    // SỬA: Đọc thông số kỹ thuật gốc và lưu vào baseAttributes thay vì attributes
    if (data.attributes) {
      try {
        const parsed = JSON.parse(data.attributes);
        baseAttributes.value = Array.isArray(parsed)
          ? parsed
          : Object.keys(parsed).map(key => ({ name:key, value:parsed[key] }));
      } catch { baseAttributes.value = []; }
    }

    if (data.variants?.length > 0) {
      variants.value = data.variants.filter(v => v.isActive);
    } else {
      const varRes = await fetch(`${BASE_URL}/api/products/${productId}/variants`).then(r => r.json());
      variants.value = (varRes || []).filter(v => v.isActive);
    }

    // Khi load xong, tự động chọn phiên bản đầu tiên (hàm này sẽ kích hoạt gộp thông số)
    if (variants.value.length > 0) {
        selectVariant(variants.value[0]);
    } else {
        // Nếu sản phẩm không có phiên bản nào, chỉ hiện thông số gốc
        attributes.value = [...baseAttributes.value];
    }

    loadRelatedProducts(data);

  } catch (error) {
    toast("Lỗi khi tải dữ liệu sản phẩm", "error");
    console.error(error);
  } finally {
    loading.value = false;
  }
}

async function loadRelatedProducts(currentProduct) {
  try {
    const brand = currentProduct.brand;
    const categoryId = currentProduct.categoryId;
    
    // Tìm sản phẩm cùng danh mục hoặc cùng thương hiệu
    const res = await productsApi.getAll({ 
      page: 0, 
      size: 10,
      brand: brand,
      categoryId: categoryId
    });
    
    const all = res.data?.data?.content || res.data?.content || [];
    // Loại bỏ sản phẩm hiện tại
    relatedProducts.value = all.filter(p => p.id !== currentProduct.id).slice(0, 4);
  } catch (err) {
    console.error("Lỗi tải sản phẩm liên quan", err);
  }
}

async function handleAddToCart() {
  if (!selectedVariant.value) return toast("Vui lòng chọn cấu hình sản phẩm", "warning");
  if (quantity.value > currentStock.value) return toast("Số lượng vượt quá tồn kho", "warning");
  addingToCart.value = true;
  try {
    await cartStore.addToCart(selectedVariant.value.id, quantity.value);
    toast(`Đã thêm ${quantity.value} máy vào giỏ hàng!`, "success");
  } catch {
    toast("Không thể thêm vào giỏ hàng. Vui lòng đăng nhập.", "error");
  } finally {
    addingToCart.value = false;
  }
}

async function handleBuyNow() {
  if (!selectedVariant.value) return toast("Vui lòng chọn cấu hình sản phẩm", "warning");
  if (quantity.value > currentStock.value) return toast("Số lượng vượt quá tồn kho", "warning");
  
  try {
    await cartStore.addToCart(selectedVariant.value.id, quantity.value);
    router.push("/cart");
  } catch {
    toast("Vui lòng đăng nhập để tiếp tục", "error");
  }
}

function handleScroll() {
  // Hiện sticky bar khi cuộn qua 600px (đại diện cho việc qua khỏi info chính)
  isStickyVisible.value = window.scrollY > 600;
}

import { onUnmounted } from "vue";

onMounted(() => {
  loadProductData();
  window.addEventListener('scroll', handleScroll);
});

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
});
</script>

<style scoped>
.oos-watermark {
  position: absolute;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  z-index: 10;
  pointer-events: none;
}

.thumb {
  cursor: pointer;
  border-radius: 8px;
  border: 2px solid transparent;
  transition: border-color 0.15s;
  overflow: hidden;
}

.thumb--active {
  border-color: var(--el-color-primary);
}

.commitment-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.commitment-title {
  font-weight: 700;
  font-size: 14px;
  color: #1e293b;
  margin-bottom: 2px;
}

.commitment-desc {
  font-size: 12px;
  color: #64748b;
}

.sticky-bar {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-top: 1px solid #e2e8f0;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.05);
  padding: 12px 0;
  width: 100%;
}

.sticky-bar-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sticky-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.sticky-img {
  width: 50px;
  height: 50px;
  border-radius: 6px;
  background: #f1f5f9;
}

.sticky-name {
  font-weight: 700;
  color: #1e293b;
  font-size: 16px;
}

.sticky-price {
  font-weight: 800;
  color: var(--el-color-danger);
}

@media (max-width: 768px) {
  .sticky-info {
    display: none;
  }
  .sticky-bar-container {
    justify-content: center;
  }
  .sticky-actions {
    width: 100%;
  }
  .sticky-actions button {
    width: 100%;
  }
}
</style>