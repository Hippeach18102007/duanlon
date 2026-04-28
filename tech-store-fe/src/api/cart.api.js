// FILE: src/api/cart.api.js
import http from "./http";

export const cartApi = {
  addItem(data) {
    return http.post("/api/cart/items", data);
  },

  getItems() {
    return http.get("/api/cart/items");
  },

  count() {
    return http.get("/api/cart/count");
  },

  remove(id) {
    return http.delete(`/api/cart/items/${id}`);
  },

  updateQuantity(id, quantity) {
    return http.put(`/api/cart/items/${id}`, null, {
      params: { quantity },
    });
  },

  clear() {
    return http.delete("/api/cart");
  },
};
