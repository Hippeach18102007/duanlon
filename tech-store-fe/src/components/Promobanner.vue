<template>
  <el-carousel
    v-if="slides.length > 0"
    height="300px"
    :interval="5000"
    arrow="always"
    indicator-position="inside"
    style="border-radius: 4px; overflow: hidden; margin-bottom: 24px;"
  >
    <el-carousel-item v-for="(slide, i) in slides" :key="i">
      <div :style="{ background: slide.bg, height: '100%', display: 'flex', alignItems: 'center' }">
        <el-row align="middle" style="width: 100%; padding: 0 48px;" :gutter="24">
          <el-col :xs="24" :sm="14">
            <el-space direction="vertical" :size="16" alignment="flex-start">
              <el-tag :type="slide.tagType" effect="plain" size="default">
                {{ slide.tagLabel }}
              </el-tag>
              <span style="font-size: clamp(20px, 2.8vw, 32px); font-weight: 800; color: #f8fafc; line-height: 1.2; display: block;">
                {{ slide.title }}
              </span>
              <el-button type="primary" size="large" color="#6366f1" @click="goToProduct(slide.productId)">
                Xem thêm →
              </el-button>
            </el-space>
          </el-col>

          <el-col :xs="0" :sm="10" style="display: flex; align-items: center; justify-content: center; height: 260px;">
            <el-image
              :src="slide.img"
              :alt="slide.title"
              fit="cover"
              style="width: 100%; max-width: 320px; height: 210px; border-radius: 4px; box-shadow: 0 20px 60px rgba(0,0,0,0.4);"
            />
          </el-col>
        </el-row>
      </div>
    </el-carousel-item>
  </el-carousel>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { productsApi } from "../api/products.api";

const router = useRouter();
const slides = ref([]);

const BG_COLORS = ["#0f172a", "#16213e", "#1b263b", "#0d1b2a", "#1a1a2e", "#0e1628"];
const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

function fixImageUrl(url) {
  if (!url) return "https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?w=500&q=80";
  if (url.startsWith("http")) return url;
  return `${BASE_URL}${url}`;
}

function buildSlide(product, tagKey, index) {
  const isBestseller = tagKey === "bestseller";
  return {
    productId: product.id,
    title: product.name,
    tagLabel: isBestseller ? "🔥 BÁN CHẠY NHẤT" : "✨ HÀNG MỚI VỀ",
    tagType: isBestseller ? "warning" : "success",
    bg: BG_COLORS[index % BG_COLORS.length],
    img: fixImageUrl(product.imageUrl || product.thumbnailUrl),
  };
}

async function loadSlides() {
  try {
    const [bestsellerRes, newArrivalRes] = await Promise.allSettled([
      productsApi.list({ sortBy: "best_selling", page: 0, size: 20 }),
      productsApi.list({ sortBy: "newest_arrival", page: 0, size: 20, isNew: true }),
    ]);

    const bestsellers = bestsellerRes.status === "fulfilled"
      ? (bestsellerRes.value.data?.data?.content || bestsellerRes.value.data?.content || [])
      : [];

    const newArrivals = newArrivalRes.status === "fulfilled"
      ? (newArrivalRes.value.data?.data?.content || newArrivalRes.value.data?.content || [])
      : [];

    const tagFilteredBestsellers = bestsellers
      .filter(p => Array.isArray(p.tags) && p.tags.some(t => (typeof t === "string" ? t : t?.name)?.toLowerCase() === "bestseller"))
      .slice(0, 10);

    const tagFilteredNew = newArrivals
      .filter(p => Array.isArray(p.tags) && p.tags.some(t => (typeof t === "string" ? t : t?.name)?.toLowerCase() === "new-arrival"))
      .slice(0, 10);

    const useBestsellers = tagFilteredBestsellers.length > 0 ? tagFilteredBestsellers : bestsellers.slice(0, 10);
    const useNew = tagFilteredNew.length > 0 ? tagFilteredNew : newArrivals.slice(0, 10);

    const combined = [];
    const maxLen = Math.max(useBestsellers.length, useNew.length);
    for (let i = 0; i < maxLen; i++) {
      if (i < useBestsellers.length) combined.push(buildSlide(useBestsellers[i], "bestseller", combined.length));
      if (i < useNew.length) combined.push(buildSlide(useNew[i], "new-arrival", combined.length));
    }

    slides.value = combined;
  } catch (e) {
    console.error("PromoBanner: lỗi tải dữ liệu", e);
    slides.value = [];
  }
}

function goToProduct(id) {
  if (id) router.push("/product/" + id);
}

onMounted(loadSlides);
</script>