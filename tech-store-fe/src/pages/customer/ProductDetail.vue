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
        <el-space :size="8" align="center" style="margin-bottom: 10px;">
          <el-tag v-if="product.isNew" type="danger" effect="dark" round>NEW ARRIVAL</el-tag>
          <el-text type="info" size="small">SKU: {{ product.sku }}</el-text>
        </el-space>

        <el-text tag="div" style="font-size: 22px; font-weight: 700; color: #1e293b; margin-bottom: 12px;">
          {{ product.name }}
        </el-text>

        <el-space v-if="product.tags?.length > 0" wrap :size="6" style="margin-bottom: 12px;">
          <el-tag v-for="(tag, idx) in product.tags" :key="idx" size="small" type="warning" effect="light">
            {{ tag }}
          </el-tag>
        </el-space>

        <el-divider border-style="dashed" />

        <el-text type="danger" style="font-size: 2rem; font-weight: 800; display: block; margin-bottom: 20px;">
          {{ formatPrice(selectedVariant ? selectedVariant.price : product.minPrice) }}
        </el-text>

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
            <el-col flex="none">
              <el-input-number
                v-model="quantity"
                :min="1"
                :max="currentStock > 0 ? currentStock : 1"
                :disabled="currentStock === 0"
                style="width: 130px;"
              />
            </el-col>
            <el-col flex="1">
              <el-button
                type="primary"
                size="large"
                style="width: 100%;"
                :icon="ShoppingCart"
                :disabled="currentStock === 0 || !selectedVariant"
                :loading="addingToCart"
                @click="handleAddToCart"
              >
                THÊM VÀO GIỎ HÀNG
              </el-button>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-else description="Không tìm thấy thông tin sản phẩm" />

    <div v-if="product" style="margin-top: 40px;">
      <el-card shadow="never">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="Đặc điểm nổi bật" name="description">
            <div style="padding: 12px; white-space: pre-wrap; line-height: 1.8; color: #475569;">
              {{ displayDescription }}
            </div>
          </el-tab-pane>

          <el-tab-pane label="Thông số kỹ thuật" name="specs">
            <div style="padding: 12px;">
              <el-table :data="attributes" border stripe style="width: 100%; max-width: 600px;">
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
    </div>
  </div>
</template>

<script setup>
import { ShoppingCart } from "@element-plus/icons-vue";
import { ref, computed, onMounted } from "vue";
import { useRoute } from "vue-router";
import { productsApi } from "../../api/products.api";
import { useCartStore } from "../../stores/cart";
import { toast } from "../../ui/toast";

const route = useRoute();
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

const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

const currentStock = computed(() => selectedVariant.value?.stockQuantity || 0);

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

  } catch (error) {
    toast("Lỗi khi tải dữ liệu sản phẩm", "error");
    console.error(error);
  } finally {
    loading.value = false;
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

onMounted(() => loadProductData());
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
</style>