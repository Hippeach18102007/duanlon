// ADD: Pinia store quản lý trạng thái giỏ hàng (badge)

import { defineStore } from "pinia";
import { cartApi } from "../api/cart.api";

export const useCartStore = defineStore("cart", {
  state: () => ({
    count: 0, // số lượng item trong giỏ
  }),

  actions: {
    // =========================
    // ADD TO CART
    // =========================
    async addToCart(variantId, quantity = 1) {
      if (!variantId || quantity <= 0) {
        throw new Error("Invalid variantId or quantity");
      }

      await cartApi.addItem({
        variantId: Number(variantId),
        quantity: Number(quantity),
      });

      await this.refreshCount();
    },

    // =========================
    // CLEAR CART
    // =========================
    async clearCart() {
      await cartApi.clear();
      this.count = 0; // clear local state ngay
    },

    // =========================
    // CART BADGE
    // =========================
    async refreshCount() {
      const res = await cartApi.count();
      this.count = Number(res?.data || 0);
    },
  },
});
